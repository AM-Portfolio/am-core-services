package com.am.mcp.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Typed config for all 'am.*' keys in application.yaml.
 * This is the single source of truth — change application.yaml, not code.
 *
 * Override any value at runtime with an env var (uppercase, dots →
 * underscores):
 * am.auth.url → AM_AUTH_URL
 * am.tools.portfolio.enabled → AM_TOOLS_PORTFOLIO_ENABLED
 */
@Getter
@Component
@ConfigurationProperties(prefix = "am")
public class AmMcpProperties {

    private final Defaults defaults = new Defaults();
    private final Auth auth = new Auth();
    private final Services services = new Services();
    private final Timeouts timeouts = new Timeouts();
    private final Mcp mcp = new Mcp();
    private final HttpPool httpPool = new HttpPool();
    private final Tools tools = new Tools();
    private final ContractMcp contractMcp = new ContractMcp();

    // ── Nested config classes ─────────────────────────────────────────────────

    @Getter
    public static class Defaults {
        private String userId = "user1";

        public void setUserId(String v) {
            this.userId = v;
        }
    }

    @Getter
    public static class Auth {
        private String url = "http://localhost:8001";
        private String staticToken = "";
        private String username = "";
        private String password = "";
        private int refreshBufferSec = 60;

        public void setUrl(String v) {
            this.url = v;
        }

        public void setStaticToken(String v) {
            this.staticToken = v;
        }

        public void setUsername(String v) {
            this.username = v;
        }

        public void setPassword(String v) {
            this.password = v;
        }

        public void setRefreshBufferSec(int v) {
            this.refreshBufferSec = v;
        }
    }

    @Getter
    public static class Services {
        private String aiAgentUrl = "http://localhost:8100";
        /** Chat path appended to aiAgentUrl. am-fin-agent uses /v1/ai/chat; fin-portfolio-agent uses /api/v1/ai/chat. */
        private String aiAgentChatPath = "/api/v1/ai/chat";
        private String portfolioUrl = "http://localhost:8060";
        private String tradeUrl = "http://localhost:8040";
        private String marketDataUrl = "http://localhost:8050";

        public void setAiAgentUrl(String v) {
            this.aiAgentUrl = v;
        }

        public void setAiAgentChatPath(String v) {
            this.aiAgentChatPath = v;
        }

        public void setPortfolioUrl(String v) {
            this.portfolioUrl = v;
        }

        public void setTradeUrl(String v) {
            this.tradeUrl = v;
        }

        public void setMarketDataUrl(String v) {
            this.marketDataUrl = v;
        }
    }

    @Getter
    public static class Timeouts {
        private int connectMs = 3000;
        private int readMs = 15000;
        private int aiAgentReadMs = 90000;

        public void setConnectMs(int v) {
            this.connectMs = v;
        }

        public void setReadMs(int v) {
            this.readMs = v;
        }

        public void setAiAgentReadMs(int v) {
            this.aiAgentReadMs = v;
        }
    }

    @Getter
    public static class Mcp {
        /** Max characters in any tool response. Protects LLM context window. */
        private int maxResponseChars = 8000;
        /** Hard timeout for any single tool call in milliseconds. */
        private int toolTimeoutMs = 20000;

        public void setMaxResponseChars(int v) {
            this.maxResponseChars = v;
        }

        public void setToolTimeoutMs(int v) {
            this.toolTimeoutMs = v;
        }
    }

    @Getter
    public static class HttpPool {
        private int maxTotal = 50;
        private int maxPerRoute = 20;
        private int ttlSeconds = 30;

        public void setMaxTotal(int v) {
            this.maxTotal = v;
        }

        public void setMaxPerRoute(int v) {
            this.maxPerRoute = v;
        }

        public void setTtlSeconds(int v) {
            this.ttlSeconds = v;
        }
    }

    /**
     * Per-tool-group on/off toggles.
     * Set am.tools.<group>.enabled=false in application.yaml to disable a group.
     * Env override: AM_TOOLS_PORTFOLIO_ENABLED=false
     */
    @Getter
    public static class Tools {
        private final ToolGroup portfolio = new ToolGroup(true);
        private final ToolGroup basket = new ToolGroup(true);
        private final ToolGroup trade = new ToolGroup(true);
        private final ToolGroup market = new ToolGroup(true);
        private final ToolGroup analysis = new ToolGroup(true);
        private final ToolGroup aiAgent = new ToolGroup(true);

        @Getter
        public static class ToolGroup {
            private boolean enabled;

            public ToolGroup(boolean defaultEnabled) {
                this.enabled = defaultEnabled;
            }

            public void setEnabled(boolean v) {
                this.enabled = v;
            }
        }
    }

    /**
     * Pack-backed contract MCP (am-specs tools.json). When enabled and packs-dir
     * contains service tools, tools are registered alongside curated domain {@code @Tool}s.
     */
    @Getter
    public static class ContractMcp {
        private boolean enabled = false;
        /** Directory with am-specs services/ or packages/mcp/services/ trees. */
        private String packsDir = "";
        /** Target env key in tool _meta.base_urls (dev|preprod|prod). */
        private String environment = "dev";
        /** Empty = all services that have tools.json under packs-dir. */
        private List<String> services = new ArrayList<>();
        /** Product groups from grouping.yaml / tools _meta.group. */
        private List<String> groups = new ArrayList<>(List.of("core"));
        private boolean includeAllGroups = false;

        public void setEnabled(boolean v) {
            this.enabled = v;
        }

        public void setPacksDir(String v) {
            this.packsDir = v;
        }

        public void setEnvironment(String v) {
            this.environment = v;
        }

        public void setServices(List<String> v) {
            this.services = v != null ? v : new ArrayList<>();
        }

        public void setGroups(List<String> v) {
            this.groups = v != null ? v : new ArrayList<>();
        }

        public void setIncludeAllGroups(boolean v) {
            this.includeAllGroups = v;
        }
    }
}
