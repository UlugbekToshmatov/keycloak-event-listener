package ai.examin.keycloak.listeners;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.util.logging.Logger;

public class UserSyncEventListenerFactory implements EventListenerProviderFactory {
    private static final Logger logger = Logger.getLogger(UserSyncEventListenerFactory.class.getName());

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new UserSyncEventListener();
    }

    @Override
    public void init(Config.Scope config) {
        logger.info("UserSyncEventListenerFactory initialized");
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        logger.info("UserSyncEventListenerFactory post-initialized");
    }

    @Override
    public void close() {
        logger.info("UserSyncEventListenerFactory closed");
    }

    @Override
    public String getId() {
        return "user-sync-event-listener";
    }
}
