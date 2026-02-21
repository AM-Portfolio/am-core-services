package com.am.ai.service;

import com.am.ai.dto.AiIntentResponse;
import com.am.ai.dto.ChatRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class AiOrchestratorService {

    /**
     * Processes the incoming chat request, queries the LLM to determine
     * the user's intent, and returns a structured response indicating which
     * UI widget should be rendered.
     */
    public AiIntentResponse processChat(ChatRequest request) {
        log.info("Processing AI Chat Request for user {}: {}", request.getUserId(), request.getMessage());
        
        // --- PLACEHOLDER FOR LANGCHAIN4J / SPRING AI LOGIC ---
        // 1. Send request.getMessage() and the prompt to LLM.
        // 2. Request JSON schema output matching AiIntentResponse.
        // 3. Map LLM JSON to AiIntentResponse DTO.
        
        // For local build verification of the skeleton, we return a mock intent:
        String messageLower = request.getMessage().toLowerCase();
        
        AiIntentResponse response = new AiIntentResponse();
        Map<String, Object> params = new HashMap<>();
        
        if (messageLower.contains("top") || messageLower.contains("movers") || messageLower.contains("gainers")) {
            response.setWidgetId("TOP_MOVERS");
            response.setMessage("Here are the top movers for your portfolio.");
            params.put("type", "STOCK"); // Mock param
        } else if (messageLower.contains("allocation") || messageLower.contains("sector") || messageLower.contains("breakdown")) {
            response.setWidgetId("ALLOCATION_PIE_CHART");
            response.setMessage("This is how your portfolio is allocated.");
            params.put("groupBy", "SECTOR"); // Mock param
        } else {
            response.setWidgetId("UNKNOWN");
            response.setMessage("I'm not exactly sure what widget to show for that. Could you clarify?");
        }
        
        response.setWidgetParams(params);
        return response;
    }
}
