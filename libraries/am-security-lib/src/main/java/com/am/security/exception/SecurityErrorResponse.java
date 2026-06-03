package com.am.security.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Structured JSON payload returned by the security lib on any auth failure.
 *
 * Sample 401 response:
 * <pre>
 * {
 *   "status":    401,
 *   "error":     "Unauthorized",
 *   "message":   "JWT token has expired.",
 *   "detail":    "The token expired at 2026-06-03T01:00:00Z. Clock-skew tolerance is 60 seconds.",
 *   "hint":      "Re-authenticate or use your refresh token to obtain a new access token.",
 *   "path":      "/analysis/portfolio/summary",
 *   "timestamp": "2026-06-03T02:11:53Z"
 * }
 * </pre>
 *
 * Uses a plain builder (no Lombok) so the lib stays dependency-light.
 * Optional fields ({@code detail}, {@code hint}) are omitted from JSON when null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SecurityErrorResponse {

    private final int    status;
    private final String error;
    private final String message;
    private final String detail;   // extended diagnostic — what went wrong exactly
    private final String hint;     // actionable advice for the caller
    private final String path;
    private final String timestamp;

    private SecurityErrorResponse(Builder b) {
        this.status    = b.status;
        this.error     = b.error;
        this.message   = b.message;
        this.detail    = b.detail;
        this.hint      = b.hint;
        this.path      = b.path;
        this.timestamp = Instant.now().toString();
    }

    // ── Getters (Jackson serialisation) ───────────────────────────────────────

    public int    getStatus()    { return status;    }
    public String getError()     { return error;     }
    public String getMessage()   { return message;   }
    public String getDetail()    { return detail;    }
    public String getHint()      { return hint;      }
    public String getPath()      { return path;      }
    public String getTimestamp() { return timestamp; }

    // ── Factory helpers — one per distinct failure scenario ───────────────────

    /** 401 – No Authorization header / no Bearer token present. */
    public static SecurityErrorResponse missingToken(String path) {
        return new Builder()
                .status(401).error("Unauthorized")
                .message("Missing authentication token.")
                .detail("No 'Authorization: Bearer <token>' header was found in the request.")
                .hint("Obtain a token: POST {issuer}/protocol/openid-connect/token " +
                      "with grant_type=password or client_credentials.")
                .path(path).build();
    }

    /** 401 – Bearer prefix present but value is not a valid JWT. */
    public static SecurityErrorResponse malformedToken(String path) {
        return new Builder()
                .status(401).error("Unauthorized")
                .message("Malformed JWT token.")
                .detail("The Authorization header value is not a valid JWT. " +
                        "A JWT must have three base64url-encoded segments separated by dots: " +
                        "<header>.<payload>.<signature>.")
                .hint("Decode your token at https://jwt.io to verify its structure.")
                .path(path).build();
    }

    /** 401 – JWT signature verification failed (wrong key / wrong JWKS endpoint). */
    public static SecurityErrorResponse invalidSignature(String path, String jwksUri) {
        return new Builder()
                .status(401).error("Unauthorized")
                .message("JWT signature verification failed.")
                .detail("The token signature could not be verified against the configured JWKS. " +
                        "This usually means the token was issued by a different realm/client, " +
                        "or the signing key has been rotated.")
                .hint("Configured JWKS URI: [" + nvl(jwksUri) + "]. " +
                      "Check OIDC_JWKS_URI and ensure the token is issued by the correct realm.")
                .path(path).build();
    }

    /** 401 – Token exp claim is in the past. */
    public static SecurityErrorResponse tokenExpired(String path, String expiredAt) {
        return new Builder()
                .status(401).error("Unauthorized")
                .message("JWT token has expired.")
                .detail("The token expired at " + nvl(expiredAt) + ". " +
                        "Clock-skew tolerance is 60 seconds.")
                .hint("Re-authenticate or use your refresh token to obtain a new access token.")
                .path(path).build();
    }

    /** 401 – Token iss claim does not match the configured issuer-uri. */
    public static SecurityErrorResponse invalidIssuer(String path, String expected, String actual) {
        return new Builder()
                .status(401).error("Unauthorized")
                .message("JWT issuer mismatch.")
                .detail("Expected issuer: [" + nvl(expected) + "], " +
                        "token issuer: [" + nvl(actual) + "]. " +
                        "Ensure the token is issued by the correct Keycloak realm.")
                .hint("Check OIDC_ISSUER_URI in the service environment — " +
                      "it must match the 'iss' claim inside the token.")
                .path(path).build();
    }

    /** 401 – Token aud claim does not include the expected client-id. */
    public static SecurityErrorResponse invalidAudience(String path, String expectedClientId) {
        return new Builder()
                .status(401).error("Unauthorized")
                .message("JWT audience mismatch.")
                .detail("The token's 'aud' claim does not include the required client: [" +
                        nvl(expectedClientId) + "]. " +
                        "Verify the token is requested with the correct scope/audience.")
                .hint("Check OAUTH2_CLIENT_ID in the service environment and ensure the " +
                      "Keycloak client is configured to include this audience.")
                .path(path).build();
    }

    /**
     * 401 – JWKS endpoint unreachable (identity server is down or URI is misconfigured).
     * The service cannot verify any token until this is resolved.
     */
    public static SecurityErrorResponse jwksUnavailable(String path, String jwksUri) {
        return new Builder()
                .status(401).error("Unauthorized")
                .message("Cannot reach the identity provider to verify token keys.")
                .detail("The JWKS endpoint [" + nvl(jwksUri) + "] is unreachable. " +
                        "This is a server-side configuration or network connectivity issue.")
                .hint("Verify the identity server is running and that OIDC_JWKS_URI is " +
                      "reachable from within this service's network.")
                .path(path).build();
    }

    /**
     * 401 – OAuth2 client credentials (client-id / client-secret) rejected by the auth server.
     * Applies when the service itself exchanges credentials for a token internally.
     */
    public static SecurityErrorResponse invalidClientCredentials(String path, String clientId) {
        return new Builder()
                .status(401).error("Unauthorized")
                .message("OAuth2 client credentials rejected by the identity provider.")
                .detail("The auth server refused the client credentials for client-id: [" +
                        nvl(clientId) + "]. " +
                        "The client-id or client-secret is incorrect, or the client is disabled in Keycloak.")
                .hint("Check OAUTH2_CLIENT_ID and OAUTH2_CLIENT_SECRET in the service environment " +
                      "and verify the client is enabled and the secret matches in Keycloak admin console.")
                .path(path).build();
    }

    /** 401 – Token nbf (not-before) claim is in the future. */
    public static SecurityErrorResponse tokenNotYetValid(String path, String validFrom) {
        return new Builder()
                .status(401).error("Unauthorized")
                .message("JWT token is not yet valid.")
                .detail("The token's 'nbf' (not-before) claim is [" + nvl(validFrom) + "]. " +
                        "The token cannot be accepted before that time.")
                .hint("Check that system clocks are synchronised between the auth server and this service.")
                .path(path).build();
    }

    /** 401 – Generic / uncategorised authentication failure (fallback). */
    public static SecurityErrorResponse authenticationFailed(String path, String reason) {
        return new Builder()
                .status(401).error("Unauthorized")
                .message("Authentication failed.")
                .detail(reason != null ? reason : "The request could not be authenticated.")
                .hint("Check the Authorization header, token expiry, issuer, and OIDC configuration.")
                .path(path).build();
    }

    /** 403 – Authenticated but lacks the required roles / granted authorities. */
    public static SecurityErrorResponse accessDenied(String path, String grantedAuthorities) {
        return new Builder()
                .status(403).error("Forbidden")
                .message("Access denied. Insufficient permissions for this resource.")
                .detail("Your token is valid but your account does not hold the required role(s). " +
                        (grantedAuthorities != null && !grantedAuthorities.isBlank()
                                ? "Current granted authorities: [" + grantedAuthorities + "]."
                                : "No authorities could be extracted from your token."))
                .hint("Contact your administrator to assign the required role(s) in Keycloak. " +
                      "Check the 'realm_access.roles' claim in your decoded JWT.")
                .path(path).build();
    }

    // ── Internal helper ────────────────────────────────────────────────────────

    private static String nvl(String v) { return v != null ? v : "not configured"; }

    // ── Builder ────────────────────────────────────────────────────────────────

    public static final class Builder {
        private int status;
        private String error;
        private String message;
        private String detail;
        private String hint;
        private String path;

        public Builder status(int v)     { this.status  = v; return this; }
        public Builder error(String v)   { this.error   = v; return this; }
        public Builder message(String v) { this.message = v; return this; }
        public Builder detail(String v)  { this.detail  = v; return this; }
        public Builder hint(String v)    { this.hint    = v; return this; }
        public Builder path(String v)    { this.path    = v; return this; }

        public SecurityErrorResponse build() { return new SecurityErrorResponse(this); }
    }
}
