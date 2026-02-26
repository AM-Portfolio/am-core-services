# IndexAnalyticsApi

All URIs are relative to *http://localhost:8101*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getAdvancedAnalytics1**](IndexAnalyticsApi.md#getAdvancedAnalytics1) | **POST** /api/v1/analytics/index/{indexSymbol}/advanced | Get advanced index analytics |
| [**getAdvancedAnalytics1WithHttpInfo**](IndexAnalyticsApi.md#getAdvancedAnalytics1WithHttpInfo) | **POST** /api/v1/analytics/index/{indexSymbol}/advanced | Get advanced index analytics |



## getAdvancedAnalytics1

> AdvancedAnalyticsResponse getAdvancedAnalytics1(indexSymbol, advancedAnalyticsRequest)

Get advanced index analytics

Retrieves comprehensive analytics for a market index with customizable components and timeframes

### Example

```java
// Import classes:
import com.am.portfolio.client.invoker.ApiClient;
import com.am.portfolio.client.invoker.ApiException;
import com.am.portfolio.client.invoker.Configuration;
import com.am.portfolio.client.invoker.models.*;
import com.am.portfolio.client.api.IndexAnalyticsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8101");

        IndexAnalyticsApi apiInstance = new IndexAnalyticsApi(defaultClient);
        String indexSymbol = "indexSymbol_example"; // String | 
        AdvancedAnalyticsRequest advancedAnalyticsRequest = new AdvancedAnalyticsRequest(); // AdvancedAnalyticsRequest | 
        try {
            AdvancedAnalyticsResponse result = apiInstance.getAdvancedAnalytics1(indexSymbol, advancedAnalyticsRequest);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling IndexAnalyticsApi#getAdvancedAnalytics1");
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
| **indexSymbol** | **String**|  | |
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
| **404** | Index not found |  -  |
| **400** | Invalid request parameters |  -  |
| **200** | Analytics data retrieved successfully |  -  |

## getAdvancedAnalytics1WithHttpInfo

> ApiResponse<AdvancedAnalyticsResponse> getAdvancedAnalytics1 getAdvancedAnalytics1WithHttpInfo(indexSymbol, advancedAnalyticsRequest)

Get advanced index analytics

Retrieves comprehensive analytics for a market index with customizable components and timeframes

### Example

```java
// Import classes:
import com.am.portfolio.client.invoker.ApiClient;
import com.am.portfolio.client.invoker.ApiException;
import com.am.portfolio.client.invoker.ApiResponse;
import com.am.portfolio.client.invoker.Configuration;
import com.am.portfolio.client.invoker.models.*;
import com.am.portfolio.client.api.IndexAnalyticsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8101");

        IndexAnalyticsApi apiInstance = new IndexAnalyticsApi(defaultClient);
        String indexSymbol = "indexSymbol_example"; // String | 
        AdvancedAnalyticsRequest advancedAnalyticsRequest = new AdvancedAnalyticsRequest(); // AdvancedAnalyticsRequest | 
        try {
            ApiResponse<AdvancedAnalyticsResponse> response = apiInstance.getAdvancedAnalytics1WithHttpInfo(indexSymbol, advancedAnalyticsRequest);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling IndexAnalyticsApi#getAdvancedAnalytics1");
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
| **indexSymbol** | **String**|  | |
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
| **404** | Index not found |  -  |
| **400** | Invalid request parameters |  -  |
| **200** | Analytics data retrieved successfully |  -  |

