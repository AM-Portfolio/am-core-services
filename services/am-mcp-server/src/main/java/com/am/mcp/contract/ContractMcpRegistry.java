package com.am.mcp.contract;

import com.am.mcp.config.AmMcpProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Holds pack-backed contract tools loaded at startup (when enabled).
 */
@Component
public class ContractMcpRegistry {

    private static final Logger log = LoggerFactory.getLogger(ContractMcpRegistry.class);

    private final AmMcpProperties props;
    private final ObjectMapper mapper;
    private final ContractHttpExecutor executor;

    private volatile List<ContractToolCallback> callbacks = List.of();
    private volatile Map<String, Integer> toolsByService = Map.of();
    private volatile String packsDirResolved = "";
    private volatile boolean enabled;

    public ContractMcpRegistry(
            AmMcpProperties props, ObjectMapper mapper, ContractHttpExecutor executor) {
        this.props = props;
        this.mapper = mapper;
        this.executor = executor;
    }

    @PostConstruct
    public void init() {
        reload();
    }

    public synchronized void reload() {
        AmMcpProperties.ContractMcp cfg = props.getContractMcp();
        enabled = cfg.isEnabled();
        if (!enabled) {
            callbacks = List.of();
            toolsByService = Map.of();
            packsDirResolved = "";
            log.info("Contract MCP disabled (am.contract-mcp.enabled=false)");
            return;
        }
        String dir = cfg.getPacksDir();
        if (dir == null || dir.isBlank()) {
            log.warn("Contract MCP enabled but am.contract-mcp.packs-dir is empty");
            callbacks = List.of();
            return;
        }
        Path packsDir = Path.of(dir).toAbsolutePath().normalize();
        packsDirResolved = packsDir.toString();
        try {
            ContractPackLoader loader = new ContractPackLoader(
                    mapper,
                    Set.copyOf(cfg.getServices()),
                    Set.copyOf(cfg.getGroups()),
                    cfg.isIncludeAllGroups());
            ContractPackLoader.LoadResult result = loader.load(packsDir);
            List<ContractToolCallback> built = new ArrayList<>();
            for (ContractToolSpec spec : result.tools()) {
                built.add(new ContractToolCallback(spec, executor));
            }
            callbacks = List.copyOf(built);
            toolsByService = new LinkedHashMap<>(result.toolsByService());
        } catch (Exception e) {
            log.error("Failed to load contract MCP packs from {}: {}", packsDir, e.toString());
            callbacks = List.of();
            toolsByService = Map.of();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String packsDirResolved() {
        return packsDirResolved;
    }

    public Map<String, Integer> toolsByService() {
        return toolsByService;
    }

    public int toolCount() {
        return callbacks.size();
    }

    public ToolCallback[] toolCallbacks() {
        return callbacks.toArray(ToolCallback[]::new);
    }

    public List<ContractToolCallback> callbacks() {
        return callbacks;
    }
}
