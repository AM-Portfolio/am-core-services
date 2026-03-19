//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class AllocationHolding {
  /// Returns a new [AllocationHolding] instance.
  AllocationHolding({
    this.symbol,
    this.name,
    this.value,
    this.percentage,
    this.portfolioPercentage,
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
  String? symbol;

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

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  double? portfolioPercentage;

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
  bool operator ==(Object other) => identical(this, other) || other is AllocationHolding &&
    other.symbol == symbol &&
    other.name == name &&
    other.value == value &&
    other.percentage == percentage &&
    other.portfolioPercentage == portfolioPercentage &&
    other.dayChangePercentage == dayChangePercentage &&
    other.dayChangeAmount == dayChangeAmount &&
    other.totalChangePercentage == totalChangePercentage &&
    other.totalChangeAmount == totalChangeAmount;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (symbol == null ? 0 : symbol!.hashCode) +
    (name == null ? 0 : name!.hashCode) +
    (value == null ? 0 : value!.hashCode) +
    (percentage == null ? 0 : percentage!.hashCode) +
    (portfolioPercentage == null ? 0 : portfolioPercentage!.hashCode) +
    (dayChangePercentage == null ? 0 : dayChangePercentage!.hashCode) +
    (dayChangeAmount == null ? 0 : dayChangeAmount!.hashCode) +
    (totalChangePercentage == null ? 0 : totalChangePercentage!.hashCode) +
    (totalChangeAmount == null ? 0 : totalChangeAmount!.hashCode);

  @override
  String toString() => 'AllocationHolding[symbol=$symbol, name=$name, value=$value, percentage=$percentage, portfolioPercentage=$portfolioPercentage, dayChangePercentage=$dayChangePercentage, dayChangeAmount=$dayChangeAmount, totalChangePercentage=$totalChangePercentage, totalChangeAmount=$totalChangeAmount]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.symbol != null) {
      json[r'symbol'] = this.symbol;
    } else {
      json[r'symbol'] = null;
    }
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
    if (this.portfolioPercentage != null) {
      json[r'portfolioPercentage'] = this.portfolioPercentage;
    } else {
      json[r'portfolioPercentage'] = null;
    }
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

  /// Returns a new [AllocationHolding] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static AllocationHolding? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "AllocationHolding[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "AllocationHolding[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return AllocationHolding(
        symbol: mapValueOfType<String>(json, r'symbol'),
        name: mapValueOfType<String>(json, r'name'),
        value: num.parse('${json[r'value']}'),
        percentage: mapValueOfType<double>(json, r'percentage'),
        portfolioPercentage: mapValueOfType<double>(json, r'portfolioPercentage'),
        dayChangePercentage: mapValueOfType<double>(json, r'dayChangePercentage'),
        dayChangeAmount: num.parse('${json[r'dayChangeAmount']}'),
        totalChangePercentage: mapValueOfType<double>(json, r'totalChangePercentage'),
        totalChangeAmount: num.parse('${json[r'totalChangeAmount']}'),
      );
    }
    return null;
  }

  static List<AllocationHolding> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <AllocationHolding>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = AllocationHolding.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, AllocationHolding> mapFromJson(dynamic json) {
    final map = <String, AllocationHolding>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = AllocationHolding.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of AllocationHolding-objects as value to a dart map
  static Map<String, List<AllocationHolding>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<AllocationHolding>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = AllocationHolding.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

