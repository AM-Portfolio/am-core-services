//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class PerformanceResponse {
  /// Returns a new [PerformanceResponse] instance.
  PerformanceResponse({
    this.portfolioId,
    this.timeFrame,
    this.totalReturnPercentage,
    this.totalReturnValue,
    this.chartData = const [],
    this.errorMessage,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? portfolioId;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? timeFrame;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  double? totalReturnPercentage;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  num? totalReturnValue;

  List<DataPoint> chartData;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? errorMessage;

  @override
  bool operator ==(Object other) => identical(this, other) || other is PerformanceResponse &&
    other.portfolioId == portfolioId &&
    other.timeFrame == timeFrame &&
    other.totalReturnPercentage == totalReturnPercentage &&
    other.totalReturnValue == totalReturnValue &&
    _deepEquality.equals(other.chartData, chartData) &&
    other.errorMessage == errorMessage;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (portfolioId == null ? 0 : portfolioId!.hashCode) +
    (timeFrame == null ? 0 : timeFrame!.hashCode) +
    (totalReturnPercentage == null ? 0 : totalReturnPercentage!.hashCode) +
    (totalReturnValue == null ? 0 : totalReturnValue!.hashCode) +
    (chartData.hashCode) +
    (errorMessage == null ? 0 : errorMessage!.hashCode);

  @override
  String toString() => 'PerformanceResponse[portfolioId=$portfolioId, timeFrame=$timeFrame, totalReturnPercentage=$totalReturnPercentage, totalReturnValue=$totalReturnValue, chartData=$chartData, errorMessage=$errorMessage]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.portfolioId != null) {
      json[r'portfolioId'] = this.portfolioId;
    } else {
      json[r'portfolioId'] = null;
    }
    if (this.timeFrame != null) {
      json[r'timeFrame'] = this.timeFrame;
    } else {
      json[r'timeFrame'] = null;
    }
    if (this.totalReturnPercentage != null) {
      json[r'totalReturnPercentage'] = this.totalReturnPercentage;
    } else {
      json[r'totalReturnPercentage'] = null;
    }
    if (this.totalReturnValue != null) {
      json[r'totalReturnValue'] = this.totalReturnValue;
    } else {
      json[r'totalReturnValue'] = null;
    }
      json[r'chartData'] = this.chartData;
    if (this.errorMessage != null) {
      json[r'errorMessage'] = this.errorMessage;
    } else {
      json[r'errorMessage'] = null;
    }
    return json;
  }

  /// Returns a new [PerformanceResponse] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static PerformanceResponse? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "PerformanceResponse[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "PerformanceResponse[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return PerformanceResponse(
        portfolioId: mapValueOfType<String>(json, r'portfolioId'),
        timeFrame: mapValueOfType<String>(json, r'timeFrame'),
        totalReturnPercentage: mapValueOfType<double>(json, r'totalReturnPercentage'),
        totalReturnValue: num.parse('${json[r'totalReturnValue']}'),
        chartData: DataPoint.listFromJson(json[r'chartData']),
        errorMessage: mapValueOfType<String>(json, r'errorMessage'),
      );
    }
    return null;
  }

  static List<PerformanceResponse> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <PerformanceResponse>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = PerformanceResponse.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, PerformanceResponse> mapFromJson(dynamic json) {
    final map = <String, PerformanceResponse>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = PerformanceResponse.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of PerformanceResponse-objects as value to a dart map
  static Map<String, List<PerformanceResponse>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<PerformanceResponse>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = PerformanceResponse.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

