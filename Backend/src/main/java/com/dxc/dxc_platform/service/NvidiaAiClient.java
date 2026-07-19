package com.dxc.dxc_platform.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class NvidiaAiClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${nvidia.api.key}")
    private String apiKey;

    private static final String URL =
            "https://integrate.api.nvidia.com/v1/chat/completions";
    private static final String MODEL =
            "mistralai/mistral-large-3-675b-instruct-2512";


    public String chat(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "model",             MODEL,
                "messages",          List.of(Map.of("role", "user", "content", prompt)),
                "temperature",       0.15,
                "top_p",             1.0,
                "frequency_penalty", 0.0,
                "presence_penalty",  0.0,
                "max_tokens",        512,
                "stream",            false
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);


        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                ResponseEntity<Map> response =
                        restTemplate.postForEntity(URL, request, Map.class);

                List<Map<String, Object>> choices =
                        (List<Map<String, Object>>) response.getBody().get("choices");
                Map<String, Object> message =
                        (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");

            } catch (HttpClientErrorException.TooManyRequests e) {
                if (attempt == maxRetries) {
                    throw new RuntimeException(
                            "Limite API dépassée après " + maxRetries + " tentatives. " +
                                    "Réessayez dans quelques secondes.", e);
                }

                try {
                    long waitMs = (long) Math.pow(2, attempt) * 1000L;
                    System.out.println("⏳ Rate limit 429 — attente " + waitMs + "ms (tentative " + attempt + "/" + maxRetries + ")");
                    Thread.sleep(waitMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interruption pendant retry", ie);
                }
            } catch (Exception e) {
                throw new RuntimeException(
                        "Erreur appel NVIDIA/Mistral : " + e.getMessage(), e);
            }
        }
        throw new RuntimeException("Échec appel API après " + maxRetries + " tentatives");
    }
}