package ai.examin.keycloak.listeners;

import ai.examin.keycloak.dto.KeycloakEvent;
import ai.examin.keycloak.client.KeycloakSpiClient;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.ResourceType;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserSyncEventListener implements EventListenerProvider {
    private static final Logger logger = Logger.getLogger(UserSyncEventListener.class.getName());
    private final KeycloakSpiClient keycloakSpiClient;
    private final String webhookUrl;

    public UserSyncEventListener() {
        this.keycloakSpiClient = new KeycloakSpiClient();
        this.webhookUrl = getWebhookUrl();
    }

    @Override
    public void onEvent(Event event) {
        if (event == null) return;

        try {
            switch (event.getType()) {
                case REGISTER:
                    sendUserEventAsync("USER_REGISTERED", event.getUserId(), event.getDetails());
                    break;
                case VERIFY_EMAIL:
                    sendUserEventAsync("EMAIL_VERIFIED", event.getUserId(), event.getDetails());
                    break;
                case UPDATE_PASSWORD:
                    sendUserEventAsync("PASSWORD_UPDATED", event.getUserId(), event.getDetails());
                    break;
                case LOGIN:
                    // Only sync on social login
                    if (isSocialLogin(event)) {
                        sendUserEventAsync("SOCIAL_LOGIN", event.getUserId(), event.getDetails());
                    }
                    break;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing user event: " + event.getType(), e);
        }
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
        if (adminEvent == null) return;

        try {
            if (adminEvent.getResourceType() == ResourceType.USER) {
                switch (adminEvent.getOperationType()) {
                    case CREATE:
                        sendAdminEventAsync("USER_CREATED_ADMIN", adminEvent);
                        break;
                    case UPDATE:
                        sendAdminEventAsync("USER_UPDATED_ADMIN", adminEvent);
                        break;
                    case DELETE:
                        sendAdminEventAsync("USER_DELETED_ADMIN", adminEvent);
                        break;
                    default:
                        // Ignore other operations
                        break;
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error processing admin event: " + adminEvent.getOperationType(), e);
        }
    }

    private boolean isSocialLogin(Event event) {
        Map<String, String> details = event.getDetails();
        return details != null &&
            (details.containsKey("identity_provider") ||
                details.containsKey("identity_provider_identity"));
    }

    private void sendUserEventAsync(String eventType, String userId, Map<String, String> details) {
        // Use CompletableFuture for async processing to avoid blocking Keycloak
        CompletableFuture.runAsync(() -> sendUserEvent(eventType, userId, details));
    }

    private void sendAdminEventAsync(String eventType, AdminEvent adminEvent) {
        CompletableFuture.runAsync(() -> sendAdminEvent(eventType, adminEvent));
    }

    private void sendUserEvent(String eventType, String userId, Map<String, String> details) {
        try {
            // Get access token
            String accessToken = keycloakSpiClient.getServiceAccessToken();

            // Create payload
            KeycloakEvent payload = new KeycloakEvent(eventType, userId, Instant.now().toString(), details);

            // Send request
            boolean success = keycloakSpiClient.sendPostRequest(webhookUrl, payload, accessToken);

            if (success) {
                logger.info("Successfully synced user event: " + eventType + " for user: " + userId);
            } else {
                logger.warning("Failed to sync user event: " + eventType + " for user: " + userId);
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to send user event: " + eventType, e);
        }
    }

    private void sendAdminEvent(String eventType, AdminEvent adminEvent) {
        try {
            // Get access token
            String accessToken = keycloakSpiClient.getServiceAccessToken();

            // Create payload for admin events (simpler structure)
            KeycloakEvent payload = new KeycloakEvent(
                eventType,
                extractUserIdFromResourcePath(adminEvent.getResourcePath()),
                Instant.now().toString(),
                null
            );

            // Send request
            boolean success = keycloakSpiClient.sendPostRequest(webhookUrl, payload, accessToken);

            if (success) {
                logger.info("Successfully synced admin event: " + eventType);
            } else {
                logger.warning("Failed to sync admin event: " + eventType);
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to send admin event: " + eventType, e);
        }
    }

    private String extractUserIdFromResourcePath(String resourcePath) {
        // Extract user ID from resource path like "users/12345-67890-abcde"
        if (resourcePath != null && resourcePath.contains("users/")) {
            String[] parts = resourcePath.split("users/");
            if (parts.length > 1) {
                return parts[1].split("/")[0]; // Get the first part after users/
            }
        }
        return null;
    }

    @Override
    public void close() {
        logger.info("UserSyncEventListener closed");
    }

    private String getWebhookUrl() {
        // Try system property first
        String url = System.getProperty("user.sync.webhook.url");
        if (url != null && !url.trim().isEmpty()) {
            return url;
        }

        // Try environment variable
        url = System.getenv("USER_SYNC_WEBHOOK_URL");
        if (url != null && !url.trim().isEmpty()) {
            return url;
        }

        // Default fallback
        return "http://localhost:8070/api/v1/auth/sync/user-event";
    }
}
