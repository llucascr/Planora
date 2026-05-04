package com.planora.backend.controller;

import com.planora.backend.service.WebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/webhook")
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/github/issues")
    public ResponseEntity<Void> handleIssueEvent(
            @RequestHeader(value = "X-GitHub-Event", required = false) String event,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody String rawPayload
    ) {
        webhookService.processIssueEvent(event, signature, rawPayload);
        return ResponseEntity.ok().build();
    }
}
