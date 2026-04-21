package com.dxc.dxc_platform.repository;

import com.dxc.dxc_platform.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByUser1IdOrUser2Id(Long userId1, Long userId2);

    Optional<Conversation> findByUser1IdAndUser2Id(Long u1, Long u2);

    Optional<Conversation> findByUser2IdAndUser1Id(Long u1, Long u2);
}