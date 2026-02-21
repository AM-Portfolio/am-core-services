package com.am.ai.controller;

import com.am.ai.dto.AiIntentResponse;
import com.am.ai.dto.ChatRequest;
import com.am.ai.service.AiOrchestratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiOrchestratorService aiService;

    @PostMapping("/chat")
    public ResponseEntity<AiIntentResponse> chat(@RequestBody ChatRequest request) {
        AiIntentResponse response = aiService.processChat(request);
        return ResponseEntity.ok(response);
    }
}
