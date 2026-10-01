package net.javaguides.springboot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import net.javaguides.springboot.model.ChatMessage;
import net.javaguides.springboot.web.ChatController;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class JavaChatAppApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ChatController chatController;

	@Test
	void contextLoads() {
	}

	@Test
	void testLoginPage() throws Exception {
		mockMvc.perform(get("/login")).andExpect(status().isOk());
	}

	@Test
	void testRegistrationPage() throws Exception {
		mockMvc.perform(get("/registration")).andExpect(status().isOk());
	}

	@Test
	void testSendMessage() {
		ChatMessage msg = new ChatMessage(ChatMessage.MessageType.CHAT, "Hello World", "Alex", "design-trends");
		assertDoesNotThrow(() -> chatController.sendMessage(msg));
		var history = chatController.getRecentMessages("design-trends");
		assertNotNull(history);
		assertEquals(1, history.size());
		assertEquals("Hello World", history.get(0).getContent());
	}
}
