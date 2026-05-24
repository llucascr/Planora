package com.planora.backend.service;

public interface WebhookEventHandler {
    boolean supports(String eventType);
    void handle(String rawPayload);
}
