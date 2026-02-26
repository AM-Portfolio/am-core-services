# PortfolioManagementApi

All URIs are relative to *http://localhost:8101*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getPortfolioBasicDetails**](PortfolioManagementApi.md#getPortfolioBasicDetails) | **GET** /api/v1/portfolios/list | Get portfolio IDs and names |
| [**getPortfolioBasicDetailsWithHttpInfo**](PortfolioManagementApi.md#getPortfolioBasicDetailsWithHttpInfo) | **GET** /api/v1/portfolios/list | Get portfolio IDs and names |
| [**getPortfolioById**](PortfolioManagementApi.md#getPortfolioById) | **GET** /api/v1/portfolios/{portfolioId} | Get portfolio by ID |
| [**getPortfolioByIdWithHttpInfo**](PortfolioManagementApi.md#getPortfolioByIdWithHttpInfo) | **GET** /api/v1/portfolios/{portfolioId} | Get portfolio by ID |
| [**getPortfolioHoldings**](PortfolioManagementApi.md#getPortfolioHoldings) | **GET** /api/v1/portfolios/holdings | Get portfolio holdings |
| [**getPortfolioHoldingsWithHttpInfo**](PortfolioManagementApi.md#getPortfolioHoldingsWithHttpInfo) | **GET** /api/v1/portfolios/holdings | Get portfolio holdings |
| [**getPortfolioSummary**](PortfolioManagementApi.md#getPortfolioSummary) | **GET** /api/v1/portfolios/summary | Get portfolio summary |
| [**getPortfolioSummaryWithHttpInfo**](PortfolioManagementApi.md#getPortfolioSummaryWithHttpInfo) | **GET** /api/v1/portfolios/summary | Get portfolio summary |
| [**getPortfolios**](PortfolioManagementApi.md#getPortfolios) | **GET** /api/v1/portfolios | Get all portfolios for user |
| [**getPortfoliosWithHttpInfo**](PortfolioManagementApi.md#getPortfoliosWithHttpInfo) | **GET** /api/v1/portfolios | Get all portfolios for user |



## getPortfolioBasicDetails

> List<PortfolioBasicInfo> getPortfolioBasicDetails(userId)

Get portfolio IDs and names

Retrieves a lightweight list of portfolio IDs and names for all user portfolios

### Example

```java
// Import classes:
import com.am.portfolio.client.invoker.ApiClient;
import com.am.portfolio.client.invoker.ApiException;
import com.am.portfolio.client.invoker.Configuration;
import com.am.portfolio.client.invoker.models.*;
import com.am.portfolio.client.api.PortfolioManagementApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8101");

        PortfolioManagementApi apiInstance = new PortfolioManagementApi(defaultClient);
        String userId = "userId_example"; // String | User ID to fetch portfolio basic details for
        try {
            List<PortfolioBasicInfo> result = apiInstance.getPortfolioBasicDetails(userId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling PortfolioManagementApi#getPortfolioBasicDetails");
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
| **userId** | **String**| User ID to fetch portfolio basic details for | |

### Return type

[**List&lt;PortfolioBasicInfo&gt;**](PortfolioBasicInfo.md)


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **404** | No portfolios found for user |  -  |
| **200** | Portfolio list retrieved successfully |  -  |

## getPortfolioBasicDetailsWithHttpInfo

> ApiResponse<List<PortfolioBasicInfo>> getPortfolioBasicDetails getPortfolioBasicDetailsWithHttpInfo(userId)

Get portfolio IDs and names

Retrieves a lightweight list of portfolio IDs and names for all user portfolios

### Example

```java
// Import classes:
import com.am.portfolio.client.invoker.ApiClient;
import com.am.portfolio.client.invoker.ApiException;
import com.am.portfolio.client.invoker.ApiResponse;
import com.am.portfolio.client.invoker.Configuration;
import com.am.portfolio.client.invoker.models.*;
import com.am.portfolio.client.api.PortfolioManagementApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8101");

        PortfolioManagementApi apiInstance = new PortfolioManagementApi(defaultClient);
        String userId = "userId_example"; // String | User ID to fetch portfolio basic details for
        try {
            ApiResponse<List<PortfolioBasicInfo>> response = apiInstance.getPortfolioBasicDetailsWithHttpInfo(userId);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling PortfolioManagementApi#getPortfolioBasicDetails");
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
| **userId** | **String**| User ID to fetch portfolio basic details for | |

### Return type

ApiResponse<[**List&lt;PortfolioBasicInfo&gt;**](PortfolioBasicInfo.md)>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **404** | No portfolios found for user |  -  |
| **200** | Portfolio list retrieved successfully |  -  |


## getPortfolioById

> PortfolioModelV1 getPortfolioById(portfolioId)

Get portfolio by ID

Retrieves detailed portfolio information for a specific portfolio ID

### Example

```java
// Import classes:
import com.am.portfolio.client.invoker.ApiClient;
import com.am.portfolio.client.invoker.ApiException;
import com.am.portfolio.client.invoker.Configuration;
import com.am.portfolio.client.invoker.models.*;
import com.am.portfolio.client.api.PortfolioManagementApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8101");

        PortfolioManagementApi apiInstance = new PortfolioManagementApi(defaultClient);
        String portfolioId = "portfolioId_example"; // String | Portfolio ID (UUID format)
        try {
            PortfolioModelV1 result = apiInstance.getPortfolioById(portfolioId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling PortfolioManagementApi#getPortfolioById");
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
| **portfolioId** | **String**| Portfolio ID (UUID format) | |

### Return type

[**PortfolioModelV1**](PortfolioModelV1.md)


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*, application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **404** | Portfolio not found |  -  |
| **200** | Portfolio found |  -  |
| **400** | Invalid portfolio ID format |  -  |

## getPortfolioByIdWithHttpInfo

> ApiResponse<PortfolioModelV1> getPortfolioById getPortfolioByIdWithHttpInfo(portfolioId)

Get portfolio by ID

Retrieves detailed portfolio information for a specific portfolio ID

### Example

```java
// Import classes:
import com.am.portfolio.client.invoker.ApiClient;
import com.am.portfolio.client.invoker.ApiException;
import com.am.portfolio.client.invoker.ApiResponse;
import com.am.portfolio.client.invoker.Configuration;
import com.am.portfolio.client.invoker.models.*;
import com.am.portfolio.client.api.PortfolioManagementApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8101");

        PortfolioManagementApi apiInstance = new PortfolioManagementApi(defaultClient);
        String portfolioId = "portfolioId_example"; // String | Portfolio ID (UUID format)
        try {
            ApiResponse<PortfolioModelV1> response = apiInstance.getPortfolioByIdWithHttpInfo(portfolioId);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling PortfolioManagementApi#getPortfolioById");
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
| **portfolioId** | **String**| Portfolio ID (UUID format) | |

### Return type

ApiResponse<[**PortfolioModelV1**](PortfolioModelV1.md)>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*, application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **404** | Portfolio not found |  -  |
| **200** | Portfolio found |  -  |
| **400** | Invalid portfolio ID format |  -  |


## getPortfolioHoldings

> PortfolioHoldings getPortfolioHoldings(userId, portfolioId, page, size, interval)

Get portfolio holdings

Retrieves all holdings across portfolios for a user with current values. Optionally filter by specific portfolio ID.

### Example

```java
// Import classes:
import com.am.portfolio.client.invoker.ApiClient;
import com.am.portfolio.client.invoker.ApiException;
import com.am.portfolio.client.invoker.Configuration;
import com.am.portfolio.client.invoker.models.*;
import com.am.portfolio.client.api.PortfolioManagementApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8101");

        PortfolioManagementApi apiInstance = new PortfolioManagementApi(defaultClient);
        String userId = "userId_example"; // String | User ID to fetch portfolio holdings for
        String portfolioId = "portfolioId_example"; // String | Optional portfolio ID to filter results for specific portfolio
        Integer page = 56; // Integer | 
        Integer size = 56; // Integer | 
        String interval = "interval_example"; // String | 
        try {
            PortfolioHoldings result = apiInstance.getPortfolioHoldings(userId, portfolioId, page, size, interval);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling PortfolioManagementApi#getPortfolioHoldings");
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
| **userId** | **String**| User ID to fetch portfolio holdings for | |
| **portfolioId** | **String**| Optional portfolio ID to filter results for specific portfolio | [optional] |
| **page** | **Integer**|  | [optional] |
| **size** | **Integer**|  | [optional] |
| **interval** | **String**|  | [optional] |

### Return type

[**PortfolioHoldings**](PortfolioHoldings.md)


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **404** | No holdings found for user |  -  |
| **200** | Portfolio holdings retrieved successfully |  -  |

## getPortfolioHoldingsWithHttpInfo

> ApiResponse<PortfolioHoldings> getPortfolioHoldings getPortfolioHoldingsWithHttpInfo(userId, portfolioId, page, size, interval)

Get portfolio holdings

Retrieves all holdings across portfolios for a user with current values. Optionally filter by specific portfolio ID.

### Example

```java
// Import classes:
import com.am.portfolio.client.invoker.ApiClient;
import com.am.portfolio.client.invoker.ApiException;
import com.am.portfolio.client.invoker.ApiResponse;
import com.am.portfolio.client.invoker.Configuration;
import com.am.portfolio.client.invoker.models.*;
import com.am.portfolio.client.api.PortfolioManagementApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8101");

        PortfolioManagementApi apiInstance = new PortfolioManagementApi(defaultClient);
        String userId = "userId_example"; // String | User ID to fetch portfolio holdings for
        String portfolioId = "portfolioId_example"; // String | Optional portfolio ID to filter results for specific portfolio
        Integer page = 56; // Integer | 
        Integer size = 56; // Integer | 
        String interval = "interval_example"; // String | 
        try {
            ApiResponse<PortfolioHoldings> response = apiInstance.getPortfolioHoldingsWithHttpInfo(userId, portfolioId, page, size, interval);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling PortfolioManagementApi#getPortfolioHoldings");
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
| **userId** | **String**| User ID to fetch portfolio holdings for | |
| **portfolioId** | **String**| Optional portfolio ID to filter results for specific portfolio | [optional] |
| **page** | **Integer**|  | [optional] |
| **size** | **Integer**|  | [optional] |
| **interval** | **String**|  | [optional] |

### Return type

ApiResponse<[**PortfolioHoldings**](PortfolioHoldings.md)>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **404** | No holdings found for user |  -  |
| **200** | Portfolio holdings retrieved successfully |  -  |


## getPortfolioSummary

> PortfolioSummaryV1 getPortfolioSummary(userId, portfolioId, page, size, interval)

Get portfolio summary

Retrieves a summary of all portfolios for a user with performance metrics. Optionally filter by specific portfolio ID.

### Example

```java
// Import classes:
import com.am.portfolio.client.invoker.ApiClient;
import com.am.portfolio.client.invoker.ApiException;
import com.am.portfolio.client.invoker.Configuration;
import com.am.portfolio.client.invoker.models.*;
import com.am.portfolio.client.api.PortfolioManagementApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8101");

        PortfolioManagementApi apiInstance = new PortfolioManagementApi(defaultClient);
        String userId = "userId_example"; // String | User ID to fetch portfolio summary for
        String portfolioId = "portfolioId_example"; // String | Optional portfolio ID to filter results for specific portfolio
        Integer page = 56; // Integer | 
        Integer size = 56; // Integer | 
        String interval = "interval_example"; // String | 
        try {
            PortfolioSummaryV1 result = apiInstance.getPortfolioSummary(userId, portfolioId, page, size, interval);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling PortfolioManagementApi#getPortfolioSummary");
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
| **userId** | **String**| User ID to fetch portfolio summary for | |
| **portfolioId** | **String**| Optional portfolio ID to filter results for specific portfolio | [optional] |
| **page** | **Integer**|  | [optional] |
| **size** | **Integer**|  | [optional] |
| **interval** | **String**|  | [optional] |

### Return type

[**PortfolioSummaryV1**](PortfolioSummaryV1.md)


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **404** | No portfolio summary found for user |  -  |
| **200** | Portfolio summary retrieved successfully |  -  |

## getPortfolioSummaryWithHttpInfo

> ApiResponse<PortfolioSummaryV1> getPortfolioSummary getPortfolioSummaryWithHttpInfo(userId, portfolioId, page, size, interval)

Get portfolio summary

Retrieves a summary of all portfolios for a user with performance metrics. Optionally filter by specific portfolio ID.

### Example

```java
// Import classes:
import com.am.portfolio.client.invoker.ApiClient;
import com.am.portfolio.client.invoker.ApiException;
import com.am.portfolio.client.invoker.ApiResponse;
import com.am.portfolio.client.invoker.Configuration;
import com.am.portfolio.client.invoker.models.*;
import com.am.portfolio.client.api.PortfolioManagementApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8101");

        PortfolioManagementApi apiInstance = new PortfolioManagementApi(defaultClient);
        String userId = "userId_example"; // String | User ID to fetch portfolio summary for
        String portfolioId = "portfolioId_example"; // String | Optional portfolio ID to filter results for specific portfolio
        Integer page = 56; // Integer | 
        Integer size = 56; // Integer | 
        String interval = "interval_example"; // String | 
        try {
            ApiResponse<PortfolioSummaryV1> response = apiInstance.getPortfolioSummaryWithHttpInfo(userId, portfolioId, page, size, interval);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling PortfolioManagementApi#getPortfolioSummary");
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
| **userId** | **String**| User ID to fetch portfolio summary for | |
| **portfolioId** | **String**| Optional portfolio ID to filter results for specific portfolio | [optional] |
| **page** | **Integer**|  | [optional] |
| **size** | **Integer**|  | [optional] |
| **interval** | **String**|  | [optional] |

### Return type

ApiResponse<[**PortfolioSummaryV1**](PortfolioSummaryV1.md)>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **404** | No portfolio summary found for user |  -  |
| **200** | Portfolio summary retrieved successfully |  -  |


## getPortfolios

> List<PortfolioModelV1> getPortfolios(userId)

Get all portfolios for user

Retrieves all portfolios associated with a specific user ID

### Example

```java
// Import classes:
import com.am.portfolio.client.invoker.ApiClient;
import com.am.portfolio.client.invoker.ApiException;
import com.am.portfolio.client.invoker.Configuration;
import com.am.portfolio.client.invoker.models.*;
import com.am.portfolio.client.api.PortfolioManagementApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8101");

        PortfolioManagementApi apiInstance = new PortfolioManagementApi(defaultClient);
        String userId = "userId_example"; // String | User ID to fetch portfolios for
        try {
            List<PortfolioModelV1> result = apiInstance.getPortfolios(userId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling PortfolioManagementApi#getPortfolios");
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
| **userId** | **String**| User ID to fetch portfolios for | |

### Return type

[**List&lt;PortfolioModelV1&gt;**](PortfolioModelV1.md)


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | List of portfolios retrieved successfully |  -  |
| **404** | No portfolios found for user |  -  |

## getPortfoliosWithHttpInfo

> ApiResponse<List<PortfolioModelV1>> getPortfolios getPortfoliosWithHttpInfo(userId)

Get all portfolios for user

Retrieves all portfolios associated with a specific user ID

### Example

```java
// Import classes:
import com.am.portfolio.client.invoker.ApiClient;
import com.am.portfolio.client.invoker.ApiException;
import com.am.portfolio.client.invoker.ApiResponse;
import com.am.portfolio.client.invoker.Configuration;
import com.am.portfolio.client.invoker.models.*;
import com.am.portfolio.client.api.PortfolioManagementApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8101");

        PortfolioManagementApi apiInstance = new PortfolioManagementApi(defaultClient);
        String userId = "userId_example"; // String | User ID to fetch portfolios for
        try {
            ApiResponse<List<PortfolioModelV1>> response = apiInstance.getPortfoliosWithHttpInfo(userId);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling PortfolioManagementApi#getPortfolios");
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
| **userId** | **String**| User ID to fetch portfolios for | |

### Return type

ApiResponse<[**List&lt;PortfolioModelV1&gt;**](PortfolioModelV1.md)>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | List of portfolios retrieved successfully |  -  |
| **404** | No portfolios found for user |  -  |

