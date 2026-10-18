package com.am.mcp.contract;

import com.am.mcp.auth.AuthTokenProvider;
import com.am.mcp.config.AmMcpProperties;
import com.am.mcp.util.ResponseHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Executes am-specs contract tools by HTTP using pack {@code _meta}
 * (method/path/base_urls) and executor-injected auth — same contract agents get via MCP.
 *
 * <p>Java enterprise SDKs ({@code com.asrax:*}) share this OpenAPI surface; HTTP keeps
 * every pack-backed service callable without a Maven dep per service. Prefer SDK in
 * domain {@code @Tool} classes when curated UX is needed.
 */
@Component
public class ContractHttpExecutor {

    private static final Logger log = LoggerFactory.getLogger(ContractHttpExecutor.class);
    private static final Set<String> BODY_METHODS = Set.of("POST", "PUT", "PATCH");

    private final RestClient restClient;
    private final AuthTokenProvider auth;
    private final AmMcpProperties props;
    private final ResponseHelper response;
    private final ObjectMapper mapper;

    public ContractHttpExecutor(
            RestClient restClient,
            AuthTokenProvider auth,
            AmMcpProperties props,
            ResponseHelper response,
            ObjectMapper mapper) {
        this.restClient = restClient;
        this.auth = auth;
        this.props = props;
        this.response = response;
        this.mapper = mapper;
    }

    public String execute(ContractToolSpec tool, String toolInputJson) {
        try {
            Map<String, Object> args = parseArgs(toolInputJson);
            String env = props.getContractMcp().getEnvironment();
            String base = tool.resolveBaseUrl(env);
            if (base == null || base.isBlank()) {
                return response.failure(
                        tool.name(),
                        "NO_BASE_URL",
                        "No base_url for env=" + env + " on " + tool.name(),
                        false,
                        "Set am.contract-mcp.environment or pack base_urls.");
            }

            String path = tool.path();
            for (String p : tool.pathParamNames()) {
                Object v = args.get(p);
                if (v == null) {
                    return response.failure(
                            tool.name(),
                            "MISSING_PATH_PARAM",
                            "Required path param: " + p,
                            false,
                            "Pass '" + p + "' in tool arguments.");
                }
                path = path.replace("{" + p + "}", encodePathSegment(String.valueOf(v)));
            }

            UriComponentsBuilder ub = UriComponentsBuilder
                    .fromUriString(trimSlash(base) + ensureLeadingSlash(path));
            for (String q : tool.queryParamNames()) {
                Object v = args.get(q);
                if (v != null) {
                    ub.queryParam(q, v);
                }
            }
            // Params without location → query (OpenAPI residual)
            for (Map.Entry<String, Object> e : args.entrySet()) {
                if (tool.paramLocations().containsKey(e.getKey())) {
                    continue;
                }
                if (e.getValue() != null && !"body".equals(e.getKey())) {
                    ub.queryParam(e.getKey(), e.getValue());
                }
            }

            URI uri = ub.encode().build().toUri();
            HttpMethod method = HttpMethod.valueOf(tool.method());
            HttpHeaders headers = auth.authHeaders();
            headers.setAccept(MediaType.parseMediaTypes(MediaType.APPLICATION_JSON_VALUE));

            Object body = null;
            if (BODY_METHODS.contains(tool.method())) {
                body = args.get("body");
                if (body == null) {
                    // Flatten non-path/query args as JSON body
                    Map<String, Object> bodyMap = new LinkedHashMap<>();
                    for (Map.Entry<String, Object> e : args.entrySet()) {
                        String loc = tool.paramLocations().getOrDefault(e.getKey(), "");
                        if ("path".equalsIgnoreCase(loc) || "query".equalsIgnoreCase(loc)) {
                            continue;
                        }
                        bodyMap.put(e.getKey(), e.getValue());
                    }
                    if (!bodyMap.isEmpty()) {
                        body = bodyMap;
                    }
                }
            }

            log.debug("Contract MCP {} {} {}", tool.method(), uri, tool.name());

            RestClient.RequestBodySpec spec = restClient.method(method).uri(uri).headers(h -> h.addAll(headers));
            ResponseEntity<String> entity;
            if (body != null) {
                entity = spec.contentType(MediaType.APPLICATION_JSON).body(body).retrieve().toEntity(String.class);
            } else {
                entity = spec.retrieve().toEntity(String.class);
            }

            Map<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("ok", entity.getStatusCode().is2xxSuccessful());
            envelope.put("status", entity.getStatusCode().value());
            envelope.put("service", tool.serviceId());
            envelope.put("tool", tool.name());
            envelope.put("sdk_api", tool.sdkApi());
            envelope.put("op_id", tool.opId());
            envelope.put("body", parseBodyMaybe(entity.getBody()));
            return response.toJson(envelope);
        } catch (RestClientResponseException e) {
            return response.failure(
                    tool.name(),
                    "HTTP_" + e.getStatusCode().value(),
                    truncate(e.getResponseBodyAsString(), 2000),
                    e.getStatusCode().is5xxServerError(),
                    "HTTP " + e.getStatusCode().value());
        } catch (Exception e) {
            log.warn("Contract tool {} failed: {}", tool.name(), e.toString());
            return response.errorJson(tool.name(), e);
        }
    }

    private Map<String, Object> parseArgs(String toolInputJson) throws Exception {
        if (toolInputJson == null || toolInputJson.isBlank()) {
            return new LinkedHashMap<>();
        }
        return mapper.readValue(toolInputJson, new TypeReference<LinkedHashMap<String, Object>>() {});
    }

    private Object parseBodyMaybe(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return "";
        }
        try {
            if (t.startsWith("{") || t.startsWith("[")) {
                return mapper.readTree(t);
            }
        } catch (Exception ignored) {
            // keep string
        }
        return raw;
    }

    private static String trimSlash(String base) {
        if (base.endsWith("/")) {
            return base.substring(0, base.length() - 1);
        }
        return base;
    }

    private static String ensureLeadingSlash(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private static String encodePathSegment(String v) {
        return v.replace("/", "%2F");
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
