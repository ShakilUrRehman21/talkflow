package net.javaguides.springboot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import net.javaguides.springboot.web.ChatController;

@Component
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final ChatController chatController;

    public WebSocketEventListener(ChatController chatController) {
        this.chatController = chatController;
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        String username = (headerAccessor.getSessionAttributes() != null)
                ? (String) headerAccessor.getSessionAttributes().get("username")
                : null;
        if (username == null && sessionId != null) {
            username = ChatController.getSessionUsers().get(sessionId);
        }

        String channel = (headerAccessor.getSessionAttributes() != null)
                ? (String) headerAccessor.getSessionAttributes().get("channel")
                : null;
        if (channel == null && sessionId != null) {
            channel = ChatController.getSessionChannels().get(sessionId);
        }
        if (channel == null || channel.isBlank()) {
            channel = "design-trends";
        }
        channel = channel.trim().toLowerCase();

        if (username != null && sessionId != null) {
            logger.info("User disconnected: {} (sessionId: {}) from channel: {}", username, sessionId, channel);
            chatController.handleSessionDisconnect(sessionId, username, channel);
        }
    }
}
