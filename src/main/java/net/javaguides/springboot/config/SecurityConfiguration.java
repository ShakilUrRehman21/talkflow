package net.javaguides.springboot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import net.javaguides.springboot.service.UserService;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

	@Bean
	public DaoAuthenticationProvider authenticationProvider(UserService userService, BCryptPasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider auth = new DaoAuthenticationProvider();
		auth.setUserDetailsService(userService);
		auth.setPasswordEncoder(passwordEncoder);
		return auth;
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http, DaoAuthenticationProvider authenticationProvider) throws Exception {
		http
			.authenticationProvider(authenticationProvider)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(
					"/registration**",
					"/js/**",
					"/css/**",
					"/img/**",
					"/webjars/**",
					"/ws/**",
					"/h2-console/**"
				).permitAll()
				.anyRequest().authenticated()
			)
			.formLogin(form -> form
				.loginPage("/login")
				.defaultSuccessUrl("/chat", true)
				.permitAll()
			)
			.logout(logout -> logout
				.logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
				.invalidateHttpSession(true)
				.clearAuthentication(true)
				.logoutSuccessUrl("/login?logout")
				.permitAll()
			)
			.csrf(csrf -> csrf
				.ignoringRequestMatchers("/h2-console/**", "/ws/**")
			)
			.headers(headers -> headers
				.frameOptions(frame -> frame.sameOrigin())
			);

		return http.build();
	}
}
