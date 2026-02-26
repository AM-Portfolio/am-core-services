package com.am.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AM Platform MCP Server — Enterprise Edition
 *
 * A standalone Spring Boot service that exposes 12 tools to Claude via MCP (stdio).
 * Lives under am-core-services/services/ alongside am-analysis, am-gateway, etc.
 *
 * All tools use existing SDK beans (no raw HTTP for internal services):
 *   - MarketDataClientService  ← am-market-client-lib
 *   - TradeClientService       ← am-trade-client-lib
 *   - AnalysisRepository       ← am-analysis-adapter (MongoDB)
 *   - TokenExtractor           ← am-security-lib
 *
 * Enterprise features: circuit breakers, connection pooling, AOP tracing,
 * response truncation, env-var config with Spring profiles.
 *
 * Launch: java -jar am-mcp-server-1.0.0-SNAPSHOT.jar [--spring.profiles.active=prod]
 */
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
@EnableMongoRepositories(basePackages = "com.am.analysis.adapter.repository")
@ComponentScan(basePackages = {
        "com.am.mcp",
        "com.am.market.client",
        "com.am.trade.client"
})
public class AmMcpServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(AmMcpServerApplication.class, args);
    }
}
