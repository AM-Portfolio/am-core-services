# am_analysis_sdk.api.AnalysisControllerApi

## Load the API package
```dart
import 'package:am_analysis_sdk/api.dart';
```

All URIs are relative to *http://localhost:8090*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getAllocation**](AnalysisControllerApi.md#getallocation) | **GET** /api/v1/analysis/{type}/{id}/allocation | 
[**getDashboardPerformance**](AnalysisControllerApi.md#getdashboardperformance) | **GET** /api/v1/analysis/dashboard/performance | 
[**getDashboardSummary**](AnalysisControllerApi.md#getdashboardsummary) | **GET** /api/v1/analysis/dashboard/summary | 
[**getDashboardTopMovers**](AnalysisControllerApi.md#getdashboardtopmovers) | **GET** /api/v1/analysis/dashboard/top-movers | 
[**getPerformance**](AnalysisControllerApi.md#getperformance) | **GET** /api/v1/analysis/{type}/{id}/performance | 
[**getPortfolioOverviews**](AnalysisControllerApi.md#getportfoliooverviews) | **GET** /api/v1/analysis/dashboard/portfolio-overviews | 
[**getRecentActivity**](AnalysisControllerApi.md#getrecentactivity) | **GET** /api/v1/analysis/dashboard/recent-activity | 
[**getTopMoversByCategory**](AnalysisControllerApi.md#gettopmoversbycategory) | **GET** /api/v1/analysis/{type}/top-movers | 
[**getTopMoversByEntity**](AnalysisControllerApi.md#gettopmoversbyentity) | **GET** /api/v1/analysis/{type}/{id}/top-movers | 
[**publishDashboardUpdate**](AnalysisControllerApi.md#publishdashboardupdate) | **POST** /api/v1/analysis/dashboard/publish-update | 


# **getAllocation**
> AllocationResponse getAllocation(authorization, type, id, groupBy, groupBy2)



### Example
```dart
import 'package:am_analysis_sdk/api.dart';

final api_instance = AnalysisControllerApi();
final authorization = authorization_example; // String | 
final type = type_example; // String | 
final id = id_example; // String | 
final groupBy = groupBy_example; // String | 
final groupBy2 = groupBy_example; // String | 

try {
    final result = api_instance.getAllocation(authorization, type, id, groupBy, groupBy2);
    print(result);
} catch (e) {
    print('Exception when calling AnalysisControllerApi->getAllocation: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **authorization** | **String**|  | 
 **type** | **String**|  | 
 **id** | **String**|  | 
 **groupBy** | **String**|  | [optional] 
 **groupBy2** | **String**|  | [optional] 

### Return type

[**AllocationResponse**](AllocationResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getDashboardPerformance**
> PerformanceResponse getDashboardPerformance(arg0, arg1)



### Example
```dart
import 'package:am_analysis_sdk/api.dart';

final api_instance = AnalysisControllerApi();
final arg0 = arg0_example; // String | 
final arg1 = arg1_example; // String | 

try {
    final result = api_instance.getDashboardPerformance(arg0, arg1);
    print(result);
} catch (e) {
    print('Exception when calling AnalysisControllerApi->getDashboardPerformance: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **arg0** | **String**|  | 
 **arg1** | **String**|  | [optional] [default to '1M']

### Return type

[**PerformanceResponse**](PerformanceResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getDashboardSummary**
> DashboardSummary getDashboardSummary(arg0)



### Example
```dart
import 'package:am_analysis_sdk/api.dart';

final api_instance = AnalysisControllerApi();
final arg0 = arg0_example; // String | 

try {
    final result = api_instance.getDashboardSummary(arg0);
    print(result);
} catch (e) {
    print('Exception when calling AnalysisControllerApi->getDashboardSummary: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **arg0** | **String**|  | 

### Return type

[**DashboardSummary**](DashboardSummary.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getDashboardTopMovers**
> TopMoversResponse getDashboardTopMovers(arg0, arg1)



### Example
```dart
import 'package:am_analysis_sdk/api.dart';

final api_instance = AnalysisControllerApi();
final arg0 = arg0_example; // String | 
final arg1 = arg1_example; // String | 

try {
    final result = api_instance.getDashboardTopMovers(arg0, arg1);
    print(result);
} catch (e) {
    print('Exception when calling AnalysisControllerApi->getDashboardTopMovers: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **arg0** | **String**|  | 
 **arg1** | **String**|  | [optional] [default to '1D']

### Return type

[**TopMoversResponse**](TopMoversResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getPerformance**
> PerformanceResponse getPerformance(authorization, type, id, timeFrame)



### Example
```dart
import 'package:am_analysis_sdk/api.dart';

final api_instance = AnalysisControllerApi();
final authorization = authorization_example; // String | 
final type = type_example; // String | 
final id = id_example; // String | 
final timeFrame = timeFrame_example; // String | 

try {
    final result = api_instance.getPerformance(authorization, type, id, timeFrame);
    print(result);
} catch (e) {
    print('Exception when calling AnalysisControllerApi->getPerformance: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **authorization** | **String**|  | 
 **type** | **String**|  | 
 **id** | **String**|  | 
 **timeFrame** | **String**|  | [optional] [default to '1M']

### Return type

[**PerformanceResponse**](PerformanceResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getPortfolioOverviews**
> List<PortfolioOverview> getPortfolioOverviews(arg0, arg1)



### Example
```dart
import 'package:am_analysis_sdk/api.dart';

final api_instance = AnalysisControllerApi();
final arg0 = arg0_example; // String | 
final arg1 = arg1_example; // String | 

try {
    final result = api_instance.getPortfolioOverviews(arg0, arg1);
    print(result);
} catch (e) {
    print('Exception when calling AnalysisControllerApi->getPortfolioOverviews: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **arg0** | **String**|  | 
 **arg1** | **String**|  | [optional] 

### Return type

[**List<PortfolioOverview>**](PortfolioOverview.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getRecentActivity**
> RecentActivityResponse getRecentActivity(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7)



### Example
```dart
import 'package:am_analysis_sdk/api.dart';

final api_instance = AnalysisControllerApi();
final arg0 = arg0_example; // String | 
final arg1 = arg1_example; // String | 
final arg2 = arg2_example; // String | 
final arg3 = arg3_example; // String | 
final arg4 = arg4_example; // String | 
final arg5 = arg5_example; // String | 
final arg6 = 56; // int | 
final arg7 = 56; // int | 

try {
    final result = api_instance.getRecentActivity(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
    print(result);
} catch (e) {
    print('Exception when calling AnalysisControllerApi->getRecentActivity: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **arg0** | **String**|  | 
 **arg1** | **String**|  | [optional] 
 **arg2** | **String**|  | [optional] 
 **arg3** | **String**|  | [optional] 
 **arg4** | **String**|  | [optional] 
 **arg5** | **String**|  | [optional] [default to 'TIMESTAMP']
 **arg6** | **int**|  | [optional] [default to 0]
 **arg7** | **int**|  | [optional] [default to 20]

### Return type

[**RecentActivityResponse**](RecentActivityResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getTopMoversByCategory**
> TopMoversResponse getTopMoversByCategory(authorization, type, timeFrame, groupBy, groupBy2)



### Example
```dart
import 'package:am_analysis_sdk/api.dart';

final api_instance = AnalysisControllerApi();
final authorization = authorization_example; // String | 
final type = type_example; // String | 
final timeFrame = timeFrame_example; // String | 
final groupBy = groupBy_example; // String | 
final groupBy2 = groupBy_example; // String | 

try {
    final result = api_instance.getTopMoversByCategory(authorization, type, timeFrame, groupBy, groupBy2);
    print(result);
} catch (e) {
    print('Exception when calling AnalysisControllerApi->getTopMoversByCategory: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **authorization** | **String**|  | 
 **type** | **String**|  | 
 **timeFrame** | **String**|  | [optional] 
 **groupBy** | **String**|  | [optional] 
 **groupBy2** | **String**|  | [optional] 

### Return type

[**TopMoversResponse**](TopMoversResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getTopMoversByEntity**
> TopMoversResponse getTopMoversByEntity(authorization, type, id, timeFrame, groupBy, groupBy2)



### Example
```dart
import 'package:am_analysis_sdk/api.dart';

final api_instance = AnalysisControllerApi();
final authorization = authorization_example; // String | 
final type = type_example; // String | 
final id = id_example; // String | 
final timeFrame = timeFrame_example; // String | 
final groupBy = groupBy_example; // String | 
final groupBy2 = groupBy_example; // String | 

try {
    final result = api_instance.getTopMoversByEntity(authorization, type, id, timeFrame, groupBy, groupBy2);
    print(result);
} catch (e) {
    print('Exception when calling AnalysisControllerApi->getTopMoversByEntity: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **authorization** | **String**|  | 
 **type** | **String**|  | 
 **id** | **String**|  | 
 **timeFrame** | **String**|  | [optional] 
 **groupBy** | **String**|  | [optional] 
 **groupBy2** | **String**|  | [optional] 

### Return type

[**TopMoversResponse**](TopMoversResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **publishDashboardUpdate**
> publishDashboardUpdate(arg0)



### Example
```dart
import 'package:am_analysis_sdk/api.dart';

final api_instance = AnalysisControllerApi();
final arg0 = arg0_example; // String | 

try {
    api_instance.publishDashboardUpdate(arg0);
} catch (e) {
    print('Exception when calling AnalysisControllerApi->publishDashboardUpdate: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **arg0** | **String**|  | 

### Return type

void (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: Not defined

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

