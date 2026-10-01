package net.javaguides.springboot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import net.javaguides.springboot.model.ChatMessage;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findTop100ByChannelAndTypeOrderByTimestampAsc(String channel, ChatMessage.MessageType type);

    List<ChatMessage> findTop100ByChannelOrderByTimestampAsc(String channel);

    List<ChatMessage> findTop100ByTypeOrderByTimestampAsc(ChatMessage.MessageType type);

    List<ChatMessage> findTop100ByOrderByTimestampAsc();
}
