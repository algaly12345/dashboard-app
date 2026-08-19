package com.realestate.admin.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.realestate.admin.service.SettingsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Wires up Firebase Cloud Messaging for the notifications feature, reading
 * the service account JSON and enabled flag from business_settings
 * (Settings -> Firebase Admin SDK) instead of a static file - same
 * live-editable pattern as R2/NHC. This bean is only built once at
 * application startup though, so changing the credentials from Settings
 * requires a restart (kubectl rollout restart) to take effect.
 */
@Configuration
@Slf4j
public class FirebaseConfig {

    private final SettingsService settingsService;

    public FirebaseConfig(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Bean
    public FirebaseMessagingHolder firebaseMessagingHolder() {
        boolean enabled = "1".equals(settingsService.get("firebase_enabled", "0"));
        if (!enabled) {
            log.warn("Firebase push notifications disabled (Settings -> Firebase Admin SDK toggle is off). " +
                    "The Send screen will accept requests but won't actually deliver anything.");
            return new FirebaseMessagingHolder(null, "not_enabled");
        }

        String json = settingsService.get("firebase_service_account_json", "");
        if (json.isBlank()) {
            log.warn("Firebase is enabled but no service account JSON is saved in Settings - push notifications disabled.");
            return new FirebaseMessagingHolder(null, "credentials_missing");
        }

        try (var in = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(in))
                    .build();
            FirebaseApp app = FirebaseApp.getApps().isEmpty()
                    ? FirebaseApp.initializeApp(options)
                    : FirebaseApp.getInstance();
            return new FirebaseMessagingHolder(FirebaseMessaging.getInstance(app), null);
        } catch (Exception e) {
            log.error("Failed to initialize Firebase from saved credentials - push notifications disabled.", e);
            return new FirebaseMessagingHolder(null, e.getMessage());
        }
    }
}
