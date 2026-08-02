package com.am.trade.client.auth;

/**
 * Optional per-request bearer for outbound trade HTTP calls.
 * Provided by the host app (e.g. am-mcp-server AuthTokenProvider).
 */
@FunctionalInterface
public interface TradeAuthTokenSupplier {
    String getToken();
}
