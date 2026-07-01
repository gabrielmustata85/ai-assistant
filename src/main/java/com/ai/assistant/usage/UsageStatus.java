package com.ai.assistant.usage;

/** Starea consumului pentru userul curent, în perioada curentă. */
public record UsageStatus(long used, long limit, String period) {
    public boolean exhausted() {
        return used >= limit;
    }
}
