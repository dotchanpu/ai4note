package com.example.coursekb.mapper;

import com.example.coursekb.entity.UserKnowledgeStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserKnowledgeStatusRepository extends JpaRepository<UserKnowledgeStatus, Long> {
    Optional<UserKnowledgeStatus> findByUserIdAndKnowledgeItemId(Long userId, Long knowledgeItemId);

    List<UserKnowledgeStatus> findByUserIdAndKnowledgeItemIdIn(
            Long userId, Collection<Long> knowledgeItemIds);
}
