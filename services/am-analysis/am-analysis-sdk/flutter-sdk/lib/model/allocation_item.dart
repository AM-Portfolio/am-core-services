//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class AllocationItem {
  /// Returns a new [AllocationItem] instance.
  AllocationItem({
    this.name,
    this.value,
    this.percentage,
    this.holdings = const [],
    this.dayChangePercentage,
    this.dayChangeAmount,
    this.totalChangePercentage,
    this.totalChangeAmount,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? name;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  num? value;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  double? percentage;

  List<AllocationHolding> holdings;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  double? dayChangePercentage;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  num? dayChangeAmount;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  double? totalChangePercentage;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  num? totalChangeAmount;

  @override
  bool operator ==(Object other) => identical(this, other) || other is AllocationItem &&
    other.name == name &&
    other.value == value &&
    other.percentage == percentage &&
    _deepEquality.equals(other.holdings, holdings) &&
    other.dayChangePercentage == dayChangePercentage &&
    other.dayChangeAmount == dayChangeAmount &&
    other.totalChangePercentage == totalChangePercentage &&
    other.totalChangeAmount == totalChangeAmount;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (name == null ? 0 : name!.hashCode) +
    (value == null ? 0 : value!.hashCode) +
    (percentage == null ? 0 : percentage!.hashCode) +
    (holdings.hashCode) +
    (dayChangePercentage == null ? 0 : dayChangePercentage!.hashCode) +
    (dayChangeAmount == null ? 0 : dayChangeAmount!.hashCode) +
    (totalChangePercentage == null ? 0 : totalChangePercentage!.hashCode) +
    (totalChangeAmount == null ? 0 : totalChangeAmount!.hashCode);

  @override
  String toString() => 'AllocationItem[name=$name, value=$value, percentage=$percentage, holdings=$holdings, dayChangePercentage=$dayChangePercentage, dayChangeAmount=$dayChangeAmount, totalChangePercentage=$totalChangePercentage, totalChangeAmount=$totalChangeAmount]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.name != null) {
      json[r'name'] = this.name;
    } else {
      json[r'name'] = null;
    }
    if (this.value != null) {
      json[r'value'] = this.value;
    } else {
      json[r'value'] = null;
    }
    if (this.percentage != null) {
      json[r'percentage'] = this.percentage;
    } else {
      json[r'percentage'] = null;
    }
      json[r'holdings'] = this.holdings;
    if (this.dayChangePercentage != null) {
      json[r'dayChangePercentage'] = this.dayChangePercentage;
    } else {
      json[r'dayChangePercentage'] = null;
    }
    if (this.dayChangeAmount != null) {
      json[r'dayChangeAmount'] = this.dayChangeAmount;
    } else {
      json[r'dayChangeAmount'] = null;
    }
    if (this.totalChangePercentage != null) {
      json[r'totalChangePercentage'] = this.totalChangePercentage;
    } else {
      json[r'totalChangePercentage'] = null;
    }
    if (this.totalChangeAmount != null) {
      json[r'totalChangeAmount'] = this.totalChangeAmount;
    } else {
      json[r'totalChangeAmount'] = null;
    }
    return json;
  }

  /// Returns a new [AllocationItem] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static AllocationItem? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "AllocationItem[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "AllocationItem[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return AllocationItem(
        name: mapValueOfType<String>(json, r'name'),
        value: num.parse('${json[r'value']}'),
        percentage: mapValueOfType<double>(json, r'percentage'),
        holdings: AllocationHolding.listFromJson(json[r'holdings']),
        dayChangePercentage: mapValueOfType<double>(json, r'dayChangePercentage'),
        dayChangeAmount: num.parse('${json[r'dayChangeAmount']}'),
        totalChangePercentage: mapValueOfType<double>(json, r'totalChangePercentage'),
        totalChangeAmount: num.parse('${json[r'totalChangeAmount']}'),
      );
    }
    return null;
  }

  static List<AllocationItem> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <AllocationItem>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = AllocationItem.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, AllocationItem> mapFromJson(dynamic json) {
    final map = <String, AllocationItem>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = AllocationItem.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of AllocationItem-objects as value to a dart map
  static Map<String, List<AllocationItem>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<AllocationItem>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = AllocationItem.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

