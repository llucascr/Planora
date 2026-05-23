package com.planora.backend.controller;

import com.planora.backend.model.Job.dto.CallbackRequest;
import com.planora.backend.model.issue.dto.AcceptedResponse;
import com.planora.backend.service.ApiPythonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/ia")
public class ApiPythonController {

    private final ApiPythonService apiPythonService;

    @PostMapping
    public ResponseEntity<AcceptedResponse> generateBacklog(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody String description,
            @RequestParam Long boardId,
            @RequestParam Long columnId,
            @RequestParam Long userId,
            @RequestParam String repository
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(apiPythonService.generateBacklog(description, boardId, columnId, jwt, userId, repository));
    }

    @PostMapping("/callback")
    public ResponseEntity<?> callback(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CallbackRequest callbackRequest
    ) {
        try {
            apiPythonService.saveBacklog(callbackRequest, jwt);
            return ResponseEntity.status(HttpStatus.OK).body("{success: true}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{success: false, message: " + e.getMessage() + "}");
        }
    }

}
