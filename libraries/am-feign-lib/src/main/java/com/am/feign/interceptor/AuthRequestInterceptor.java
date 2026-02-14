package com.am.feign.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Propagates the Authorization header from the current incoming request
 * to any outgoing Feign client requests.
 */
@Slf4j
@Component
public class AuthRequestInterceptor implements RequestInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String token = request.getHeader(AUTHORIZATION_HEADER);
            
            if (token != null) {
                log.debug("Propagating Authorization token to Feign call");
                template.header(AUTHORIZATION_HEADER, token);
            } else {
                log.warn("No Authorization header found in current request context");
            }
        } else {
            log.warn("No request attributes found in RequestContextHolder - are you calling this outside a request thread?");
        }
    }
}
