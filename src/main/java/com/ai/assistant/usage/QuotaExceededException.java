package com.ai.assistant.usage;

/** Aruncată când userul a atins limita ferestrei curente. Mapează la HTTP 402. */
public class QuotaExceededException extends RuntimeException {
    private final long used;
    private final long limit;
    private final long resetAt;   // epoch millis când se resetează fereastra

    public QuotaExceededException(long used, long limit, long resetAt) {
        super("Ai atins limita de întrebări. Revii mai târziu sau treci la un plan.");
        this.used = used;
        this.limit = limit;
        this.resetAt = resetAt;
    }

    public long getUsed() { return used; }
    public long getLimit() { return limit; }
    public long getResetAt() { return resetAt; }
}
