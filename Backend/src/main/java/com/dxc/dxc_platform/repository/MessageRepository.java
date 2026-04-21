package com.dxc.dxc_platform.repository;

import com.dxc.dxc_platform.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByConversationIdOrderBySentAtAscIdAsc(Long convId);

    List<Message> findByReceiverIdAndReadFalse(Long receiverId);

    Optional<Message> findTopByConversationIdOrderBySentAtDescIdDesc(Long conversationId);

    long countByConversationIdAndReceiverIdAndReadFalse(Long conversationId, Long receiverId);
}