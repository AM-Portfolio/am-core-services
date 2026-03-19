//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;


class AnalysisControllerApi {
  AnalysisControllerApi([ApiClient? apiClient]) : apiClient = apiClient ?? defaultApiClient;

  final ApiClient apiClient;

  /// Performs an HTTP 'GET /api/v1/analysis/{type}/{id}/allocation' operation and returns the [Response].
  /// Parameters:
  ///
  /// * [String] authorization (required):
  ///
  /// * [String] type (required):
  ///
  /// * [String] id (required):
  ///
  /// * [String] groupBy:
  ///
  /// * [String] groupBy2:
  Future<Response> getAllocationWithHttpInfo(String authorization, String type, String id, { String? groupBy, String? groupBy2, }) async {
    // ignore: prefer_const_declarations
    final path = r'/api/v1/analysis/{type}/{id}/allocation'
      .replaceAll('{type}', type)
      .replaceAll('{id}', id);

    // ignore: prefer_final_locals
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    if (groupBy2 != null) {
      queryParams.addAll(_queryParams('', 'groupBy', groupBy2));
    }

    headerParams[r'Authorization'] = parameterToString(authorization);
    if (groupBy != null) {
      headerParams[r'groupBy'] = parameterToString(groupBy);
    }

    const contentTypes = <String>[];


    return apiClient.invokeAPI(
      path,
      'GET',
      queryParams,
      postBody,
      headerParams,
      formParams,
      contentTypes.isEmpty ? null : contentTypes.first,
    );
  }

