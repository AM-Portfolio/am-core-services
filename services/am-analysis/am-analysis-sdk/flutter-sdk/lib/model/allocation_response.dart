//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class AllocationResponse {
  /// Returns a new [AllocationResponse] instance.
  AllocationResponse({
    this.portfolioId,
    this.sectors = const [],
    this.assetClasses = const [],
    this.marketCaps = const [],
    this.stocks = const [],
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? portfolioId;

  List<AllocationItem> sectors;

  List<AllocationItem> assetClasses;

  List<AllocationItem> marketCaps;

  List<AllocationItem> stocks;

  @override
  bool operator ==(Object other) => identical(this, other) || other is AllocationResponse &&
    other.portfolioId == portfolioId &&
    _deepEquality.equals(other.sectors, sectors) &&
    _deepEquality.equals(other.assetClasses, assetClasses) &&
    _deepEquality.equals(other.marketCaps, marketCaps) &&
    _deepEquality.equals(other.stocks, stocks);

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (portfolioId == null ? 0 : portfolioId!.hashCode) +
    (sectors.hashCode) +
    (assetClasses.hashCode) +
    (marketCaps.hashCode) +
    (stocks.hashCode);

  @override
  String toString() => 'AllocationResponse[portfolioId=$portfolioId, sectors=$sectors, assetClasses=$assetClasses, marketCaps=$marketCaps, stocks=$stocks]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.portfolioId != null) {
      json[r'portfolioId'] = this.portfolioId;
    } else {
      json[r'portfolioId'] = null;
    }
      json[r'sectors'] = this.sectors;
      json[r'assetClasses'] = this.assetClasses;
      json[r'marketCaps'] = this.marketCaps;
      json[r'stocks'] = this.stocks;
    return json;
  }

  /// Returns a new [AllocationResponse] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static AllocationResponse? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "AllocationResponse[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "AllocationResponse[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return AllocationResponse(
        portfolioId: mapValueOfType<String>(json, r'portfolioId'),
        sectors: AllocationItem.listFromJson(json[r'sectors']),
        assetClasses: AllocationItem.listFromJson(json[r'assetClasses']),
        marketCaps: AllocationItem.listFromJson(json[r'marketCaps']),
        stocks: AllocationItem.listFromJson(json[r'stocks']),
      );
    }
    return null;
  }

  static List<AllocationResponse> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <AllocationResponse>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = AllocationResponse.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, AllocationResponse> mapFromJson(dynamic json) {
    final map = <String, AllocationResponse>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = AllocationResponse.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of AllocationResponse-objects as value to a dart map
  static Map<String, List<AllocationResponse>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<AllocationResponse>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = AllocationResponse.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

