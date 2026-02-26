# PortfolioAnalyticsApi

All URIs are relative to *http://localhost:8101*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getAdvancedAnalytics**](PortfolioAnalyticsApi.md#getAdvancedAnalytics) | **POST** /api/v1/analytics/portfolio/{portfolioId}/advanced | Get advanced portfolio analytics |
| [**getAdvancedAnalyticsWithHttpInfo**](PortfolioAnalyticsApi.md#getAdvancedAnalyticsWithHttpInfo) | **POST** /api/v1/analytics/portfolio/{portfolioId}/advanced | Get advanced portfolio analytics |



## getAdvancedAnalytics

> AdvancedAnalyticsResponse getAdvancedAnalytics(portfolioId, advancedAnalyticsRequest)

Get advanced portfolio analytics

Retrieves comprehensive analytics for a portfolio with customizable components and timeframes

### Example

```java
// Import classes:
import com.am.portfolio.client.invoker.ApiClient;
import com.am.portfolio.client.invoker.ApiException;
import com.am.portfolio.client.invoker.Configuration;
import com.am.portfolio.client.invoker.models.*;
import com.am.portfolio.client.api.PortfolioAnalyticsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8101");

        PortfolioAnalyticsApi apiInstance = new PortfolioAnalyticsApi(defaultClient);
        String portfolioId = "portfolioId_example"; // String | 
        AdvancedAnalyticsRequest advancedAnalyticsRequest = new AdvancedAnalyticsRequest(); // AdvancedAnalyticsRequest | 
        try {
            AdvancedAnalyticsResponse result = apiInstance.getAdvancedAnalytics(portfolioId, advancedAnalyticsRequest);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling PortfolioAnalyticsApi#getAdvancedAnalytics");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **portfolioId** | **String**|  | |
| **advancedAnalyticsRequest** | [**AdvancedAnalyticsRequest**](AdvancedAnalyticsRequest.md)|  | |

### Return type

[**AdvancedAnalyticsResponse**](AdvancedAnalyticsResponse.md)


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: */*, application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **404** | Portfolio not found |  -  |
| **400** | Invalid request parameters |  -  |
| **200** | Analytics data retrieved successfully |  -  |

## getAdvancedAnalyticsWithHttpInfo

> ApiResponse<AdvancedAnalyticsResponse> getAdvancedAnalytics getAdvancedAnalyticsWithHttpInfo(portfolioId, advancedAnalyticsRequest)

Get advanced portfolio analytics

Retrieves comprehensive analytics for a portfolio with customizable components and timeframes

### Example

```java
// Import classes:
import com.am.portfolio.client.invoker.ApiClient;
import com.am.portfolio.client.invoker.ApiException;
import com.am.portfolio.client.invoker.ApiResponse;
import com.am.portfolio.client.invoker.Configuration;
import com.am.portfolio.client.invoker.models.*;
import com.am.portfolio.client.api.PortfolioAnalyticsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8101");

        PortfolioAnalyticsApi apiInstance = new PortfolioAnalyticsApi(defaultClient);
        String portfolioId = "portfolioId_example"; // String | 
        AdvancedAnalyticsRequest advancedAnalyticsRequest = new AdvancedAnalyticsRequest(); // AdvancedAnalyticsRequest | 
        try {
            ApiResponse<AdvancedAnalyticsResponse> response = apiInstance.getAdvancedAnalyticsWithHttpInfo(portfolioId, advancedAnalyticsRequest);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling PortfolioAnalyticsApi#getAdvancedAnalytics");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Response headers: " + e.getResponseHeaders());
            System.err.println("Reason: " + e.getResponseBody());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **portfolioId** | **String**|  | |
| **advancedAnalyticsRequest** | [**AdvancedAnalyticsRequest**](AdvancedAnalyticsRequest.md)|  | |

### Return type

ApiResponse<[**AdvancedAnalyticsResponse**](AdvancedAnalyticsResponse.md)>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: */*, application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **404** | Portfolio not found |  -  |
| **400** | Invalid request parameters |  -  |
| **200** | Analytics data retrieved successfully |  -  |

