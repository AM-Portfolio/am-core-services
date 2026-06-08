package com.am.security.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A universal data model representing the authenticated user's profile across the AM platform.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmUserProfile {
    private String userId;
    private String username;
    private String email;
    @Builder.Default
    private List<String> roles = new ArrayList<>();
    private String token;
    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();
}
