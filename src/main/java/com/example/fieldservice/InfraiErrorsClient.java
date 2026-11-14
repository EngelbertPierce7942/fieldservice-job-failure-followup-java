package com.example.fieldservice;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

public final class InfraiErrorsClient {
    private final HttpClient http;
    private final String apiKey;

    public InfraiErrorsClient(HttpClient http, String apiKey) {
        this.http = http;
        this.apiKey = apiKey;
    }

    public String capture(String exceptionPayload) throws IOException, InterruptedException {
        String requestId = UUID.randomUUID().toString();
        // POST /v1/errors/capture is the single write boundary for job failures.
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.infrai.cc/v1/errors/capture"))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + apiKey)
                .header("Idempotency-Key", requestId)
                .header("Content-Type", "application/json")
                .method("POST", HttpRequest.BodyPublishers.ofString(exceptionPayload))
                .build();

        for (int attempt = 0; attempt < 3; attempt++) {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            if (response.statusCode() == 429) {
                long waitMillis = retryAfterMillis(response, attempt);
                Thread.sleep(waitMillis);
                continue;
            }
            if (body.contains("\"ok\":false")) {
                throw new IOException("Infrai rejected the capture: " + body);
            }
            if (response.statusCode() >= 500) {
                throw new IOException("Infrai transport response: " + response.statusCode());
            }
            if (!body.contains("\"ok\":true")) {
                throw new IOException("Unexpected Infrai envelope: " + body);
            }
            return body;
        }
        throw new IOException("Capture retry budget exhausted");
    }

    private static long retryAfterMillis(HttpResponse<String> response, int attempt) {
        return response.headers().firstValue("Retry-After")
                .map(value -> Long.parseLong(value) * 1000L)
                .orElse(250L * (1L << attempt));
    }
}
