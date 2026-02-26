package com.am.mcp.config;

import com.am.mcp.auth.AuthTokenProvider;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * HTTP infrastructure beans:
 * - Pooled RestClient (for all internal calls)
 * - RestTemplate (for AI agent — long timeout)
 *
 * All timeout and pool values come from AmMcpProperties (application.yaml).
 */
@Configuration
public class McpInfraConfig {

        /**
         * Pooled Apache HttpClient — the foundation for all outbound calls.
         * Prevents connection exhaustion under load.
         */
        @Bean
        public CloseableHttpClient pooledHttpClient(AmMcpProperties props) {
                AmMcpProperties.HttpPool pool = props.getHttpPool();
                AmMcpProperties.Timeouts to = props.getTimeouts();

                PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
                cm.setMaxTotal(pool.getMaxTotal());
                cm.setDefaultMaxPerRoute(pool.getMaxPerRoute());

                RequestConfig requestConfig = RequestConfig.custom()
                                .setConnectTimeout(Timeout.of(to.getConnectMs(), TimeUnit.MILLISECONDS))
                                .setResponseTimeout(Timeout.of(to.getReadMs(), TimeUnit.MILLISECONDS))
                                .build();

                return HttpClients.custom()
                                .setConnectionManager(cm)
                                .setDefaultRequestConfig(requestConfig)
                                .evictExpiredConnections()
                                .evictIdleConnections(Timeout.of(pool.getTtlSeconds(), TimeUnit.SECONDS))
                                .build();
        }

        /**
         * RestClient backed by the connection pool — used by all tool classes
         * for fast internal API calls (am-analysis, am-auth etc).
         */
        @Bean
        public RestClient restClient(CloseableHttpClient httpClient) {
                return RestClient.builder()
                                .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
                                .build();
        }

        /**
         * RestTemplate — used only by AiAgentTools (needs a longer read timeout
         * for LLM calls which the shared RestClient doesn't have).
         */
        @Bean
        public RestTemplate aiAgentRestTemplate(RestTemplateBuilder builder,
                        AmMcpProperties props) {
                return builder
                                .setConnectTimeout(Duration.ofMillis(props.getTimeouts().getConnectMs()))
                                .setReadTimeout(Duration.ofMillis(props.getTimeouts().getAiAgentReadMs()))
                                .build();
        }
}
