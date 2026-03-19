# AnalysisControllerApi

All URIs are relative to *http://localhost:8090*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getAllocation**](AnalysisControllerApi.md#getAllocation) | **GET** /api/v1/analysis/{type}/{id}/allocation |  |
| [**getAllocationWithHttpInfo**](AnalysisControllerApi.md#getAllocationWithHttpInfo) | **GET** /api/v1/analysis/{type}/{id}/allocation |  |
| [**getPerformance**](AnalysisControllerApi.md#getPerformance) | **GET** /api/v1/analysis/{type}/{id}/performance |  |
| [**getPerformanceWithHttpInfo**](AnalysisControllerApi.md#getPerformanceWithHttpInfo) | **GET** /api/v1/analysis/{type}/{id}/performance |  |
| [**getTopMoversByCategory**](AnalysisControllerApi.md#getTopMoversByCategory) | **GET** /api/v1/analysis/{type}/top-movers |  |
| [**getTopMoversByCategoryWithHttpInfo**](AnalysisControllerApi.md#getTopMoversByCategoryWithHttpInfo) | **GET** /api/v1/analysis/{type}/top-movers |  |
| [**getTopMoversByEntity**](AnalysisControllerApi.md#getTopMoversByEntity) | **GET** /api/v1/analysis/{type}/{id}/top-movers |  |
| [**getTopMoversByEntityWithHttpInfo**](AnalysisControllerApi.md#getTopMoversByEntityWithHttpInfo) | **GET** /api/v1/analysis/{type}/{id}/top-movers |  |



## getAllocation

> AllocationResponse getAllocation(authorization, type, id, groupBy, groupBy2)



### Example

```java
// Import classes:
import com.am.portfolio.client.analysis.invoker.ApiClient;
import com.am.portfolio.client.analysis.invoker.ApiException;
import com.am.portfolio.client.analysis.invoker.Configuration;
import com.am.portfolio.client.analysis.invoker.models.*;
import com.am.portfolio.client.analysis.api.AnalysisControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8090");

        AnalysisControllerApi apiInstance = new AnalysisControllerApi(defaultClient);
        String authorization = "authorization_example"; // String | 
        String type = "type_example"; // String | 
        String id = "id_example"; // String | 
        String groupBy = "STOCK"; // String | 
        String groupBy2 = "STOCK"; // String | 
        try {
            AllocationResponse result = apiInstance.getAllocation(authorization, type, id, groupBy, groupBy2);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling AnalysisControllerApi#getAllocation");
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
| **authorization** | **String**|  | |
| **type** | **String**|  | |
| **id** | **String**|  | |
| **groupBy** | **String**|  | [optional] [enum: STOCK, SECTOR, ASSET_CLASS, MARKET_CAP] |
| **groupBy2** | **String**|  | [optional] [enum: STOCK, SECTOR, ASSET_CLASS, MARKET_CAP] |

### Return type

[**AllocationResponse**](AllocationResponse.md)


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

## getAllocationWithHttpInfo

> ApiResponse<AllocationResponse> getAllocation getAllocationWithHttpInfo(authorization, type, id, groupBy, groupBy2)



### Example

```java
// Import classes:
import com.am.portfolio.client.analysis.invoker.ApiClient;
import com.am.portfolio.client.analysis.invoker.ApiException;
import com.am.portfolio.client.analysis.invoker.ApiResponse;
import com.am.portfolio.client.analysis.invoker.Configuration;
import com.am.portfolio.client.analysis.invoker.models.*;
import com.am.portfolio.client.analysis.api.AnalysisControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8090");

        AnalysisControllerApi apiInstance = new AnalysisControllerApi(defaultClient);
        String authorization = "authorization_example"; // String | 
        String type = "type_example"; // String | 
        String id = "id_example"; // String | 
        String groupBy = "STOCK"; // String | 
        String groupBy2 = "STOCK"; // String | 
        try {
            ApiResponse<AllocationResponse> response = apiInstance.getAllocationWithHttpInfo(authorization, type, id, groupBy, groupBy2);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling AnalysisControllerApi#getAllocation");
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
| **authorization** | **String**|  | |
| **type** | **String**|  | |
| **id** | **String**|  | |
| **groupBy** | **String**|  | [optional] [enum: STOCK, SECTOR, ASSET_CLASS, MARKET_CAP] |
| **groupBy2** | **String**|  | [optional] [enum: STOCK, SECTOR, ASSET_CLASS, MARKET_CAP] |

### Return type

ApiResponse<[**AllocationResponse**](AllocationResponse.md)>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## getPerformance

> PerformanceResponse getPerformance(authorization, type, id, timeFrame)



### Example

```java
// Import classes:
import com.am.portfolio.client.analysis.invoker.ApiClient;
import com.am.portfolio.client.analysis.invoker.ApiException;
import com.am.portfolio.client.analysis.invoker.Configuration;
import com.am.portfolio.client.analysis.invoker.models.*;
import com.am.portfolio.client.analysis.api.AnalysisControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8090");

        AnalysisControllerApi apiInstance = new AnalysisControllerApi(defaultClient);
        String authorization = "authorization_example"; // String | 
        String type = "type_example"; // String | 
        String id = "id_example"; // String | 
        String timeFrame = "1M"; // String | 
        try {
            PerformanceResponse result = apiInstance.getPerformance(authorization, type, id, timeFrame);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling AnalysisControllerApi#getPerformance");
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
| **authorization** | **String**|  | |
| **type** | **String**|  | |
| **id** | **String**|  | |
| **timeFrame** | **String**|  | [optional] [default to 1M] |

### Return type

[**PerformanceResponse**](PerformanceResponse.md)


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

## getPerformanceWithHttpInfo

> ApiResponse<PerformanceResponse> getPerformance getPerformanceWithHttpInfo(authorization, type, id, timeFrame)



### Example

```java
// Import classes:
import com.am.portfolio.client.analysis.invoker.ApiClient;
import com.am.portfolio.client.analysis.invoker.ApiException;
import com.am.portfolio.client.analysis.invoker.ApiResponse;
import com.am.portfolio.client.analysis.invoker.Configuration;
import com.am.portfolio.client.analysis.invoker.models.*;
import com.am.portfolio.client.analysis.api.AnalysisControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8090");

        AnalysisControllerApi apiInstance = new AnalysisControllerApi(defaultClient);
        String authorization = "authorization_example"; // String | 
        String type = "type_example"; // String | 
        String id = "id_example"; // String | 
        String timeFrame = "1M"; // String | 
        try {
            ApiResponse<PerformanceResponse> response = apiInstance.getPerformanceWithHttpInfo(authorization, type, id, timeFrame);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling AnalysisControllerApi#getPerformance");
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
| **authorization** | **String**|  | |
| **type** | **String**|  | |
| **id** | **String**|  | |
| **timeFrame** | **String**|  | [optional] [default to 1M] |

### Return type

ApiResponse<[**PerformanceResponse**](PerformanceResponse.md)>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## getTopMoversByCategory

> TopMoversResponse getTopMoversByCategory(authorization, type, timeFrame, groupBy, groupBy2)



### Example

```java
// Import classes:
import com.am.portfolio.client.analysis.invoker.ApiClient;
import com.am.portfolio.client.analysis.invoker.ApiException;
import com.am.portfolio.client.analysis.invoker.Configuration;
import com.am.portfolio.client.analysis.invoker.models.*;
import com.am.portfolio.client.analysis.api.AnalysisControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8090");

        AnalysisControllerApi apiInstance = new AnalysisControllerApi(defaultClient);
        String authorization = "authorization_example"; // String | 
        String type = "type_example"; // String | 
        String timeFrame = "timeFrame_example"; // String | 
        String groupBy = "STOCK"; // String | 
        String groupBy2 = "STOCK"; // String | 
        try {
            TopMoversResponse result = apiInstance.getTopMoversByCategory(authorization, type, timeFrame, groupBy, groupBy2);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling AnalysisControllerApi#getTopMoversByCategory");
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
| **authorization** | **String**|  | |
| **type** | **String**|  | |
| **timeFrame** | **String**|  | [optional] |
| **groupBy** | **String**|  | [optional] [enum: STOCK, SECTOR, ASSET_CLASS, MARKET_CAP] |
| **groupBy2** | **String**|  | [optional] [enum: STOCK, SECTOR, ASSET_CLASS, MARKET_CAP] |

### Return type

[**TopMoversResponse**](TopMoversResponse.md)


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

## getTopMoversByCategoryWithHttpInfo

> ApiResponse<TopMoversResponse> getTopMoversByCategory getTopMoversByCategoryWithHttpInfo(authorization, type, timeFrame, groupBy, groupBy2)



### Example

```java
// Import classes:
import com.am.portfolio.client.analysis.invoker.ApiClient;
import com.am.portfolio.client.analysis.invoker.ApiException;
import com.am.portfolio.client.analysis.invoker.ApiResponse;
import com.am.portfolio.client.analysis.invoker.Configuration;
import com.am.portfolio.client.analysis.invoker.models.*;
import com.am.portfolio.client.analysis.api.AnalysisControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8090");

        AnalysisControllerApi apiInstance = new AnalysisControllerApi(defaultClient);
        String authorization = "authorization_example"; // String | 
        String type = "type_example"; // String | 
        String timeFrame = "timeFrame_example"; // String | 
        String groupBy = "STOCK"; // String | 
        String groupBy2 = "STOCK"; // String | 
        try {
            ApiResponse<TopMoversResponse> response = apiInstance.getTopMoversByCategoryWithHttpInfo(authorization, type, timeFrame, groupBy, groupBy2);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling AnalysisControllerApi#getTopMoversByCategory");
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
| **authorization** | **String**|  | |
| **type** | **String**|  | |
| **timeFrame** | **String**|  | [optional] |
| **groupBy** | **String**|  | [optional] [enum: STOCK, SECTOR, ASSET_CLASS, MARKET_CAP] |
| **groupBy2** | **String**|  | [optional] [enum: STOCK, SECTOR, ASSET_CLASS, MARKET_CAP] |

### Return type

ApiResponse<[**TopMoversResponse**](TopMoversResponse.md)>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## getTopMoversByEntity

> TopMoversResponse getTopMoversByEntity(authorization, type, id, timeFrame, groupBy, groupBy2)



### Example

```java
// Import classes:
import com.am.portfolio.client.analysis.invoker.ApiClient;
import com.am.portfolio.client.analysis.invoker.ApiException;
import com.am.portfolio.client.analysis.invoker.Configuration;
import com.am.portfolio.client.analysis.invoker.models.*;
import com.am.portfolio.client.analysis.api.AnalysisControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8090");

        AnalysisControllerApi apiInstance = new AnalysisControllerApi(defaultClient);
        String authorization = "authorization_example"; // String | 
        String type = "type_example"; // String | 
        String id = "id_example"; // String | 
        String timeFrame = "timeFrame_example"; // String | 
        String groupBy = "STOCK"; // String | 
        String groupBy2 = "STOCK"; // String | 
        try {
            TopMoversResponse result = apiInstance.getTopMoversByEntity(authorization, type, id, timeFrame, groupBy, groupBy2);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling AnalysisControllerApi#getTopMoversByEntity");
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
| **authorization** | **String**|  | |
| **type** | **String**|  | |
| **id** | **String**|  | |
| **timeFrame** | **String**|  | [optional] |
| **groupBy** | **String**|  | [optional] [enum: STOCK, SECTOR, ASSET_CLASS, MARKET_CAP] |
| **groupBy2** | **String**|  | [optional] [enum: STOCK, SECTOR, ASSET_CLASS, MARKET_CAP] |

### Return type

[**TopMoversResponse**](TopMoversResponse.md)


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

## getTopMoversByEntityWithHttpInfo

> ApiResponse<TopMoversResponse> getTopMoversByEntity getTopMoversByEntityWithHttpInfo(authorization, type, id, timeFrame, groupBy, groupBy2)



### Example

```java
// Import classes:
import com.am.portfolio.client.analysis.invoker.ApiClient;
import com.am.portfolio.client.analysis.invoker.ApiException;
import com.am.portfolio.client.analysis.invoker.ApiResponse;
import com.am.portfolio.client.analysis.invoker.Configuration;
import com.am.portfolio.client.analysis.invoker.models.*;
import com.am.portfolio.client.analysis.api.AnalysisControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8090");

        AnalysisControllerApi apiInstance = new AnalysisControllerApi(defaultClient);
        String authorization = "authorization_example"; // String | 
        String type = "type_example"; // String | 
        String id = "id_example"; // String | 
        String timeFrame = "timeFrame_example"; // String | 
        String groupBy = "STOCK"; // String | 
        String groupBy2 = "STOCK"; // String | 
        try {
            ApiResponse<TopMoversResponse> response = apiInstance.getTopMoversByEntityWithHttpInfo(authorization, type, id, timeFrame, groupBy, groupBy2);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling AnalysisControllerApi#getTopMoversByEntity");
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
| **authorization** | **String**|  | |
| **type** | **String**|  | |
| **id** | **String**|  | |
| **timeFrame** | **String**|  | [optional] |
| **groupBy** | **String**|  | [optional] [enum: STOCK, SECTOR, ASSET_CLASS, MARKET_CAP] |
| **groupBy2** | **String**|  | [optional] [enum: STOCK, SECTOR, ASSET_CLASS, MARKET_CAP] |

### Return type

ApiResponse<[**TopMoversResponse**](TopMoversResponse.md)>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

