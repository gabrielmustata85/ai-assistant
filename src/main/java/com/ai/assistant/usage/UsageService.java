package com.ai.assistant.usage;

import com.ai.assistant.auth.CurrentUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Urmărește și limitează consumul de tokens per user, pe FERESTRE GLISANTE per plan:
 *  - FREE: fereastră scurtă (câteva ore) + buget mic → câteva zeci de întrebări, apoi blocaj până la resetare.
 *  - PRO/MAX: fereastră lunară + buget mare.
 * Totul configurabil din env (vezi @Value).
 */
@Service
public class UsageService {

    private final UsageQuotaRepository repo;
    private final long freeLimit;
    private final long freeWindowSec;
    private final long proLimit;
    private final long maxLimit;
    private final long paidWindowSec;

    public UsageService(UsageQuotaRepository repo,
                        @Value("${usage.free.token-limit:50000}") long freeLimit,
                        @Value("${usage.free.window-seconds:14400}") long freeWindowSec,
                        @Value("${usage.plan.pro-limit:5000000}") long proLimit,
                        @Value("${usage.plan.max-limit:20000000}") long maxLimit,
                        @Value("${usage.paid.window-seconds:2592000}") long paidWindowSec) {
        this.repo = repo;
        this.freeLimit = freeLimit;
        this.freeWindowSec = freeWindowSec;
        this.proLimit = proLimit;
        this.maxLimit = maxLimit;
        this.paidWindowSec = paidWindowSec;
    }

    private long limitForPlan(String plan) {
        return switch (norm(plan)) {
            case "PRO" -> proLimit;
            case "MAX" -> maxLimit;
            default -> freeLimit;
        };
    }

    private long windowForPlan(String plan) {
        return "FREE".equals(norm(plan)) ? freeWindowSec : paidWindowSec;
    }

    private String norm(String plan) {
        return plan == null ? "FREE" : plan.toUpperCase();
    }

    /** Resetează fereastra dacă a expirat (mutează consumul înapoi la 0). Nu salvează singură. */
    private void rollover(UsageQuota q) {
        long win = windowForPlan(q.getPlan());
        if (Instant.now().isAfter(q.getWindowStart().plusSeconds(win))) {
            q.setWindowStart(Instant.now());
            q.setTokensUsed(0);
        }
    }

    private UsageQuota newFree(Long userId) {
        UsageQuota q = new UsageQuota(userId, "", 0, freeLimit);
        q.setWindowStart(Instant.now());
        return q;
    }

    /** Starea curentă a userului autentificat (fără efecte). */
    @Transactional(readOnly = true)
    public UsageStatus current() {
        Long userId = CurrentUser.id();
        if (userId == null) {
            long reset = Instant.now().plusSeconds(freeWindowSec).toEpochMilli();
            return new UsageStatus(0, freeLimit, "FREE", reset, freeWindowSec);
        }
        UsageQuota q = repo.findByUserId(userId).orElse(null);
        if (q == null) {
            long reset = Instant.now().plusSeconds(freeWindowSec).toEpochMilli();
            return new UsageStatus(0, freeLimit, "FREE", reset, freeWindowSec);
        }
        long win = windowForPlan(q.getPlan());
        boolean elapsed = Instant.now().isAfter(q.getWindowStart().plusSeconds(win));
        long used = elapsed ? 0 : q.getTokensUsed();
        Instant resetAt = elapsed ? Instant.now().plusSeconds(win) : q.getWindowStart().plusSeconds(win);
        return new UsageStatus(used, q.getTokenLimit(), q.getPlan(), resetAt.toEpochMilli(), win);
    }

    /** Aruncă QuotaExceededException dacă bugetul ferestrei curente e consumat. Apelat înainte de fiecare cerere AI. */
    @Transactional
    public void check() {
        Long userId = CurrentUser.id();
        if (userId == null) return;
        UsageQuota q = repo.findByUserId(userId).orElse(null);
        if (q == null) return;   // niciun consum încă → permis
        rollover(q);
        repo.save(q);
        if (q.getTokensUsed() >= q.getTokenLimit()) {
            long resetAt = q.getWindowStart().plusSeconds(windowForPlan(q.getPlan())).toEpochMilli();
            throw new QuotaExceededException(q.getTokensUsed(), q.getTokenLimit(), resetAt);
        }
    }

    /** Adaugă tokens la consumul userului (input+output), resetând fereastra dacă a expirat. */
    @Transactional
    public void record(long tokens) {
        Long userId = CurrentUser.id();
        if (userId == null || tokens <= 0) return;
        UsageQuota q = repo.findByUserId(userId).orElseGet(() -> newFree(userId));
        rollover(q);
        q.setTokensUsed(q.getTokensUsed() + tokens);
        repo.save(q);
    }

    /** Schimbă planul userului (FREE/PRO/MAX), aplică limita și pornește o fereastră nouă. */
    @Transactional
    public UsageStatus upgrade(String plan) {
        Long userId = CurrentUser.id();
        if (userId == null) return current();
        String normalized = norm(plan);
        long limit = limitForPlan(normalized);
        UsageQuota q = repo.findByUserId(userId).orElseGet(() -> newFree(userId));
        q.setPlan(normalized);
        q.setTokenLimit(limit);
        q.setWindowStart(Instant.now());   // fereastră proaspătă la upgrade
        q.setTokensUsed(0);
        repo.save(q);
        return current();
    }
}
