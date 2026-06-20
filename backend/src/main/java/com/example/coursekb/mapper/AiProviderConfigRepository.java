package com.example.coursekb.mapper;

import com.example.coursekb.entity.AiProviderConfig;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiProviderConfigRepository extends JpaRepository<AiProviderConfig, Long> {
    List<AiProviderConfig> findByUserIdOrderByEnabledDescUpdateTimeDescIdDesc(Long userId);

    Optional<AiProviderConfig> findByIdAndUserId(Long id, Long userId);
}
