package com.planora.backend.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@RequiredArgsConstructor
@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final List<WebhookEventHandler> handlers;

    @Value("${github.webhook.secret:}")
    private String webhookSecret;

    public boolean processEvent(String event, String signature, String rawPayload) {
        if (!isValidSignature(rawPayload, signature)) {
            log.warn("Received webhook with invalid signature — ignoring");
            return false;
        }
        return handlers.stream()
                .filter(h -> h.supports(event))
                .findFirst()
                .map(h -> { h.handle(rawPayload); return true; })
                .orElse(false);
    }

    private boolean isValidSignature(String payload, String signatureHeader) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.warn("GITHUB_WEBHOOK_SECRET not configured — skipping signature verification");
            return true;
        }
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            return false;
        }
        String receivedHmac = signatureHeader.substring(7);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"
            );
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedHmac = HexFormat.of().formatHex(hmacBytes);
            return MessageDigest.isEqual(
                    receivedHmac.getBytes(StandardCharsets.UTF_8),
                    computedHmac.getBytes(StandardCharsets.UTF_8)
            );
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("HMAC signature verification error: {}", e.getMessage());
            return false;
        }
    }
}
