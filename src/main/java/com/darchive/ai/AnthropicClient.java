package com.darchive.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Thin wrapper around the Anthropic Messages API. */
@Service
public class AnthropicClient {

    private static final Logger log = LoggerFactory.getLogger(AnthropicClient.class);

    private final RestClient client;
    private final String apiKey;
    private final String model;

    public AnthropicClient(@Value("${anthropic.api-key:}") String apiKey,
                           @Value("${anthropic.model}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        var factory = new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        factory.setReadTimeout(Duration.ofSeconds(120));
        this.client = RestClient.builder()
            .requestFactory(factory)
            .baseUrl("https://api.anthropic.com")
            .defaultHeader("x-api-key", apiKey)
            .defaultHeader("anthropic-version", "2023-06-01")
            .build();
    }

    public static Map<String, Object> textMessage(String role, String text) {
        return Map.of("role", role, "content", text);
    }

    /** A user message carrying a PDF plus an instruction. */
    public static Map<String, Object> pdfMessage(byte[] pdf, String instruction) {
        Map<String, Object> doc = Map.of("type", "document", "source", Map.of(
            "type", "base64", "media_type", "application/pdf",
            "data", Base64.getEncoder().encodeToString(pdf)));
        Map<String, Object> text = Map.of("type", "text", "text", instruction);
        return Map.of("role", "user", "content", List.of(doc, text));
    }

    public String complete(String system, List<Map<String, Object>> messages, int maxTokens) {
        if (apiKey == null || apiKey.isBlank())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "AI is not configured on this server (missing ANTHROPIC_API_KEY)");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("max_tokens", maxTokens);
        body.put("system", system);
        body.put("messages", messages);

        try {
            Map<?, ?> res = client.post().uri("/v1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

            StringBuilder out = new StringBuilder();
            if (res != null && res.get("content") instanceof List<?> blocks) {
                for (Object b : blocks) {
                    if (b instanceof Map<?, ?> m && "text".equals(m.get("type")) && m.get("text") != null)
                        out.append(m.get("text"));
                }
            }
            String text = out.toString().strip();
            if (text.isEmpty())
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "The AI returned an empty answer");
            return text;
        } catch (RestClientResponseException e) {
            log.warn("Anthropic API error {}: {}", e.getStatusCode().value(), e.getResponseBodyAsString());
            int code = e.getStatusCode().value();
            if (code == 429 || code == 529)
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "The AI is busy, try again in a minute");
            if (code == 400)
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "The AI could not read this document (it may be too long - limit is about 100 pages)");
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "The AI request failed");
        }
    }
}
