package com.am.mcp.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Traefik routes {@code /mcp/*} to this service, but the cluster strip-prefix
 * middleware does not include {@code /mcp}. Spring AI MCP listens on
 * {@code /sse} and {@code /message}, so rewrite the public prefix here.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class McpIngressPrefixFilter extends OncePerRequestFilter {

    private static final String PREFIX = "/mcp";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (uri != null && (uri.equals(PREFIX) || uri.startsWith(PREFIX + "/"))) {
            String rewritten = uri.equals(PREFIX) ? "/" : uri.substring(PREFIX.length());
            if (rewritten.isEmpty()) {
                rewritten = "/";
            }
            filterChain.doFilter(new RewrittenUriRequest(request, rewritten), response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static final class RewrittenUriRequest extends HttpServletRequestWrapper {
        private final String uri;

        private RewrittenUriRequest(HttpServletRequest request, String uri) {
            super(request);
            this.uri = uri;
        }

        @Override
        public String getRequestURI() {
            return uri;
        }

        @Override
        public String getServletPath() {
            return uri;
        }

        @Override
        public StringBuffer getRequestURL() {
            StringBuffer url = new StringBuffer();
            String scheme = getScheme();
            int port = getServerPort();
            url.append(scheme).append("://").append(getServerName());
            if (("http".equals(scheme) && port != 80) || ("https".equals(scheme) && port != 443)) {
                url.append(':').append(port);
            }
            url.append(uri);
            return url;
        }
    }
}
