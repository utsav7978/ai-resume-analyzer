package com.resumeanalyzer.security;

import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class SecurityAnalyticsFilter extends OncePerRequestFilter {

    private final SecurityAnalyticsClient securityClient;

    @Value("${security.analytics.trust-forwarded-headers:false}")
    private boolean trustForwardedHeaders;

    public SecurityAnalyticsFilter(
            SecurityAnalyticsClient securityClient) {
        this.securityClient = securityClient;
    }

    // Skip actuator endpoints
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        return request.getRequestURI().startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = resolveClientIp(request);

        // 1. Active IP enforcement
        if (securityClient.isBlocked(clientIp)) {

            response.setStatus(
                    HttpServletResponse.SC_FORBIDDEN);

            response.setContentType(
                    MediaType.APPLICATION_JSON_VALUE);

            response.setCharacterEncoding("UTF-8");

            response.getWriter().write(
                    """
                    {
                      "error": "Forbidden",
                      "message": "Your IP has been blocked by security policy."
                    }
                    """
            );

            return;
        }

        // 2. Record request start time
        long startTime = System.nanoTime();

        // Prevent duplicate telemetry delivery
        AtomicBoolean sent = new AtomicBoolean(false);

        Runnable sendEvent = () -> {

            if (!sent.compareAndSet(false, true)) {
                return;
            }

            long latency = TimeUnit.NANOSECONDS.toMillis(
                    System.nanoTime() - startTime);

            Map<String, Object> metadata =
                    new LinkedHashMap<>();

            metadata.put("host", request.getHeader("Host"));

            addIfPresent(metadata, "latitude",
                    firstNonBlank(
                            request.getHeader("x-vercel-ip-latitude"),
                            request.getHeader("cf-iplatitude")));

            addIfPresent(metadata, "longitude",
                    firstNonBlank(
                            request.getHeader("x-vercel-ip-longitude"),
                            request.getHeader("cf-iplongitude")));

            addIfPresent(metadata, "city",
                    firstNonBlank(
                            request.getHeader("x-vercel-ip-city"),
                            request.getHeader("cf-ipcity")));

            addIfPresent(metadata, "country",
                    firstNonBlank(
                            request.getHeader("x-vercel-ip-country"),
                            request.getHeader("cf-ipcountry")));

            // 3. Construct telemetry event
            Map<String, Object> event =
                    new LinkedHashMap<>();

            event.put("event_id", UUID.randomUUID().toString());
            event.put("endpoint", request.getRequestURI());
            event.put("method", request.getMethod());
            event.put("status_code", response.getStatus());
            event.put("latency_ms", latency);
            event.put("ip", clientIp);

            event.put("user_agent",
                    request.getHeader("User-Agent") != null
                            ? request.getHeader("User-Agent")
                            : "");

            event.put("metadata", metadata);

            // 4. Send telemetry asynchronously
            securityClient.sendTelemetry(event);
        };

        // 5. Continue normal application processing
        try {
            filterChain.doFilter(request, response);

        } finally {

            // Also wait for asynchronous MVC responses to finish.
            if (request.isAsyncStarted()) {

                AsyncListener listener = new AsyncListener() {

                    @Override
                    public void onComplete(AsyncEvent event) {
                        sendEvent.run();
                    }

                    @Override
                    public void onTimeout(AsyncEvent event) {
                        sendEvent.run();
                    }

                    @Override
                    public void onError(AsyncEvent event) {
                        sendEvent.run();
                    }

                    @Override
                    public void onStartAsync(AsyncEvent event)
                            throws IOException {
                        event.getAsyncContext().addListener(this);
                    }
                };

                try {
                    request.getAsyncContext().addListener(listener);
                } catch (IllegalStateException e) {
                    sendEvent.run();
                }

            } else {
                sendEvent.run();
            }
        }
    }

    // Resolve client IP
    private String resolveClientIp(HttpServletRequest request) {

        if (trustForwardedHeaders) {

            String forwardedFor =
                    request.getHeader("X-Forwarded-For");

            if (forwardedFor != null &&
                    !forwardedFor.isBlank()) {
                return forwardedFor.split(",")[0].trim();
            }

            String realIp =
                    request.getHeader("X-Real-IP");

            if (realIp != null && !realIp.isBlank()) {
                return realIp.trim();
            }
        }

        return request.getRemoteAddr();
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    private void addIfPresent(
            Map<String, Object> map,
            String key,
            String value) {

        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }
}