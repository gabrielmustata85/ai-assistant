package com.ai.assistant.usage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsageQuotaRepository extends JpaRepository<UsageQuota, Long> {
    Optional<UsageQuota> findByUserId(Long userId);
}
