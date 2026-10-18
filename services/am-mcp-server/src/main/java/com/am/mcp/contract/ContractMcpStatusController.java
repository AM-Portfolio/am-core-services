package com.am.mcp.contract;

import com.am.mcp.config.AmMcpProperties;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Ops visibility for pack-backed contract MCP (not an MCP tool).
 */
@RestController
@RequestMapping(path = "/api/v1/contract-mcp", produces = MediaType.APPLICATION_JSON_VALUE)
public class ContractMcpStatusController {

    private final ContractMcpRegistry registry;
    private final AmMcpProperties props;

    public ContractMcpStatusController(ContractMcpRegistry registry, AmMcpProperties props) {
        this.registry = registry;
        this.props = props;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> m = new LinkedHashMap<>();
        AmMcpProperties.ContractMcp cfg = props.getContractMcp();
        m.put("enabled", registry.isEnabled());
        m.put("packsDir", registry.packsDirResolved());
        m.put("environment", cfg.getEnvironment());
        m.put("servicesAllowlist", cfg.getServices());
        m.put("groups", cfg.getGroups());
        m.put("includeAllGroups", cfg.isIncludeAllGroups());
        m.put("toolCount", registry.toolCount());
        m.put("toolsByService", registry.toolsByService());
        m.put(
                "sampleTools",
                registry.callbacks().stream()
                        .limit(12)
                        .map(c -> c.spec().name())
                        .collect(Collectors.toList()));
        m.put(
                "note",
                "Domain @Tools stay registered separately. Contract tools come from am-specs "
                        + "tools.json when packs-dir is mounted.");
        return m;
    }

    @PostMapping("/reload")
    public Map<String, Object> reload() {
        registry.reload();
        return status();
    }
}
