//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class RecentActivityResponse {
  /// Returns a new [RecentActivityResponse] instance.
  RecentActivityResponse({
    this.items = const [],
    this.page,
    this.size,
    this.totalItems,
    this.totalPages,
    this.hasNext,
    this.hasPrevious,
    this.totalWinning,
    this.totalLosing,
    this.totalNeutral,
  });

  List<ActivityItem> items;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? page;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? size;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? totalItems;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? totalPages;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  bool? hasNext;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  bool? hasPrevious;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? totalWinning;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? totalLosing;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? totalNeutral;

  @override
  bool operator ==(Object other) => identical(this, other) || other is RecentActivityResponse &&
    _deepEquality.equals(other.items, items) &&
    other.page == page &&
    other.size == size &&
    other.totalItems == totalItems &&
    other.totalPages == totalPages &&
    other.hasNext == hasNext &&
    other.hasPrevious == hasPrevious &&
    other.totalWinning == totalWinning &&
    other.totalLosing == totalLosing &&
    other.totalNeutral == totalNeutral;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (items.hashCode) +
    (page == null ? 0 : page!.hashCode) +
    (size == null ? 0 : size!.hashCode) +
    (totalItems == null ? 0 : totalItems!.hashCode) +
    (totalPages == null ? 0 : totalPages!.hashCode) +
    (hasNext == null ? 0 : hasNext!.hashCode) +
    (hasPrevious == null ? 0 : hasPrevious!.hashCode) +
    (totalWinning == null ? 0 : totalWinning!.hashCode) +
    (totalLosing == null ? 0 : totalLosing!.hashCode) +
    (totalNeutral == null ? 0 : totalNeutral!.hashCode);

  @override
  String toString() => 'RecentActivityResponse[items=$items, page=$page, size=$size, totalItems=$totalItems, totalPages=$totalPages, hasNext=$hasNext, hasPrevious=$hasPrevious, totalWinning=$totalWinning, totalLosing=$totalLosing, totalNeutral=$totalNeutral]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
      json[r'items'] = this.items;
    if (this.page != null) {
      json[r'page'] = this.page;
    } else {
      json[r'page'] = null;
    }
    if (this.size != null) {
      json[r'size'] = this.size;
    } else {
      json[r'size'] = null;
    }
    if (this.totalItems != null) {
      json[r'totalItems'] = this.totalItems;
    } else {
      json[r'totalItems'] = null;
    }
    if (this.totalPages != null) {
      json[r'totalPages'] = this.totalPages;
    } else {
      json[r'totalPages'] = null;
    }
    if (this.hasNext != null) {
      json[r'hasNext'] = this.hasNext;
    } else {
      json[r'hasNext'] = null;
    }
    if (this.hasPrevious != null) {
      json[r'hasPrevious'] = this.hasPrevious;
    } else {
      json[r'hasPrevious'] = null;
    }
    if (this.totalWinning != null) {
      json[r'totalWinning'] = this.totalWinning;
    } else {
      json[r'totalWinning'] = null;
    }
    if (this.totalLosing != null) {
      json[r'totalLosing'] = this.totalLosing;
    } else {
      json[r'totalLosing'] = null;
    }
    if (this.totalNeutral != null) {
      json[r'totalNeutral'] = this.totalNeutral;
    } else {
      json[r'totalNeutral'] = null;
    }
    return json;
  }

  /// Returns a new [RecentActivityResponse] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static RecentActivityResponse? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "RecentActivityResponse[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "RecentActivityResponse[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return RecentActivityResponse(
        items: ActivityItem.listFromJson(json[r'items']),
        page: mapValueOfType<int>(json, r'page'),
        size: mapValueOfType<int>(json, r'size'),
        totalItems: mapValueOfType<int>(json, r'totalItems'),
        totalPages: mapValueOfType<int>(json, r'totalPages'),
        hasNext: mapValueOfType<bool>(json, r'hasNext'),
        hasPrevious: mapValueOfType<bool>(json, r'hasPrevious'),
        totalWinning: mapValueOfType<int>(json, r'totalWinning'),
        totalLosing: mapValueOfType<int>(json, r'totalLosing'),
        totalNeutral: mapValueOfType<int>(json, r'totalNeutral'),
      );
    }
    return null;
  }

  static List<RecentActivityResponse> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <RecentActivityResponse>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = RecentActivityResponse.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, RecentActivityResponse> mapFromJson(dynamic json) {
    final map = <String, RecentActivityResponse>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = RecentActivityResponse.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of RecentActivityResponse-objects as value to a dart map
  static Map<String, List<RecentActivityResponse>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<RecentActivityResponse>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = RecentActivityResponse.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

