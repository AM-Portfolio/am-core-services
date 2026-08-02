package com.am.trade.client.auth;

/**
 * Optional per-request bearer for outbound trade HTTP calls.
 * Host apps (e.g. am-mcp-server {@code AuthTokenProvider}) supply the live user JWT
 * so each TradeClientService request can authenticate without a static apiKey.
 */
@FunctionalInterface
public interface TradeAuthTokenSupplier {
    /** @return bearer token for the current request, or null/blank if unavailable */
    String getToken();
}
