package ai.examin.keycloak.dto;

import java.util.Map;

public class KeycloakEvent {
    private String eventType;
    private String userId;
    private String timestamp;
    private Map<String, String> details;

    public KeycloakEvent(String eventType, String userId, String timestamp, Map<String, String> details) {
        this.eventType = eventType;
        this.userId = userId;
        this.timestamp = timestamp;
        this.details = details;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, String> getDetails() {
        return details;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }
}
