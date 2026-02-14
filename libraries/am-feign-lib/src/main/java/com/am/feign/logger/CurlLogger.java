package com.am.feign.logger;

import feign.Logger;
import feign.Request;
import feign.Response;
import feign.Util;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;

@Slf4j
public class CurlLogger extends Logger {

    private final org.slf4j.Logger logger;

    public CurlLogger() {
        this(CurlLogger.class);
    }

    public CurlLogger(Class<?> clazz) {
        this(LoggerFactory.getLogger(clazz));
    }

    public CurlLogger(org.slf4j.Logger logger) {
        this.logger = logger;
    }

    @Override
    protected void logRequest(String configKey, Level logLevel, Request request) {
        if (logger.isDebugEnabled()) {
            String curlCmd = toCurl(request);
            logger.debug("\n[{}] ---> {} {}\n{}\n---> END HTTP ({}-byte body)",
                    configKey, request.httpMethod(), request.url(), curlCmd,
                    request.body() != null ? request.body().length : 0);
        }
    }

    @Override
    protected Response logAndRebufferResponse(String configKey, Level logLevel, Response response, long elapsedTime)
            throws IOException {
        if (logger.isDebugEnabled()) {
            String reason = response.reason() != null && !response.reason().isEmpty() ? " " + response.reason() : "";
            int status = response.status();
            logger.debug("\n[{}] <--- HTTP/1.1 {} {} ({}ms)", configKey, status, reason, elapsedTime);

            if (response.body() != null && !(status == 204 || status == 205)) {
                // Buffer body
                byte[] bodyData = Util.toByteArray(response.body().asInputStream());
                String bodyStr = new String(bodyData, StandardCharsets.UTF_8);

                logger.debug("\n[{}] Response Body:\n{}\n<--- END HTTP ({}-byte body)", configKey, bodyStr,
                        bodyData.length);
                return response.toBuilder().body(bodyData).build();
            } else {
                logger.debug("\n[{}] <--- END HTTP ({}-byte body)", configKey,
                        response.body() != null ? response.body().length() : 0);
            }
        }
        return response;
    }

    @Override
    protected void log(String configKey, String format, Object... args) {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format(methodTag(configKey) + format, args));
        }
    }

    private String toCurl(Request request) {
        StringBuilder curl = new StringBuilder("curl -X ");
        curl.append(request.httpMethod().name()).append(" '").append(request.url()).append("'");

        for (Map.Entry<String, Collection<String>> header : request.headers().entrySet()) {
            for (String value : header.getValue()) {
                curl.append(" -H '").append(header.getKey()).append(": ").append(value).append("'");
            }
        }

        if (request.body() != null && request.body().length > 0) {
            String body = new String(request.body(), StandardCharsets.UTF_8);
            curl.append(" -d '").append(body.replace("'", "'\\''")).append("'");
        }

        return curl.toString();
    }
}
