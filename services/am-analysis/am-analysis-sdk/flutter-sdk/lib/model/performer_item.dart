//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class PerformerItem {
  /// Returns a new [PerformerItem] instance.
  PerformerItem({
    this.symbol,
    this.companyName,
    this.changePercent,
    this.profitLossPercent,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? symbol;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? companyName;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  double? changePercent;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  double? profitLossPercent;

  @override
  bool operator ==(Object other) => identical(this, other) || other is PerformerItem &&
    other.symbol == symbol &&
    other.companyName == companyName &&
    other.changePercent == changePercent &&
    other.profitLossPercent == profitLossPercent;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (symbol == null ? 0 : symbol!.hashCode) +
    (companyName == null ? 0 : companyName!.hashCode) +
    (changePercent == null ? 0 : changePercent!.hashCode) +
    (profitLossPercent == null ? 0 : profitLossPercent!.hashCode);

  @override
  String toString() => 'PerformerItem[symbol=$symbol, companyName=$companyName, changePercent=$changePercent, profitLossPercent=$profitLossPercent]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.symbol != null) {
      json[r'symbol'] = this.symbol;
    } else {
      json[r'symbol'] = null;
    }
    if (this.companyName != null) {
      json[r'companyName'] = this.companyName;
    } else {
      json[r'companyName'] = null;
    }
    if (this.changePercent != null) {
      json[r'changePercent'] = this.changePercent;
    } else {
      json[r'changePercent'] = null;
    }
    if (this.profitLossPercent != null) {
      json[r'profitLossPercent'] = this.profitLossPercent;
    } else {
      json[r'profitLossPercent'] = null;
    }
    return json;
  }

  /// Returns a new [PerformerItem] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static PerformerItem? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "PerformerItem[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "PerformerItem[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return PerformerItem(
        symbol: mapValueOfType<String>(json, r'symbol'),
        companyName: mapValueOfType<String>(json, r'companyName'),
        changePercent: mapValueOfType<double>(json, r'changePercent'),
        profitLossPercent: mapValueOfType<double>(json, r'profitLossPercent'),
      );
    }
    return null;
  }

  static List<PerformerItem> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <PerformerItem>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = PerformerItem.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, PerformerItem> mapFromJson(dynamic json) {
    final map = <String, PerformerItem>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = PerformerItem.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of PerformerItem-objects as value to a dart map
  static Map<String, List<PerformerItem>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<PerformerItem>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = PerformerItem.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

