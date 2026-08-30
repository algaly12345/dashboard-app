package com.realestate.admin.controller.api;

import com.realestate.admin.service.NotificationSendService;
import com.realestate.admin.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Sends a single push notification directly to one FCM device token -
 * meant for other backends (e.g. Laravel) to trigger a one-off notification
 * without going through the admin dashboard's UI.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationApiController {

    private final NotificationSendService notificationSendService;
    private final SettingsService settingsService;

    public record SendToTokenRequest(String fcmToken, String title, String body) {
    }

    @PostMapping("/send-to-token")
    public ResponseEntity<Map<String, Object>> sendToToken(
            @RequestHeader(value = "X-Api-Key", required = false) String apiKey,
            @RequestBody SendToTokenRequest request) {

        String expectedKey = settingsService.get("notification_api_key", "");
        if (expectedKey.isBlank() || apiKey == null || !apiKey.equals(expectedKey)) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "unauthorized"));
        }

        NotificationSendService.SendResult result = notificationSendService.sendToToken(
                request.fcmToken(), request.title(), request.body());

        if (result.sent()) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "messageId", result.messageId()
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", result.error() != null ? result.error() : "unknown_error"
            ));
        }
    }
}
