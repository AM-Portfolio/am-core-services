package com.am.mcp.contract;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * One OpenAPI-derived MCP tool from am-specs {@code tools.json}.
 */
public final class ContractToolSpec {

    private final String serviceId;
    private final String name;
    private final String description;
    private final String inputSchemaJson;
    private final String method;
    private final String path;
    private final Map<String, String> baseUrls;
    private final String defaultBaseUrl;
    private final String route;
    private final String group;
    private final String sdkApi;
    private final String opId;
    private final Map<String, String> paramLocations;

    public ContractToolSpec(
            String serviceId,
            String name,
            String description,
            String inputSchemaJson,
            String method,
            String path,
            Map<String, String> baseUrls,
            String defaultBaseUrl,
            String route,
            String group,
            String sdkApi,
            String opId,
            Map<String, String> paramLocations) {
        this.serviceId = Objects.requireNonNull(serviceId, "serviceId");
        this.name = Objects.requireNonNull(name, "name");
        this.description = description != null ? description : name;
        this.inputSchemaJson = inputSchemaJson != null ? inputSchemaJson : "{\"type\":\"object\",\"properties\":{}}";
        this.method = method != null ? method.toUpperCase() : "GET";
        this.path = path != null ? path : "/";
        this.baseUrls = baseUrls != null ? Map.copyOf(baseUrls) : Map.of();
        this.defaultBaseUrl = defaultBaseUrl != null ? defaultBaseUrl : "";
        this.route = route != null ? route : "";
        this.group = group != null ? group : "all";
        this.sdkApi = sdkApi != null ? sdkApi : "";
        this.opId = opId != null ? opId : "";
        this.paramLocations = paramLocations != null ? Map.copyOf(paramLocations) : Map.of();
    }

    public static ContractToolSpec fromToolNode(String serviceId, JsonNode tool, com.fasterxml.jackson.databind.ObjectMapper mapper)
            throws Exception {
        JsonNode fn = tool.path("function");
        String name = fn.path("name").asText(null);
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("tool missing function.name");
        }
        String description = fn.path("description").asText(name);
        JsonNode params = fn.path("parameters");
        String schema = params.isMissingNode() || params.isNull()
                ? "{\"type\":\"object\",\"properties\":{}}"
                : mapper.writeValueAsString(params);

        JsonNode meta = tool.path("_meta");
        String method = meta.path("method").asText("get");
        String path = meta.path("path").asText("/");
        String defaultBase = meta.path("base_url").asText("");
        Map<String, String> bases = new LinkedHashMap<>();
        JsonNode bu = meta.path("base_urls");
        if (bu.isObject()) {
            bu.fields().forEachRemaining(e -> bases.put(e.getKey(), e.getValue().asText("")));
        }
        Map<String, String> locations = new LinkedHashMap<>();
        JsonNode pl = meta.path("param_locations");
        if (pl.isObject()) {
            pl.fields().forEachRemaining(e -> locations.put(e.getKey(), e.getValue().asText("query")));
        }
        return new ContractToolSpec(
                serviceId,
                name,
                description,
                schema,
                method,
                path,
                bases,
                defaultBase,
                meta.path("route").asText(""),
                meta.path("group").asText("all"),
                meta.path("sdk_api").asText(""),
                meta.path("op_id").asText(""),
                locations);
    }

    public String resolveBaseUrl(String env) {
        if (env != null && baseUrls.containsKey(env) && !baseUrls.get(env).isBlank()) {
            return baseUrls.get(env);
        }
        if (!defaultBaseUrl.isBlank()) {
            return defaultBaseUrl;
        }
        return baseUrls.values().stream().filter(u -> u != null && !u.isBlank()).findFirst().orElse("");
    }

    public String serviceId() {
        return serviceId;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public String inputSchemaJson() {
        return inputSchemaJson;
    }

    public String method() {
        return method;
    }

    public String path() {
        return path;
    }

    public Map<String, String> baseUrls() {
        return baseUrls;
    }

    public String route() {
        return route;
    }

    public String group() {
        return group;
    }

    public String sdkApi() {
        return sdkApi;
    }

    public String opId() {
        return opId;
    }

    public Map<String, String> paramLocations() {
        return paramLocations;
    }

    public List<String> pathParamNames() {
        return paramLocations.entrySet().stream()
                .filter(e -> "path".equalsIgnoreCase(e.getValue()))
                .map(Map.Entry::getKey)
                .toList();
    }

    public List<String> queryParamNames() {
        return paramLocations.entrySet().stream()
                .filter(e -> "query".equalsIgnoreCase(e.getValue()))
                .map(Map.Entry::getKey)
                .toList();
    }

    public static Map<String, Object> emptyArgs() {
        return Collections.emptyMap();
    }
}
