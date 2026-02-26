# MarketIndicesApi

All URIs are relative to *http://localhost:8101*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getAllMarketIndices**](MarketIndicesApi.md#getAllMarketIndices) | **GET** /api/v1/market-index/all | Get all market indices |
| [**getAllMarketIndicesWithHttpInfo**](MarketIndicesApi.md#getAllMarketIndicesWithHttpInfo) | **GET** /api/v1/market-index/all | Get all market indices |



## getAllMarketIndices

> IndexIndices getAllMarketIndices(interval, type)

Get all market indices

Retrieves all market indices with optional filtering by interval and type

### Example

```java
// Import classes:
import com.am.portfolio.client.invoker.ApiClient;
import com.am.portfolio.client.invoker.ApiException;
import com.am.portfolio.client.invoker.Configuration;
import com.am.portfolio.client.invoker.models.*;
import com.am.portfolio.client.api.MarketIndicesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8101");

        MarketIndicesApi apiInstance = new MarketIndicesApi(defaultClient);
        String interval = "interval_example"; // String | 
        String type = "type_example"; // String | 
        try {
            IndexIndices result = apiInstance.getAllMarketIndices(interval, type);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketIndicesApi#getAllMarketIndices");
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
| **interval** | **String**|  | [optional] |
| **type** | **String**|  | [optional] |

### Return type

[**IndexIndices**](IndexIndices.md)


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json, */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Market indices retrieved successfully |  -  |
| **404** | No market indices found |  -  |
| **400** | Invalid interval parameter |  -  |

## getAllMarketIndicesWithHttpInfo

> ApiResponse<IndexIndices> getAllMarketIndices getAllMarketIndicesWithHttpInfo(interval, type)

Get all market indices

Retrieves all market indices with optional filtering by interval and type

### Example

```java
// Import classes:
import com.am.portfolio.client.invoker.ApiClient;
import com.am.portfolio.client.invoker.ApiException;
import com.am.portfolio.client.invoker.ApiResponse;
import com.am.portfolio.client.invoker.Configuration;
import com.am.portfolio.client.invoker.models.*;
import com.am.portfolio.client.api.MarketIndicesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8101");

        MarketIndicesApi apiInstance = new MarketIndicesApi(defaultClient);
        String interval = "interval_example"; // String | 
        String type = "type_example"; // String | 
        try {
            ApiResponse<IndexIndices> response = apiInstance.getAllMarketIndicesWithHttpInfo(interval, type);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketIndicesApi#getAllMarketIndices");
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
| **interval** | **String**|  | [optional] |
| **type** | **String**|  | [optional] |

### Return type

ApiResponse<[**IndexIndices**](IndexIndices.md)>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json, */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Market indices retrieved successfully |  -  |
| **404** | No market indices found |  -  |
| **400** | Invalid interval parameter |  -  |

