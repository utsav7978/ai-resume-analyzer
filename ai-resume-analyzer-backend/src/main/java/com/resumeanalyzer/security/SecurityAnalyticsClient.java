package com.resumeanalyzer.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class SecurityAnalyticsClient {

    private static final Logger log =
            LoggerFactory.getLogger(SecurityAnalyticsClient.class);

    private final ObjectMapper objectMapper;

    private final HttpClient httpClient;

    private final String collectorUrl;

    private final String apiKey;

    // Thread-safe, immutable IP blocklist snapshot
    private final AtomicReference<Set<String>> blockedIps =
            new AtomicReference<>(Set.of());

    public SecurityAnalyticsClient(
            ObjectMapper objectMapper,
            @Value("${security.analytics.collector-url}") String collectorUrl,
            @Value("${security.analytics.api-key:}") String apiKey) {

        this.objectMapper = objectMapper;
        this.collectorUrl = collectorUrl.replaceAll("/+$", "");
        this.apiKey = apiKey;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    // Check whether an incoming IP is blocked
    public boolean isBlocked(String ip) {
        return ip != null && blockedIps.get().contains(ip);
    }

    // Fetch blocklist when application starts
    // @EventListener(ApplicationReadyEvent.class)
    // public void initialBlocklistRefresh() {
    //     refreshBlocklist();
    // }

    // Refresh blocklist every 30 seconds
    @Scheduled(
        initialDelayString = "${security.analytics.blocklist-refresh-ms:30000}",
        fixedDelayString = "${security.analytics.blocklist-refresh-ms:30000}"
    )
    public void refreshBlocklist() {

        if (apiKey == null || apiKey.isBlank()) {
            return;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        collectorUrl + "/api/v1/blocked-ips"))
                .timeout(Duration.ofSeconds(5))
                .header("Authorization", "Bearer " + apiKey)
                .GET()
                .build();

        httpClient.sendAsync(
                request,
                HttpResponse.BodyHandlers.ofString()
        ).thenAccept(response -> {

            if (response.statusCode() < 200 ||
                    response.statusCode() >= 300) {
                log.warn("Blocklist refresh returned HTTP {}",
                        response.statusCode());
                return;
            }

            try {
                JsonNode root =
                        objectMapper.readTree(response.body());

                JsonNode ips = root.path("blocked_ips");

                if (!ips.isArray()) {
                    return;
                }

                Set<String> updated = new HashSet<>();

                for (JsonNode ip : ips) {
                    if (ip.isTextual()) {
                        updated.add(ip.asText());
                    }
                }

                blockedIps.set(Set.copyOf(updated));

            } catch (IOException e) {
                log.warn("Could not parse blocked IP response");
            }

        }).exceptionally(error -> {
            log.debug("Blocklist refresh failed: {}",
                    error.toString());
            return null;
        });
    }

    // Send telemetry without blocking the application thread
    public void sendTelemetry(Map<String, Object> event) {

        if (apiKey == null || apiKey.isBlank()) {
            return;
        }

        try {
            String payload = objectMapper.writeValueAsString(event);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(collectorUrl + "/ingest"))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            httpClient.sendAsync(
                    request,
                    HttpResponse.BodyHandlers.discarding()
            ).thenAccept(response -> {

                if (response.statusCode() >= 400) {
                    log.warn("Telemetry collector returned HTTP {}",
                            response.statusCode());
                }

            }).exceptionally(error -> {
                log.debug("Telemetry delivery failed: {}",
                        error.toString());
                return null;
            });

        } catch (IOException e) {
            log.warn("Could not serialize telemetry event");
        }
    }
}