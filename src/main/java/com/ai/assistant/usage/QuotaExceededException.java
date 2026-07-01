package com.ai.assistant.usage;

/** Aruncată când userul a atins limita lunară de tokens. Mapează la HTTP 402. */
public class QuotaExceededException extends RuntimeException {
    private final long used;
    private final long limit;

    public QuotaExceededException(long used, long limit) {
        super("Ai atins limita lunară de tokens (" + used + "/" + limit + ").");
        this.used = used;
        this.limit = limit;
    }

    public long getUsed() { return used; }
    public long getLimit() { return limit; }
}
