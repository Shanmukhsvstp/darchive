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

/** Thin wrapper around the Gemini Developer API generateContent endpoint. */
@Service
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final RestClient client;
    private final String apiKey;
    private final String model;

    public GeminiClient(@Value("$" + "{gemini.api-key:}") String apiKey,
                        @Value("$" + "{gemini.model:gemini-3.1-flash-lite}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        var factory = new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        factory.setReadTimeout(Duration.ofSeconds(120));
        this.client = RestClient.builder()
            .requestFactory(factory)
            .baseUrl("https://generativelanguage.googleapis.com")
            .defaultHeader("x-goog-api-key", apiKey)
            .build();
    }

    public static Map<String, Object> textMessage(String role, String text) {
        String geminiRole = "assistant".equals(role) || "model".equals(role) ? "model" : "user";
        return Map.of("role", geminiRole, "parts", List.of(Map.of("text", text)));
    }

    /** A user message carrying a PDF plus an instruction, using Gemini inline PDF input. */
    public static Map<String, Object> pdfMessage(byte[] pdf, String instruction) {
        Map<String, Object> document = Map.of("inlineData", Map.of(
            "mimeType", "application/pdf",
            "data", Base64.getEncoder().encodeToString(pdf)));
        Map<String, Object> text = Map.of("text", instruction);
        return Map.of("role", "user", "parts", List.of(document, text));
    }

    public String complete(String system, List<Map<String, Object>> messages, int maxTokens) {
        if (apiKey == null || apiKey.isBlank())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "AI is not configured on this server (missing GEMINI_API_KEY)");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", system))));
        body.put("contents", messages);
        body.put("generationConfig", Map.of("maxOutputTokens", maxTokens));

        try {
            Map<?, ?> response = client.post()
                .uri("/v1beta/models/{model}:generateContent", model)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

            StringBuilder output = new StringBuilder();
            if (response != null && response.get("candidates") instanceof List<?> candidates && !candidates.isEmpty()) {
                Object first = candidates.get(0);
                if (first instanceof Map<?, ?> candidate && candidate.get("content") instanceof Map<?, ?> content
                        && content.get("parts") instanceof List<?> parts) {
                    for (Object part : parts) {
                        if (part instanceof Map<?, ?> textPart && textPart.get("text") != null)
                            output.append(textPart.get("text"));
                    }
                }
            }
            String text = output.toString().strip();
            if (text.isEmpty())
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "The AI returned an empty answer");
            return text;
        } catch (RestClientResponseException e) {
            // Do not log response bodies: document and memo content may be echoed in provider errors.
            int code = e.getStatusCode().value();
            log.warn("Gemini API returned HTTP {}", code);
            if (code == 429 || code == 503)
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Gemini is busy or its free-tier limit was reached; try again later");
            if (code == 401 || code == 403)
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Gemini API key is invalid or the Gemini API is unavailable for this project");
            if (code == 400)
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Gemini could not process this request or document");
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "The Gemini request failed");
        }
    }
}