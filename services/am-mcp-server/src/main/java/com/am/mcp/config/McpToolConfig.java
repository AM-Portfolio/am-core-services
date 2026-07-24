package com.am.mcp.config;

import com.am.mcp.tools.AiAgentTools;
import com.am.mcp.tools.AnalysisTools;
import com.am.mcp.tools.BasketTools;
import com.am.mcp.tools.MarketTools;
import com.am.mcp.tools.PortfolioTools;
import com.am.mcp.tools.TradeTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Registers domain {@code @Tool} services with the Spring AI MCP server.
 * Unwraps AOP proxies (e.g. Resilience4j {@code @CircuitBreaker}) so
 * {@link MethodToolCallbackProvider} can see {@code @Tool} on the target class.
 */
@Configuration
public class McpToolConfig {

    @Bean
    public ToolCallbackProvider amDomainTools(
            ObjectProvider<PortfolioTools> portfolioTools,
            ObjectProvider<BasketTools> basketTools,
            ObjectProvider<MarketTools> marketTools,
            ObjectProvider<TradeTools> tradeTools,
            ObjectProvider<AnalysisTools> analysisTools,
            ObjectProvider<AiAgentTools> aiAgentTools) {

        List<Object> toolObjects = new ArrayList<>();
        portfolioTools.ifAvailable(t -> toolObjects.add(unwrap(t)));
        basketTools.ifAvailable(t -> toolObjects.add(unwrap(t)));
        marketTools.ifAvailable(t -> toolObjects.add(unwrap(t)));
        tradeTools.ifAvailable(t -> toolObjects.add(unwrap(t)));
        analysisTools.ifAvailable(t -> toolObjects.add(unwrap(t)));
        aiAgentTools.ifAvailable(t -> toolObjects.add(unwrap(t)));

        return MethodToolCallbackProvider.builder()
                .toolObjects(toolObjects.toArray())
                .build();
    }

    private static Object unwrap(Object bean) {
        Object target = AopProxyUtils.getSingletonTarget(bean);
        return target != null ? target : bean;
    }
}
