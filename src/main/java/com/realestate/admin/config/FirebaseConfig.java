package com.realestate.admin.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.InputStream;

/**
 * Wires up Firebase Cloud Messaging for the notifications feature. Disabled
 * by default (app.firebase.enabled=false) so the app runs fine before the
 * service account key is in place - see application.yml for setup steps.
 */
@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${app.firebase.enabled:false}")
    private boolean enabled;

    @Value("${app.firebase.credentials-path:classpath:firebase-service-account.json}")
    private String credentialsPath;

    @Value("${app.firebase.project-id:}")
    private String projectId;

    private final ResourceLoader resourceLoader;

    public FirebaseConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Bean
    public FirebaseMessagingHolder firebaseMessagingHolder() {
        if (!enabled) {
            log.warn("Firebase push notifications disabled (app.firebase.enabled=false). " +
                    "The Send screen will accept requests but won't actually deliver anything.");
            return new FirebaseMessagingHolder(null, "not_enabled");
        }
        try {
            Resource resource = resourceLoader.getResource(credentialsPath);
            if (!resource.exists()) {
                log.warn("Firebase service account file not found at {} - push notifications disabled.", credentialsPath);
                return new FirebaseMessagingHolder(null, "credentials_missing");
            }
            try (InputStream in = resource.getInputStream()) {
                FirebaseOptions.Builder options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(in));
                if (projectId != null && !projectId.isBlank()) {
                    options.setProjectId(projectId);
                }
                FirebaseApp app = FirebaseApp.getApps().isEmpty()
                        ? FirebaseApp.initializeApp(options.build())
                        : FirebaseApp.getInstance();
                return new FirebaseMessagingHolder(FirebaseMessaging.getInstance(app), null);
            }
        } catch (Exception e) {
            log.error("Failed to initialize Firebase - push notifications disabled.", e);
            return new FirebaseMessagingHolder(null, e.getMessage());
        }
    }
}
