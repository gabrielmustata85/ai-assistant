package com.ai.assistant.usage;

/** Starea consumului pentru userul curent, în fereastra curentă. */
public record UsageStatus(long used, long limit, String plan, long resetAt, long windowSeconds) {
    public boolean exhausted() {
        return used >= limit;
    }
}
