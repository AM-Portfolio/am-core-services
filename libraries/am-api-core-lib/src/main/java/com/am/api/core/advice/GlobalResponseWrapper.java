package com.am.api.core.advice;

import com.am.api.core.model.ApiResponse;
import com.am.api.core.annotation.WrappedResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class GlobalResponseWrapper implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // Only wrap if the controller class or endpoint method is annotated with @WrappedResponse (opt-in)
        boolean hasOptIn = returnType.hasMethodAnnotation(WrappedResponse.class)
                || returnType.getDeclaringClass().isAnnotationPresent(WrappedResponse.class);
        
        if (!hasOptIn) {
            return false;
        }

        // Do not wrap if the response is already an ApiResponse or if it's a byte array (e.g. file download)
        return !returnType.getParameterType().isAssignableFrom(ApiResponse.class)
                && !returnType.getParameterType().isAssignableFrom(byte[].class)
                && !returnType.getDeclaringClass().getName().contains("springdoc")
                && !returnType.getDeclaringClass().getName().contains("swagger");
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        
        // If the body is already an ApiResponse (e.g., from an exception handler), return it as is
        if (body instanceof ApiResponse) {
            return body;
        }
        
        // Special case for Strings as Spring uses StringHttpMessageConverter directly
        if (body instanceof String) {
            // Ideally we'd map it to JSON, but for simplicity we wrap it.
            // Returning a JSON string representation of ApiResponse is required if StringHttpMessageConverter is used.
            // For true 10/10 modularity, one might implement a specific string converter, 
            // but returning the raw string or wrapping it requires careful consideration. 
            // We'll wrap it in standard ApiResponse for now (might cause ClassCastException if not handled in String converter,
            // so we skip Strings or convert to JSON string manually).
            // To be perfectly safe, we avoid wrapping raw Strings automatically unless converted to JSON.
            // Let's just wrap it, but in a real enterprise app, we serialize it to JSON using ObjectMapper.
            // For this design, we will just pass Strings through to avoid converter crashes.
            return body;
        }

        return ApiResponse.success(body);
    }
}


// test