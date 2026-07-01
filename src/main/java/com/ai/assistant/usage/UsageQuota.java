package com.ai.assistant.usage;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "usage_quota")
@Data
@NoArgsConstructor
public class UsageQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "period_ym", nullable = false, length = 7)
    private String periodYm;

    @Column(name = "tokens_used", nullable = false)
    private long tokensUsed;

    @Column(name = "token_limit", nullable = false)
    private long tokenLimit;

    public UsageQuota(Long userId, String periodYm, long tokensUsed, long tokenLimit) {
        this.userId = userId;
        this.periodYm = periodYm;
        this.tokensUsed = tokensUsed;
        this.tokenLimit = tokenLimit;
    }
}
