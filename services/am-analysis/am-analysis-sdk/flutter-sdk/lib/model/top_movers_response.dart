//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class TopMoversResponse {
  /// Returns a new [TopMoversResponse] instance.
  TopMoversResponse({
    this.gainers = const [],
    this.losers = const [],
  });

  List<MoverItem> gainers;

  List<MoverItem> losers;

  @override
  bool operator ==(Object other) => identical(this, other) || other is TopMoversResponse &&
    _deepEquality.equals(other.gainers, gainers) &&
    _deepEquality.equals(other.losers, losers);

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (gainers.hashCode) +
    (losers.hashCode);

  @override
  String toString() => 'TopMoversResponse[gainers=$gainers, losers=$losers]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
      json[r'gainers'] = this.gainers;
      json[r'losers'] = this.losers;
    return json;
  }

  /// Returns a new [TopMoversResponse] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static TopMoversResponse? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "TopMoversResponse[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "TopMoversResponse[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return TopMoversResponse(
        gainers: MoverItem.listFromJson(json[r'gainers']),
        losers: MoverItem.listFromJson(json[r'losers']),
      );
    }
    return null;
  }

  static List<TopMoversResponse> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <TopMoversResponse>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = TopMoversResponse.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, TopMoversResponse> mapFromJson(dynamic json) {
    final map = <String, TopMoversResponse>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = TopMoversResponse.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of TopMoversResponse-objects as value to a dart map
  static Map<String, List<TopMoversResponse>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<TopMoversResponse>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = TopMoversResponse.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

