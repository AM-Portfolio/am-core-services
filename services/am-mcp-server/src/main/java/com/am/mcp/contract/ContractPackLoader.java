package com.am.mcp.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Loads am-specs contract MCP packs from disk.
 *
 * <p>Accepted layouts under {@code packsDir}:
 * <ul>
 *   <li>{@code <serviceId>/tools.json} (+ optional {@code mcp/pack.json})</li>
 *   <li>{@code services/<serviceId>/tools.json} (am-specs checkout)</li>
 *   <li>{@code packages/mcp/services/<serviceId>/tools.json}</li>
 * </ul>
 * A service is registered only when {@code tools.json} exists (and optionally {@code mcp/pack.json}).
 */
public final class ContractPackLoader {

    private static final Logger log = LoggerFactory.getLogger(ContractPackLoader.class);

    private final ObjectMapper mapper;
    private final Set<String> allowedServices;
    private final Set<String> allowedGroups;
    private final boolean includeAllGroups;

    public ContractPackLoader(
            ObjectMapper mapper,
            Set<String> allowedServices,
            Set<String> allowedGroups,
            boolean includeAllGroups) {
        this.mapper = mapper;
        this.allowedServices = allowedServices == null ? Set.of() : Set.copyOf(allowedServices);
        this.allowedGroups = allowedGroups == null ? Set.of("core") : Set.copyOf(allowedGroups);
        this.includeAllGroups = includeAllGroups;
    }

    public record LoadResult(List<ContractToolSpec> tools, Map<String, Integer> toolsByService) {}

    public LoadResult load(Path packsDir) throws IOException {
        if (packsDir == null || !Files.isDirectory(packsDir)) {
            log.warn("Contract MCP packs-dir missing or not a directory: {}", packsDir);
            return new LoadResult(List.of(), Map.of());
        }

        List<Path> toolsFiles = findToolsJson(packsDir);
        // Prefer services/<id>/tools.json over packages/mcp/services/<id>/tools.json
        toolsFiles.sort((a, b) -> Integer.compare(pathPreference(a), pathPreference(b)));

        List<ContractToolSpec> out = new ArrayList<>();
        Map<String, Integer> byService = new LinkedHashMap<>();
        Map<String, Path> chosen = new LinkedHashMap<>();

        for (Path toolsPath : toolsFiles) {
            String serviceId = resolveServiceId(toolsPath);
            if (!allowedServices.isEmpty() && !allowedServices.contains(serviceId)) {
                log.info("Skip contract pack {} (not in allowlist)", serviceId);
                continue;
            }
            if (chosen.containsKey(serviceId)) {
                continue;
            }
            chosen.put(serviceId, toolsPath);
            if (!hasPackMarker(toolsPath)) {
                log.debug("Service {}: tools.json without mcp/pack.json (still loading)", serviceId);
            }
            int before = out.size();
            out.addAll(parseTools(serviceId, toolsPath));
            byService.put(serviceId, out.size() - before);
        }

        log.info(
                "Contract MCP loaded {} tools from {} (services={})",
                out.size(),
                packsDir.toAbsolutePath().normalize(),
                byService);
        return new LoadResult(List.copyOf(out), Map.copyOf(byService));
    }

    private boolean hasPackMarker(Path toolsPath) {
        Path parent = toolsPath.getParent();
        if (parent == null) {
            return false;
        }
        return Files.isRegularFile(parent.resolve("mcp").resolve("pack.json"))
                || Files.isRegularFile(parent.resolve("pack.json"));
    }

    private List<ContractToolSpec> parseTools(String serviceId, Path toolsPath) throws IOException {
        JsonNode root = mapper.readTree(toolsPath.toFile());
        JsonNode tools = root.path("tools");
        if (!tools.isArray()) {
            log.warn("No tools[] in {}", toolsPath);
            return List.of();
        }
        List<ContractToolSpec> list = new ArrayList<>();
        for (JsonNode t : tools) {
            try {
                ContractToolSpec spec = ContractToolSpec.fromToolNode(serviceId, t, mapper);
                if (!includeAllGroups
                        && !allowedGroups.isEmpty()
                        && !allowedGroups.contains(spec.group())
                        && !"all".equalsIgnoreCase(spec.group())) {
                    continue;
                }
                list.add(spec);
            } catch (Exception e) {
                log.warn("Skip bad tool in {}: {}", toolsPath, e.getMessage());
            }
        }
        return list;
    }

    static String resolveServiceId(Path toolsPath) {
        Path parent = toolsPath.getParent();
        if (parent == null) {
            return "unknown";
        }
        String name = parent.getFileName().toString();
        // packages/mcp/services/<id>/tools.json → parent is service id
        // services/<id>/tools.json → parent is service id
        // <id>/tools.json → parent is service id
        return name;
    }

    /** Lower = preferred. */
    static int pathPreference(Path p) {
        String s = p.toString().replace('\\', '/').toLowerCase(Locale.ROOT);
        if (s.contains("/services/") && !s.contains("/packages/mcp/")) {
            return 0;
        }
        if (s.contains("/packages/mcp/services/")) {
            return 1;
        }
        return 2;
    }

    static List<Path> findToolsJson(Path root) throws IOException {
        try (Stream<Path> walk = Files.walk(root, 6)) {
            return walk
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equals("tools.json"))
                    .filter(p -> {
                        String s = p.toString().replace('\\', '/').toLowerCase(Locale.ROOT);
                        // Prefer service trees; skip random nested copies under target/
                        return !s.contains("/target/") && !s.contains("/.git/");
                    })
                    .sorted()
                    .collect(Collectors.toList());
        }
    }
}
