package com.am.mcp.tools;

import com.am.mcp.auth.AuthTokenProvider;
import com.am.mcp.config.AmMcpProperties;
import com.am.mcp.util.ResponseHelper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * AI Agent MCP tool.
 * Relays natural-language questions to am-fin-agent (Python FastAPI, port 8100).
 *
 * This is the ONLY tool that uses HTTP directly — the fin-agent has no Java SDK.
 * Uses a dedicated RestTemplate with a longer read timeout (am.timeouts.ai-agent-read-ms).
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "am.tools.ai-agent", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class AiAgentTools {

    private final RestTemplate       aiAgentRestTemplate;   // long-timeout bean from McpInfraConfig
    private final AuthTokenProvider  authTokenProvider;
    private final AmMcpProperties    props;
    private final ResponseHelper     response;

    @Tool(name = "ask_finance_agent",
          description = """
              Ask a complex, multi-step natural-language finance question to the AM AI Finance Agent.
              The agent orchestrates across portfolio, market, and trade data autonomously.
              Use this for analytical questions that need reasoning, not just data retrieval:
                "Analyse my portfolio concentration risk vs NIFTY 50."
                "Which of my holdings are underperforming their sector?"
                "Should I rebalance given my current allocation?"
                "Summarise all my trades this month and their net impact."
                "What is my portfolio's beta compared to the market?"
              For simple data lookups, use the specific tools (get_holdings, get_stock_quote, etc.)
              question: plain English — no special syntax needed.
              sessionId: optional — pass the same ID to continue a conversation.
              """)
    @CircuitBreaker(name = "am-ai-agent", fallbackMethod = "agentFallback")
    public String askFinanceAgent(
            @ToolParam(description = "User ID.") String userId,
            @ToolParam(description = "Natural-language finance question.") String question,
            @ToolParam(description = "Session ID for conversation continuity (optional).") String sessionId) {
        String uid = resolve(userId);
        String url = props.getServices().getAiAgentUrl() + "/api/v1/ai/chat";

        var headers = authTokenProvider.authHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "message",   question,
                "userId",    uid,
                "sessionId", (sessionId != null && !sessionId.isBlank()) ? sessionId : ""
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> resp = aiAgentRestTemplate.postForObject(
                url, new HttpEntity<>(body, headers), Map.class);

        if (resp == null) return "{\"error\":\"Empty response from am-fin-agent\"}";

        // Return human-readable message from AiIntentResponse
        Object msg = resp.get("message");
        return msg != null ? msg.toString() : response.toJson(resp);
    }

    public String agentFallback(String u, String q, String s, Exception e) {
        return response.unavailable("am-fin-agent");
    }

    private String resolve(String userId) {
        return (userId != null && !userId.isBlank()) ? userId : props.getDefaults().getUserId();
    }
}
