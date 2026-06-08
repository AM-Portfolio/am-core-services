package com.am.security.context;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class UserContext {
    private static final ThreadLocal<AmUserProfile> userProfileHolder = new ThreadLocal<>();

    public static void setUserProfile(AmUserProfile profile) {
        userProfileHolder.set(profile);
    }
    
    public static AmUserProfile getUserProfile() {
        return userProfileHolder.get();
    }

    public static void setUserId(String userId) { 
        AmUserProfile profile = getOrCreateProfile();
        profile.setUserId(userId);
    }
    
    public static String getUserId() { 
        AmUserProfile profile = userProfileHolder.get();
        return profile != null ? profile.getUserId() : null;
    }

    /**
     * Foolproof extraction: Throws a 401 Unauthorized if the user is not authenticated.
     * Use this method in controllers/services to guarantee 100% security enforcement.
     */
    public static String getUserIdOrThrow() {
        String userId = getUserId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated or token missing");
        }
        return userId;
    }

    public static void setEmail(String email) { 
        AmUserProfile profile = getOrCreateProfile();
        profile.setEmail(email);
    }
    
    public static String getEmail() { 
        AmUserProfile profile = userProfileHolder.get();
        return profile != null ? profile.getEmail() : null;
    }

    public static void setToken(String token) { 
        AmUserProfile profile = getOrCreateProfile();
        profile.setToken(token);
    }
    
    public static String getToken() { 
        AmUserProfile profile = userProfileHolder.get();
        return profile != null ? profile.getToken() : null;
    }

    public static void clear() {
        userProfileHolder.remove();
    }
    
    private static AmUserProfile getOrCreateProfile() {
        AmUserProfile profile = userProfileHolder.get();
        if (profile == null) {
            profile = new AmUserProfile();
            userProfileHolder.set(profile);
        }
        return profile;
    }
}
