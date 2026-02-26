package com.am.mcp.tools;

import com.am.market.client.service.MarketDataClientService;
import com.am.market.domain.enums.TimeFrame;
import com.am.mcp.util.ResponseHelper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Market data MCP tools.
 * Delegates to MarketDataClientService (am-market-client-lib SDK) — no raw HTTP.
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "am.tools.market", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class MarketTools {

    private final MarketDataClientService marketDataClientService;
    private final ResponseHelper          response;

    @Tool(name = "get_stock_quote",
          description = """
              Get the live market quote and metadata for a stock symbol.
              Returns: current price, 52-week high/low, P/E ratio, sector, market cap.
              Use this when asked:
                "What is the price of RELIANCE?", "What is TCS trading at?",
                "Show me HDFC Bank's stock info", "What is the current share price of INFY?"
              Symbol format: NSE symbols like RELIANCE, TCS, INFY, HDFCBANK.
              """)
    @CircuitBreaker(name = "am-market", fallbackMethod = "quoteFallback")
    public String getStockQuote(
            @ToolParam(description = "NSE stock symbol (e.g. 'RELIANCE', 'TCS', 'INFY').") String symbol) {
        var result = marketDataClientService.searchSecurities(List.of(symbol.toUpperCase()));
        return response.toJson(result);
    }

    public String quoteFallback(String symbol, Exception e) { return response.unavailable("am-market (quote)"); }

    @Tool(name = "search_instruments",
          description = """
              Search for stocks or ETFs by company name or partial symbol.
              Use this when asked: "Find HDFC related stocks", "Search for Tata stocks",
              "Look up Nifty ETFs", "What is the symbol for Infosys?"
              """)
    @CircuitBreaker(name = "am-market", fallbackMethod = "searchFallback")
    public String searchInstruments(
            @ToolParam(description = "Company name or partial symbol (e.g. 'HDFC', 'Tata', 'Nifty ETF').") String query) {
        var result = marketDataClientService.searchSecurities(List.of(query));
        return response.toJson(result);
    }

    public String searchFallback(String q, Exception e) { return response.unavailable("am-market (search)"); }

    @Tool(name = "get_historical_data",
          description = """
              Get OHLCV historical price data for a stock over a date range.
              Use this when asked: "Show me RELIANCE price history",
              "What was TCS price last month?", "Chart INFY for the last 3 months."
              interval: DAY (default), WEEK, or MONTH.
              from/to: YYYY-MM-DD format (e.g. "2025-01-01"). Leave blank for recent data.
              """)
    @CircuitBreaker(name = "am-market", fallbackMethod = "histFallback")
    public String getHistoricalData(
            @ToolParam(description = "Stock symbol (e.g. 'RELIANCE').") String symbol,
            @ToolParam(description = "DAY, WEEK, or MONTH (default: DAY).") String interval,
            @ToolParam(description = "Start date YYYY-MM-DD (optional).") String fromDate,
            @ToolParam(description = "End date YYYY-MM-DD (optional).") String toDate) {
        TimeFrame tf = switch ((interval != null ? interval.toUpperCase() : "DAY")) {
            case "WEEK", "1W" -> TimeFrame.WEEK;
            case "MONTH", "1M" -> TimeFrame.MONTH;
            default -> TimeFrame.DAY;
        };
        var result = marketDataClientService.getHistoricalDataBatch(
                symbol.toUpperCase(), fromDate, toDate, tf);
        return response.toJson(result);
    }

    public String histFallback(String s, String i, String f, String t, Exception e) { return response.unavailable("am-market (historical)"); }
}
