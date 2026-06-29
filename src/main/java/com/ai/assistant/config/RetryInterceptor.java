package com.ai.assistant.config;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;

@Slf4j
public class RetryInterceptor implements Interceptor {
    private final int maxRetries;

    public RetryInterceptor(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        IOException lastException = null;
        Response response = null;

        for (int i = 0; i <= maxRetries; i++) {
            try {
                response = chain.proceed(request);

                // Handle rate limiting (429) and server errors (5xx)
                if (!response.isSuccessful()) {
                    if (response.code() == 429 || response.code() >= 500) {
                        // Close the response body to avoid leaks
                        try (ResponseBody body = response.body()) {
                            log.warn("Attempt {} failed with status {}: {}", i, response.code(),
                                    body != null ? body.string() : "no body");
                        }

                        // Exponential backoff
                        long waitTime = (long) Math.pow(2, i) * 1000;
                        Thread.sleep(waitTime);
                        continue;
                    }
                }
                return response;
            } catch (IOException e) {
                lastException = e;
                log.warn("Attempt {} failed with exception: {}", i, e.getMessage());

                // Exponential backoff for IO exceptions
                try {
                    long waitTime = (long) Math.pow(2, i) * 1000;
                    Thread.sleep(waitTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted during retry", ie);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        if (response != null) {
            throw new IOException("Failed after " + maxRetries + " attempts. Last status: " + response.code());
        }
        throw lastException != null ? lastException : new IOException("All retries failed");
    }
}

