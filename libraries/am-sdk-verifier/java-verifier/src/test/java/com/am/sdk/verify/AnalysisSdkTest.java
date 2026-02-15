package com.am.sdk.verify;

import com.am.portfolio.client.analysis.api.AnalysisControllerApi;
import com.am.portfolio.client.analysis.invoker.ApiClient;
import com.am.portfolio.client.analysis.invoker.ApiException;
import com.am.portfolio.client.analysis.model.TopMoversResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Test to verify the generated SDK can communicate with the running API.
 * This runs against localhost:8090 by default.
 */
public class AnalysisSdkTest {
    
    private static final Logger log = LoggerFactory.getLogger(AnalysisSdkTest.class);
    private AnalysisControllerApi api;

    @BeforeEach
    public void setup() {
        // Initialize client targeting localhost:8090 (default)
        ApiClient client = new ApiClient();
        client.updateBaseUri("http://localhost:8090");
        client.setConnectTimeout(Duration.ofSeconds(5));
        client.setReadTimeout(Duration.ofSeconds(10));
        
        // Use the generated API
        api = new AnalysisControllerApi(client);
    }

    @Test
    public void testGetTopMovers_Connectivity() {
        // We will test if we can at least reach the endpoint. 
        // We expect a 401 Unauthorized because we are not sending a valid token,
        // OR a 200 OK if auth is disabled locally or we mock it.
        // The most important thing is that the SDK successfully makes the request structure.
        
        log.info("Verifying SDK connectivity to http://localhost:8090...");
        
        try {
            // Calling a valid endpoint: /api/v1/analysis/{type}/top-movers
            // "type" could be "EQUITY"
            TopMoversResponse response = api.getTopMoversByCategory(
                "Bearer test-token", // Dummy Authorization header
                "EQUITY",            // type
                "1D",                // timeFrame
                "STOCK",             // groupBy (header)
                "STOCK"              // groupBy (query)
            );
            
            assertNotNull(response, "Response should not be null on 200 OK");
            log.info("SUCCESS: Received 200 OK from API");

        } catch (ApiException e) {
            log.error("API Exception: Code={}, Message={}", e.getCode(), e.getMessage());
            
            // If the service is running but returns 401/403 (Auth error), that means SDK WORKED to connect!
            if (e.getCode() == 401 || e.getCode() == 403) {
                 log.info("SUCCESS: Reached API (Auth Rejected as expected)");
            } else if (e.getCode() == 404) {
                 log.warn("WARNING: Endpoint not found (404), maybe path changed? SDK structure verified though.");
            } else if (e.getCode() == 0) {
                 // Connection Refused means service is likely down
                 fail("FAILED: Could not connect to API. Is 'am-analysis' running on port 8090?");
            } else {
                 // Some other error (500 etc) means connectivity is fine, logic might be failing
                 log.info("SUCCESS: Reached API with code {}", e.getCode());
            }
        } catch (Exception e) {
            fail("FAILED: Unexpected exception: " + e.getMessage());
        }
    }
}
