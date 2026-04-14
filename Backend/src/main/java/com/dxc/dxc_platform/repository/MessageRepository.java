package com.dxc.dxc_platform.repository;

import com.dxc.dxc_platform.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByConversationIdOrderBySentAtAsc(Long convId);
    List<Message> findByReceiverIdAndReadFalse(Long receiverId);
}