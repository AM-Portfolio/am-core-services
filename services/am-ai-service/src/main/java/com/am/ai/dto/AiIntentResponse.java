package com.am.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiIntentResponse {
    
    // The textual response from the assistant (e.g., "Your top movers today are AAPL and MSFT.")
    private String message;
    
    // The intent type representing the widget to render (e.g., "TOP_MOVERS", "ALLOCATION_PIE_CHART")
    private String widgetId;
    
    // Optional additional parameters context needed by the UI to fetch data for the widget
    // Example: { "sector": "TECH", "timeFrame": "1M" }
    private Map<String, Object> widgetParams;
}
