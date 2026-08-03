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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Market data MCP tools — quotes, search, history, movers, sectors, indices.
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "am.tools.market", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class MarketTools {

    private final MarketDataClientService marketDataClientService;
    private final ResponseHelper response;

    @Tool(name = "get_stock_quote",
          description = """
              [market] Get the live market quote (LTP / price) for a stock symbol.
              Returns: current price and quote fields from market data.
              Use this when asked:
                "What is the price of RELIANCE?", "What is TCS trading at?",
                "Show me HDFC Bank's stock info", "What is the current share price of INFY?"
              Symbol format: NSE symbols like RELIANCE, TCS, INFY, HDFCBANK.
              Do NOT use search_instruments for live price — use this tool.
              """)
    @CircuitBreaker(name = "am-market", fallbackMethod = "quoteFallback")
    public String getStockQuote(
            @ToolParam(description = "NSE stock symbol (e.g. 'RELIANCE', 'TCS', 'INFY').") String symbol) {
        try {
            Map<String, Object> quotes = marketDataClientService.getQuotes(
                    symbol.toUpperCase(), null, true);
            return response.toJson(quotes);
        } catch (Exception e) {
            return response.errorJson("get_stock_quote", e);
        }
    }

    public String quoteFallback(String symbol, Exception e) {
        return response.unavailable("am-market (quote)");
    }

    @Tool(name = "search_instruments",
          description = """
              [market] Search for stocks or ETFs by company name or partial symbol.
              Use this when asked: "Find HDFC related stocks", "Search for Tata stocks",
              "Look up Nifty ETFs", "What is the symbol for Infosys?"
              Do NOT use for live price — use get_stock_quote.
              """)
    @CircuitBreaker(name = "am-market", fallbackMethod = "searchFallback")
    public String searchInstruments(
            @ToolParam(description = "Company name or partial symbol (e.g. 'HDFC', 'Tata', 'Nifty ETF').") String query) {
        try {
            var result = marketDataClientService.searchSecurities(List.of(query));
            return response.toJson(result);
        } catch (Exception e) {
            return response.errorJson("search_instruments", e);
        }
    }

    public String searchFallback(String q, Exception e) {
        return response.unavailable("am-market (search)");
    }

    @Tool(name = "get_historical_data",
          description = """
              [market] Get OHLCV historical price data for a stock over a date range.
              Use this when asked: "Show me RELIANCE price history",
              "What was TCS price last month?", "Chart INFY for the last 3 months."
              interval: DAY (default), WEEK, or MONTH.
              from/to: YYYY-MM-DD format (e.g. "2025-01-01"). Leave blank for recent data.
              """)
    @CircuitBreaker(name = "am-market", fallbackMethod = "histFallback")
    public String getHistoricalData(
            @ToolParam(description = "Stock symbol (e.g. 'RELIANCE').") String symbol,
            @ToolParam(required = false, description = "DAY, WEEK, or MONTH (default: DAY).") String interval,
            @ToolParam(required = false, description = "Start date YYYY-MM-DD (optional).") String fromDate,
            @ToolParam(required = false, description = "End date YYYY-MM-DD (optional).") String toDate) {
        try {
            TimeFrame tf = switch ((interval != null ? interval.toUpperCase() : "DAY")) {
                case "WEEK", "1W" -> TimeFrame.WEEK;
                case "MONTH", "1M" -> TimeFrame.MONTH;
                default -> TimeFrame.DAY;
            };
            var result = marketDataClientService.getHistoricalDataBatch(
                    symbol.toUpperCase(), fromDate, toDate, tf);
            return response.toJson(result);
        } catch (Exception e) {
            return response.errorJson("get_historical_data", e);
        }
    }

    public String histFallback(String s, String i, String f, String t, Exception e) {
        return response.unavailable("am-market (historical)");
    }

    @Tool(name = "get_market_movers",
          description = """
              [market] Get market-wide / index top gainers and losers (NOT portfolio holdings).
              Do NOT use for "my portfolio gainers" — use get_top_movers (analysis) instead.
              Use this when asked: "Nifty top gainers today", "Market losers", "Index movers."
              type: GAINERS or LOSERS (optional). indexSymbol e.g. NIFTY 50.
              """)
    @CircuitBreaker(name = "am-market", fallbackMethod = "moversFallback")
    public String getMarketMovers(
            @ToolParam(required = false, description = "GAINERS or LOSERS (optional).") String type,
            @ToolParam(required = false, description = "Max results (default 10).") Integer limit,
            @ToolParam(required = false, description = "Index symbol e.g. NIFTY 50 (optional).") String indexSymbol) {
        try {
            List<Map<String, Object>> movers = marketDataClientService.getMovers(
                    type, limit != null ? limit : 10, indexSymbol, null, null);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("movers", movers);
            result.put("count", movers.size());
            return response.toJson(result);
        } catch (Exception e) {
            return response.errorJson("get_market_movers", e);
        }
    }

    public String moversFallback(String t, Integer l, String i, Exception e) {
        return response.unavailable("am-market (movers)");
    }

    @Tool(name = "get_sector_performance",
          description = """
              [market] Get market-wide sector performance for an index (NOT portfolio sector allocation).
              Do NOT use for "my sector exposure" — use get_sector_allocation (analysis) instead.
              Use this when asked: "Which sectors are up today?", "Nifty sector performance."
              """)
    @CircuitBreaker(name = "am-market", fallbackMethod = "sectorPerfFallback")
    public String getSectorPerformance(
            @ToolParam(required = false, description = "Index symbol e.g. NIFTY 50 (optional).") String indexSymbol,
            @ToolParam(required = false, description = "Time frame e.g. DAY (optional).") String timeFrame) {
        try {
            List<Map<String, Object>> sectors = marketDataClientService.getSectorPerformance(
                    indexSymbol, timeFrame, null);
            return response.toJson(Map.of("sectors", sectors, "count", sectors.size()));
        } catch (Exception e) {
            return response.errorJson("get_sector_performance", e);
        }
    }

    public String sectorPerfFallback(String i, String t, Exception e) {
        return response.unavailable("am-market (sector performance)");
    }

    @Tool(name = "get_indices_data",
          description = """
              [market] Get latest index levels (NIFTY, SENSEX, BANKNIFTY, etc.).
              Use this when asked: "Where is Nifty?", "Sensex level", "Show index data."
              Pass comma-separated symbols or omit for defaults.
              """)
    @CircuitBreaker(name = "am-market", fallbackMethod = "indicesFallback")
    public String getIndicesData(
            @ToolParam(required = false, description = "Comma-separated index symbols (optional).") String symbols) {
        try {
            List<String> list;
            if (symbols != null && !symbols.isBlank()) {
                list = Arrays.stream(symbols.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
            } else {
                list = List.of("NIFTY 50", "NIFTY BANK", "SENSEX");
            }
            return response.toJson(marketDataClientService.getIndicesData(list, false));
        } catch (Exception e) {
            return response.errorJson("get_indices_data", e);
        }
    }

    public String indicesFallback(String s, Exception e) {
        return response.unavailable("am-market (indices)");
    }
}
