package net.javaguides.springboot.web;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import net.javaguides.springboot.model.User;
import net.javaguides.springboot.repository.UserRepository;

@Controller
public class MainController {

	private final UserRepository userRepository;

	public MainController(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@GetMapping("/")
	public String root() {
		return "redirect:/chat";
	}

	@GetMapping("/index")
	public String home() {
		return "redirect:/chat";
	}

	@GetMapping("/login")
	public String login() {
		return "login";
	}

	@GetMapping("/chat")
	public String chat(Principal principal, Model model) {
		String email = principal.getName();
		User user = userRepository.findByEmail(email);
		String displayName = (user != null && user.getFirstName() != null && !user.getFirstName().isBlank())
				? user.getFirstName() + (user.getLastName() != null && !user.getLastName().isBlank() ? " " + user.getLastName() : "")
				: email;
		model.addAttribute("username", displayName);
		model.addAttribute("userEmail", email);
		return "chat";
	}
}
