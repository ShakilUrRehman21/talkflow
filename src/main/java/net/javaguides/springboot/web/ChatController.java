package net.javaguides.springboot.web;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import net.javaguides.springboot.model.ChatMessage;
import net.javaguides.springboot.repository.ChatMessageRepository;

@Controller
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessageSendingOperations messagingTemplate;

    // Active session tracking: sessionId -> username, sessionId -> channel
    private static final ConcurrentHashMap<String, String> sessionUsers = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> sessionChannels = new ConcurrentHashMap<>();

    // Real online members per channel
    private static final ConcurrentHashMap<String, Set<String>> channelUsers = new ConcurrentHashMap<>();

    public static ConcurrentHashMap<String, Set<String>> getChannelUsers() {
        return channelUsers;
    }

    public static ConcurrentHashMap<String, String> getSessionUsers() {
        return sessionUsers;
    }

    public static ConcurrentHashMap<String, String> getSessionChannels() {
        return sessionChannels;
    }

    public ChatController(ChatMessageRepository chatMessageRepository,
                          SimpMessageSendingOperations messagingTemplate) {
        this.chatMessageRepository = chatMessageRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Handles chat messages sent to /app/chat.sendMessage.
     * Persists the message to the database with channel attribution,
     * then broadcasts to the target channel topic and public topic.
     */
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage chatMessage) {
        String channel = (chatMessage.getChannel() != null && !chatMessage.getChannel().isBlank())
                ? chatMessage.getChannel().trim().toLowerCase() : "design-trends";
        chatMessage.setChannel(channel);

        if (chatMessage.getTimestamp() == null) {
            chatMessage.setTimestamp(LocalDateTime.now());
        }

        if (chatMessage.getSender() != null && !chatMessage.getSender().isBlank()) {
            channelUsers.computeIfAbsent(channel, k -> ConcurrentHashMap.newKeySet()).add(chatMessage.getSender().trim());
        }

        ChatMessage saved = chatMessageRepository.save(chatMessage);
        saved.setFormattedTime(saved.getFormattedTime());

        // Broadcast to channel topic and fallback public topic
        messagingTemplate.convertAndSend("/topic/channel/" + channel, saved);
        messagingTemplate.convertAndSend("/topic/public", saved);
    }

    /**
     * Handles user join / switch events sent to /app/chat.addUser.
     * Records the user session, cleans up previous channel if switched,
     * and broadcasts updated roster.
     */
    @MessageMapping("/chat.addUser")
    public void addUser(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String username = (chatMessage.getSender() != null && !chatMessage.getSender().isBlank())
                ? chatMessage.getSender().trim() : "Member";
        String channel = (chatMessage.getChannel() != null && !chatMessage.getChannel().isBlank())
                ? chatMessage.getChannel().trim().toLowerCase() : "design-trends";

        if (headerAccessor.getSessionAttributes() != null) {
            headerAccessor.getSessionAttributes().put("username", username);
            headerAccessor.getSessionAttributes().put("channel", channel);
        }

        // If this session was previously registered in a different channel, leave old channel
        if (sessionId != null) {
            String oldChannel = sessionChannels.get(sessionId);
            if (oldChannel != null && !oldChannel.equalsIgnoreCase(channel)) {
                removeSessionFromChannel(sessionId, username, oldChannel);
            }
            sessionUsers.put(sessionId, username);
            sessionChannels.put(sessionId, channel);
        }

        // Register user in new channel
        channelUsers.computeIfAbsent(channel, k -> ConcurrentHashMap.newKeySet()).add(username);

        chatMessage.setSender(username);
        chatMessage.setChannel(channel);
        chatMessage.setTimestamp(LocalDateTime.now());
        chatMessage.setFormattedTime(chatMessage.getFormattedTime());

        // Broadcast join event
        messagingTemplate.convertAndSend("/topic/channel/" + channel, chatMessage);
        messagingTemplate.convertAndSend("/topic/public", chatMessage);

        // Broadcast entire current user roster for this channel
        broadcastChannelUsers(channel);
    }

    /**
     * Broadcasts the active user set to /topic/channel/{channel}/users
     */
    public void broadcastChannelUsers(String channel) {
        if (channel == null) return;
        String ch = channel.trim().toLowerCase();
        Set<String> activeUsers = channelUsers.getOrDefault(ch, Collections.emptySet());
        messagingTemplate.convertAndSend("/topic/channel/" + ch + "/users", activeUsers);
    }

    /**
     * Removes a session from a channel and notifies subscribers if user has no remaining sessions.
     */
    public void removeSessionFromChannel(String sessionId, String username, String channel) {
        if (channel == null || username == null) return;
        String ch = channel.trim().toLowerCase();

        sessionChannels.remove(sessionId);

        // Check if user still has another active session in this channel
        boolean stillInChannel = false;
        for (Map.Entry<String, String> entry : sessionChannels.entrySet()) {
            if (ch.equalsIgnoreCase(entry.getValue())) {
                String u = sessionUsers.get(entry.getKey());
                if (username.equalsIgnoreCase(u)) {
                    stillInChannel = true;
                    break;
                }
            }
        }

        if (!stillInChannel) {
            Set<String> users = channelUsers.get(ch);
            if (users != null) {
                users.remove(username);
            }

            ChatMessage leaveMsg = new ChatMessage();
            leaveMsg.setType(ChatMessage.MessageType.LEAVE);
            leaveMsg.setSender(username);
            leaveMsg.setChannel(ch);
            leaveMsg.setTimestamp(LocalDateTime.now());
            leaveMsg.setFormattedTime(leaveMsg.getFormattedTime());

            messagingTemplate.convertAndSend("/topic/channel/" + ch, leaveMsg);
            messagingTemplate.convertAndSend("/topic/public", leaveMsg);

            broadcastChannelUsers(ch);
        }
    }

    /**
     * Complete cleanup when a WebSocket session disconnects.
     */
    public void handleSessionDisconnect(String sessionId, String username, String channel) {
        if (sessionId == null) return;
        removeSessionFromChannel(sessionId, username, channel);
        sessionUsers.remove(sessionId);
    }

    /**
     * REST endpoint to fetch persistent chat history for a specific channel.
     */
    @GetMapping("/api/messages")
    @ResponseBody
    public List<ChatMessage> getRecentMessages(@RequestParam(value = "channel", defaultValue = "design-trends") String channel) {
        String ch = channel.trim().toLowerCase();
        List<ChatMessage> messages = chatMessageRepository.findTop100ByChannelAndTypeOrderByTimestampAsc(ch, ChatMessage.MessageType.CHAT);
        if (messages.isEmpty() && "design-trends".equalsIgnoreCase(ch)) {
            // Also check general as fallback if brand new
            messages = chatMessageRepository.findTop100ByChannelAndTypeOrderByTimestampAsc("general", ChatMessage.MessageType.CHAT);
        }
        messages.forEach(m -> m.setFormattedTime(m.getFormattedTime()));
        return messages;
    }

    /**
     * REST endpoint to fetch current active online users in a channel on page load or channel switch.
     */
    @GetMapping("/api/channels/{channel}/users")
    @ResponseBody
    public Set<String> getChannelActiveUsers(@PathVariable("channel") String channel) {
        String ch = channel.trim().toLowerCase();
        return channelUsers.getOrDefault(ch, Collections.emptySet());
    }
}
