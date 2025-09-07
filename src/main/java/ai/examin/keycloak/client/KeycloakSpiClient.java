package ai.examin.keycloak.client;

import ai.examin.keycloak.dto.KeycloakEvent;
import ai.examin.keycloak.dto.TokenResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Logger;
import java.util.logging.Level;

public class KeycloakSpiClient {
    private static final Logger logger = Logger.getLogger(KeycloakSpiClient.class.getName());
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String keycloakUrl;
    private final String realm;
    private final String clientId;
    private final String clientSecret;

    public KeycloakSpiClient() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.objectMapper = new ObjectMapper();

        // Get configuration from system properties or environment variables if present
        this.keycloakUrl = getProperty("keycloak.server-url", "KEYCLOAK_SERVER_URL", "http://localhost:8080");
        this.realm = getProperty("keycloak.realm", "KEYCLOAK_REALM", "examinai");
        this.clientId = getProperty("keycloak.auth-service.client-id", "KEYCLOAK_AUTH_SERVICE_CLIENT_ID", "keycloak-spi-client");
        this.clientSecret = getProperty("keycloak.auth-service.client-secret", "KEYCLOAK_AUTH_SERVICE_CLIENT_SECRET", "BecDB2enoNJ7pclu1czeKji0Smy7nwJD");
    }

    public boolean sendPostRequest(String url, KeycloakEvent payload, String bearerToken) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + bearerToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload));

            HttpRequest request = requestBuilder.build();

            logger.info("Sending HTTP request to: " + url);
            logger.info("Request payload: " + jsonPayload);

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                logger.info("Successfully sent POST request. Response: " + response.body());
                return true;
            } else {
                logger.warning("Failed to send POST request. HTTP " + response.statusCode() + ": " + response.body());
                return false;
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to send POST request to " + url, e);
            return false;
        }
    }

    public String getServiceAccessToken() {
        try {
            String url = String.format("%s/realms/%s/protocol/openid-connect/token", keycloakUrl, realm);
            String payload = String.format("client_id=%s&client_secret=%s&grant_type=client_credentials",
                clientId, clientSecret);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                TokenResponse tokenResponse = objectMapper.readValue(response.body(), TokenResponse.class);
                logger.info("Successfully obtained access token");
                return tokenResponse.getAccessToken();
            } else {
                logger.warning("Failed to get access token. HTTP " + response.statusCode() + ": " + response.body());
                throw new RuntimeException("Failed to get access token. HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to get admin access token", e);
            throw new RuntimeException("Failed to get access token", e);
        }
    }

    private String getProperty(String systemProperty, String envVariable, String defaultValue) {
        // Try system property first
        String value = System.getProperty(systemProperty);
        if (value != null && !value.trim().isEmpty()) {
            return value;
        }

        // Try environment variable
        value = System.getenv(envVariable);
        if (value != null && !value.trim().isEmpty()) {
            return value;
        }

        // Return default
        return defaultValue;
    }
}
