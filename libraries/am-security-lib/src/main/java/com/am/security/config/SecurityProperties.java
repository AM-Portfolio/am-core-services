package com.am.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "am.security")
public class SecurityProperties {
    private boolean enabled = true;
    
    private List<String> publicPaths = new ArrayList<>(Arrays.asList(
        "/health",
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/api-docs/**"
    ));
    
    private List<String> protectedPaths = new ArrayList<>(Arrays.asList(
        "/api/**"
    ));
    
    private List<String> proxyPaths = new ArrayList<>(Arrays.asList(
        "/proxy-api/**"
    ));
    
    private String proxyName;
    
    private OidcProperties oidc = new OidcProperties();
    private LocalMockProperties localMock = new LocalMockProperties();

    @Data
    public static class OidcProperties {
        private String issuerUri;
        private String jwkSetUri;
        private String clientId;
        private String clientSecret;
    }

    @Data
    public static class LocalMockProperties {
        private boolean enabled = false;
        private String userId = "local-dev-user";
        private String username = "Local Developer";
        private String email = "local-dev@am.com";
        private List<String> roles = new ArrayList<>(Arrays.asList("ROLE_USER"));
    }

    public List<String> getProxyPaths() {
        List<String> paths = new ArrayList<>(proxyPaths);
        if (org.apache.commons.lang3.StringUtils.isNotBlank(proxyName)) {
            // Normalize path separator (e.g. subscriptions -> /subscriptions/**)
            String normalized = proxyName.trim().replaceAll("^/|/$", "");
            paths.add("/" + normalized + "/**");
        }
        return paths;
    }
}
