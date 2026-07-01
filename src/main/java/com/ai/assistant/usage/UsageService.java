package com.ai.assistant.usage;

import com.ai.assistant.auth.CurrentUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

/** Urmărește și limitează consumul de tokens per user, resetat lunar. */
@Service
public class UsageService {

    private final UsageQuotaRepository repo;
    private final long defaultLimit;
    private final long proLimit;
    private final long maxLimit;

    public UsageService(UsageQuotaRepository repo,
                        @Value("${usage.monthly-token-limit:500000}") long defaultLimit,
                        @Value("${usage.plan.pro-limit:5000000}") long proLimit,
                        @Value("${usage.plan.max-limit:20000000}") long maxLimit) {
        this.repo = repo;
        this.defaultLimit = defaultLimit;
        this.proLimit = proLimit;
        this.maxLimit = maxLimit;
    }

    private String period() {
        return YearMonth.now().toString();   // "2026-07"
    }

    private long limitForPlan(String plan) {
        return switch (plan == null ? "FREE" : plan.toUpperCase()) {
            case "PRO" -> proLimit;
            case "MAX" -> maxLimit;
            default -> defaultLimit;
        };
    }

    /** Starea curentă a userului autentificat. */
    @Transactional(readOnly = true)
    public UsageStatus current() {
        Long userId = CurrentUser.id();
        if (userId == null) return new UsageStatus(0, defaultLimit, period(), "FREE");
        UsageQuota q = repo.findByUserId(userId).orElse(null);
        long used = (q != null && period().equals(q.getPeriodYm())) ? q.getTokensUsed() : 0;
        long limit = (q != null) ? q.getTokenLimit() : defaultLimit;
        String plan = (q != null && q.getPlan() != null) ? q.getPlan() : "FREE";
        return new UsageStatus(used, limit, period(), plan);
    }

    /** Schimbă planul userului (FREE/PRO/MAX) și limita aferentă. */
    @Transactional
    public UsageStatus upgrade(String plan) {
        Long userId = CurrentUser.id();
        if (userId == null) return current();
        String normalized = (plan == null ? "FREE" : plan.toUpperCase());
        long limit = limitForPlan(normalized);
        String p = period();
        UsageQuota q = repo.findByUserId(userId)
                .orElseGet(() -> new UsageQuota(userId, p, 0, limit));
        if (!p.equals(q.getPeriodYm())) {
            q.setPeriodYm(p);
            q.setTokensUsed(0);
        }
        q.setPlan(normalized);
        q.setTokenLimit(limit);
        repo.save(q);
        return current();
    }

    /** Aruncă QuotaExceededException dacă userul a atins limita. Apelat înainte de fiecare cerere AI. */
    public void check() {
        Long userId = CurrentUser.id();
        if (userId == null) return;
        UsageStatus s = current();
        if (s.exhausted()) {
            throw new QuotaExceededException(s.used(), s.limit());
        }
    }

    /** Adaugă tokens la consumul userului (input+output), resetând dacă s-a schimbat luna. */
    @Transactional
    public void record(long tokens) {
        Long userId = CurrentUser.id();
        if (userId == null || tokens <= 0) return;
        String p = period();
        UsageQuota q = repo.findByUserId(userId)
                .orElseGet(() -> new UsageQuota(userId, p, 0, defaultLimit));
        if (!p.equals(q.getPeriodYm())) {
            q.setPeriodYm(p);
            q.setTokensUsed(0);
        }
        q.setTokensUsed(q.getTokensUsed() + tokens);
        repo.save(q);
    }
}