  /// Parameters:
  ///
  /// * [String] authorization (required):
  ///
  /// * [String] type (required):
  ///
  /// * [String] id (required):
  ///
  /// * [String] groupBy:
  ///
  /// * [String] groupBy2:
  Future<AllocationResponse?> getAllocation(String authorization, String type, String id, { String? groupBy, String? groupBy2, }) async {
    final response = await getAllocationWithHttpInfo(authorization, type, id,  groupBy: groupBy, groupBy2: groupBy2, );
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'AllocationResponse',) as AllocationResponse;
    
    }
    return null;
  }

  /// Performs an HTTP 'GET /api/v1/analysis/dashboard/performance' operation and returns the [Response].
  /// Parameters:
  ///
  /// * [String] arg0 (required):
  ///
  /// * [String] arg1:
  Future<Response> getDashboardPerformanceWithHttpInfo(String arg0, { String? arg1, }) async {
    // ignore: prefer_const_declarations
    final path = r'/api/v1/analysis/dashboard/performance';

    // ignore: prefer_final_locals
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

      queryParams.addAll(_queryParams('', 'arg0', arg0));
    if (arg1 != null) {
      queryParams.addAll(_queryParams('', 'arg1', arg1));
    }

    const contentTypes = <String>[];


    return apiClient.invokeAPI(
      path,
      'GET',
      queryParams,
      postBody,
      headerParams,
      formParams,
      contentTypes.isEmpty ? null : contentTypes.first,
    );
  }

  /// Parameters:
  ///
  /// * [String] arg0 (required):
  ///
  /// * [String] arg1:
  Future<PerformanceResponse?> getDashboardPerformance(String arg0, { String? arg1, }) async {
    final response = await getDashboardPerformanceWithHttpInfo(arg0,  arg1: arg1, );
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'PerformanceResponse',) as PerformanceResponse;
    
    }
    return null;
  }

  /// Performs an HTTP 'GET /api/v1/analysis/dashboard/summary' operation and returns the [Response].
  /// Parameters:
  ///
  /// * [String] arg0 (required):
  Future<Response> getDashboardSummaryWithHttpInfo(String arg0,) async {
    // ignore: prefer_const_declarations
    final path = r'/api/v1/analysis/dashboard/summary';

    // ignore: prefer_final_locals
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

      queryParams.addAll(_queryParams('', 'arg0', arg0));

    const contentTypes = <String>[];


    return apiClient.invokeAPI(
      path,
      'GET',
      queryParams,
      postBody,
      headerParams,
      formParams,
      contentTypes.isEmpty ? null : contentTypes.first,
    );
  }

  /// Parameters:
  ///
  /// * [String] arg0 (required):
  Future<DashboardSummary?> getDashboardSummary(String arg0,) async {
    final response = await getDashboardSummaryWithHttpInfo(arg0,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'DashboardSummary',) as DashboardSummary;
    
    }
    return null;
  }

  /// Performs an HTTP 'GET /api/v1/analysis/dashboard/top-movers' operation and returns the [Response].
  /// Parameters:
  ///
  /// * [String] arg0 (required):
  ///
  /// * [String] arg1:
  Future<Response> getDashboardTopMoversWithHttpInfo(String arg0, { String? arg1, }) async {
    // ignore: prefer_const_declarations
    final path = r'/api/v1/analysis/dashboard/top-movers';

    // ignore: prefer_final_locals
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

      queryParams.addAll(_queryParams('', 'arg0', arg0));
    if (arg1 != null) {
      queryParams.addAll(_queryParams('', 'arg1', arg1));
    }

    const contentTypes = <String>[];


    return apiClient.invokeAPI(
      path,
      'GET',
      queryParams,
      postBody,
      headerParams,
      formParams,
      contentTypes.isEmpty ? null : contentTypes.first,
    );
  }

  /// Parameters:
  ///
  /// * [String] arg0 (required):
  ///
  /// * [String] arg1:
  Future<TopMoversResponse?> getDashboardTopMovers(String arg0, { String? arg1, }) async {
    final response = await getDashboardTopMoversWithHttpInfo(arg0,  arg1: arg1, );
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'TopMoversResponse',) as TopMoversResponse;
    
    }
    return null;
  }

  /// Performs an HTTP 'GET /api/v1/analysis/{type}/{id}/performance' operation and returns the [Response].
  /// Parameters:
  ///
  /// * [String] authorization (required):
  ///
  /// * [String] type (required):
  ///
  /// * [String] id (required):
  ///
  /// * [String] timeFrame:
  Future<Response> getPerformanceWithHttpInfo(String authorization, String type, String id, { String? timeFrame, }) async {
    // ignore: prefer_const_declarations
    final path = r'/api/v1/analysis/{type}/{id}/performance'
      .replaceAll('{type}', type)
      .replaceAll('{id}', id);

    // ignore: prefer_final_locals
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    if (timeFrame != null) {
      queryParams.addAll(_queryParams('', 'timeFrame', timeFrame));
    }

    headerParams[r'Authorization'] = parameterToString(authorization);

    const contentTypes = <String>[];


    return apiClient.invokeAPI(
      path,
      'GET',
      queryParams,
      postBody,
      headerParams,
      formParams,
      contentTypes.isEmpty ? null : contentTypes.first,
    );
  }

  /// Parameters:
  ///
  /// * [String] authorization (required):
  ///
  /// * [String] type (required):
  ///
  /// * [String] id (required):
  ///
  /// * [String] timeFrame:
  Future<PerformanceResponse?> getPerformance(String authorization, String type, String id, { String? timeFrame, }) async {
    final response = await getPerformanceWithHttpInfo(authorization, type, id,  timeFrame: timeFrame, );
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'PerformanceResponse',) as PerformanceResponse;
    
    }
    return null;
  }

  /// Performs an HTTP 'GET /api/v1/analysis/dashboard/portfolio-overviews' operation and returns the [Response].
  /// Parameters:
  ///
  /// * [String] arg0 (required):
  ///
  /// * [String] arg1:
  Future<Response> getPortfolioOverviewsWithHttpInfo(String arg0, { String? arg1, }) async {
    // ignore: prefer_const_declarations
    final path = r'/api/v1/analysis/dashboard/portfolio-overviews';

    // ignore: prefer_final_locals
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

      queryParams.addAll(_queryParams('', 'arg0', arg0));
    if (arg1 != null) {
      queryParams.addAll(_queryParams('', 'arg1', arg1));
    }

    const contentTypes = <String>[];


    return apiClient.invokeAPI(
      path,
      'GET',
      queryParams,
      postBody,
      headerParams,
      formParams,
      contentTypes.isEmpty ? null : contentTypes.first,
    );
  }

  /// Parameters:
  ///
  /// * [String] arg0 (required):
  ///
  /// * [String] arg1:
  Future<List<PortfolioOverview>?> getPortfolioOverviews(String arg0, { String? arg1, }) async {
    final response = await getPortfolioOverviewsWithHttpInfo(arg0,  arg1: arg1, );
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      final responseBody = await _decodeBodyBytes(response);
      return (await apiClient.deserializeAsync(responseBody, 'List<PortfolioOverview>') as List)
        .cast<PortfolioOverview>()
        .toList(growable: false);

    }
    return null;
  }

  /// Performs an HTTP 'GET /api/v1/analysis/dashboard/recent-activity' operation and returns the [Response].
  /// Parameters:
  ///
  /// * [String] arg0 (required):
  ///
  /// * [String] arg1:
  ///
  /// * [String] arg2:
  ///
  /// * [String] arg3:
  ///
  /// * [String] arg4:
  ///
  /// * [String] arg5:
  ///
  /// * [int] arg6:
  ///
  /// * [int] arg7:
  Future<Response> getRecentActivityWithHttpInfo(String arg0, { String? arg1, String? arg2, String? arg3, String? arg4, String? arg5, int? arg6, int? arg7, }) async {
    // ignore: prefer_const_declarations
    final path = r'/api/v1/analysis/dashboard/recent-activity';

    // ignore: prefer_final_locals
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

      queryParams.addAll(_queryParams('', 'arg0', arg0));
    if (arg1 != null) {
      queryParams.addAll(_queryParams('', 'arg1', arg1));
    }
    if (arg2 != null) {
      queryParams.addAll(_queryParams('', 'arg2', arg2));
    }
    if (arg3 != null) {
      queryParams.addAll(_queryParams('', 'arg3', arg3));
    }
    if (arg4 != null) {
      queryParams.addAll(_queryParams('', 'arg4', arg4));
    }
    if (arg5 != null) {
      queryParams.addAll(_queryParams('', 'arg5', arg5));
    }
    if (arg6 != null) {
      queryParams.addAll(_queryParams('', 'arg6', arg6));
    }
    if (arg7 != null) {
      queryParams.addAll(_queryParams('', 'arg7', arg7));
    }

    const contentTypes = <String>[];


    return apiClient.invokeAPI(
      path,
      'GET',
      queryParams,
      postBody,
      headerParams,
      formParams,
      contentTypes.isEmpty ? null : contentTypes.first,
    );
  }

  /// Parameters:
  ///
  /// * [String] arg0 (required):
  ///
  /// * [String] arg1:
  ///
  /// * [String] arg2:
  ///
  /// * [String] arg3:
  ///
  /// * [String] arg4:
  ///
  /// * [String] arg5:
  ///
  /// * [int] arg6:
  ///
  /// * [int] arg7:
  Future<RecentActivityResponse?> getRecentActivity(String arg0, { String? arg1, String? arg2, String? arg3, String? arg4, String? arg5, int? arg6, int? arg7, }) async {
    final response = await getRecentActivityWithHttpInfo(arg0,  arg1: arg1, arg2: arg2, arg3: arg3, arg4: arg4, arg5: arg5, arg6: arg6, arg7: arg7, );
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'RecentActivityResponse',) as RecentActivityResponse;
    
    }
    return null;
  }

  /// Performs an HTTP 'GET /api/v1/analysis/{type}/top-movers' operation and returns the [Response].
  /// Parameters:
  ///
  /// * [String] authorization (required):
  ///
  /// * [String] type (required):
  ///
  /// * [String] timeFrame:
  ///
  /// * [String] groupBy:
  ///
  /// * [String] groupBy2:
  Future<Response> getTopMoversByCategoryWithHttpInfo(String authorization, String type, { String? timeFrame, String? groupBy, String? groupBy2, }) async {
    // ignore: prefer_const_declarations
    final path = r'/api/v1/analysis/{type}/top-movers'
      .replaceAll('{type}', type);

    // ignore: prefer_final_locals
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    if (timeFrame != null) {
      queryParams.addAll(_queryParams('', 'timeFrame', timeFrame));
    }
    if (groupBy2 != null) {
      queryParams.addAll(_queryParams('', 'groupBy', groupBy2));
    }

    headerParams[r'Authorization'] = parameterToString(authorization);
    if (groupBy != null) {
      headerParams[r'groupBy'] = parameterToString(groupBy);
    }

    const contentTypes = <String>[];


    return apiClient.invokeAPI(
      path,
      'GET',
      queryParams,
      postBody,
      headerParams,
      formParams,
      contentTypes.isEmpty ? null : contentTypes.first,
    );
  }

  /// Parameters:
  ///
  /// * [String] authorization (required):
  ///
  /// * [String] type (required):
  ///
  /// * [String] timeFrame:
  ///
  /// * [String] groupBy:
  ///
  /// * [String] groupBy2:
  Future<TopMoversResponse?> getTopMoversByCategory(String authorization, String type, { String? timeFrame, String? groupBy, String? groupBy2, }) async {
    final response = await getTopMoversByCategoryWithHttpInfo(authorization, type,  timeFrame: timeFrame, groupBy: groupBy, groupBy2: groupBy2, );
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'TopMoversResponse',) as TopMoversResponse;
    
    }
    return null;
  }

  /// Performs an HTTP 'GET /api/v1/analysis/{type}/{id}/top-movers' operation and returns the [Response].
  /// Parameters:
  ///
  /// * [String] authorization (required):
  ///
  /// * [String] type (required):
  ///
  /// * [String] id (required):
  ///
  /// * [String] timeFrame:
  ///
  /// * [String] groupBy:
  ///
  /// * [String] groupBy2:
  Future<Response> getTopMoversByEntityWithHttpInfo(String authorization, String type, String id, { String? timeFrame, String? groupBy, String? groupBy2, }) async {
    // ignore: prefer_const_declarations
    final path = r'/api/v1/analysis/{type}/{id}/top-movers'
      .replaceAll('{type}', type)
      .replaceAll('{id}', id);

    // ignore: prefer_final_locals
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    if (timeFrame != null) {
      queryParams.addAll(_queryParams('', 'timeFrame', timeFrame));
    }
    if (groupBy2 != null) {
      queryParams.addAll(_queryParams('', 'groupBy', groupBy2));
    }

    headerParams[r'Authorization'] = parameterToString(authorization);
    if (groupBy != null) {
      headerParams[r'groupBy'] = parameterToString(groupBy);
    }

    const contentTypes = <String>[];


    return apiClient.invokeAPI(
      path,
      'GET',
      queryParams,
      postBody,
      headerParams,
      formParams,
      contentTypes.isEmpty ? null : contentTypes.first,
    );
  }

  /// Parameters:
  ///
  /// * [String] authorization (required):
  ///
  /// * [String] type (required):
  ///
  /// * [String] id (required):
  ///
  /// * [String] timeFrame:
  ///
  /// * [String] groupBy:
  ///
  /// * [String] groupBy2:
  Future<TopMoversResponse?> getTopMoversByEntity(String authorization, String type, String id, { String? timeFrame, String? groupBy, String? groupBy2, }) async {
    final response = await getTopMoversByEntityWithHttpInfo(authorization, type, id,  timeFrame: timeFrame, groupBy: groupBy, groupBy2: groupBy2, );
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'TopMoversResponse',) as TopMoversResponse;
    
    }
    return null;
  }

  /// Performs an HTTP 'POST /api/v1/analysis/dashboard/publish-update' operation and returns the [Response].
  /// Parameters:
  ///
  /// * [String] arg0 (required):
  Future<Response> publishDashboardUpdateWithHttpInfo(String arg0,) async {
    // ignore: prefer_const_declarations
    final path = r'/api/v1/analysis/dashboard/publish-update';

    // ignore: prefer_final_locals
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

      queryParams.addAll(_queryParams('', 'arg0', arg0));

    const contentTypes = <String>[];


    return apiClient.invokeAPI(
      path,
      'POST',
      queryParams,
      postBody,
      headerParams,
      formParams,
      contentTypes.isEmpty ? null : contentTypes.first,
    );
  }

  /// Parameters:
  ///
  /// * [String] arg0 (required):
  Future<void> publishDashboardUpdate(String arg0,) async {
    final response = await publishDashboardUpdateWithHttpInfo(arg0,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
  }
}
