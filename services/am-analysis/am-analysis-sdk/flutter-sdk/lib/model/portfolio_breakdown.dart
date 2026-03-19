//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class PortfolioBreakdown {
  /// Returns a new [PortfolioBreakdown] instance.
  PortfolioBreakdown({
    this.portfolioId,
    this.portfolioName,
    this.portfolioType,
    this.currentValue,
    this.investedValue,
    this.gainLoss,
    this.gainLossPercent,
    this.dayChange,
    this.dayChangePercent,
    this.holdingCount,
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
  String? portfolioName;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? portfolioType;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  num? currentValue;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  num? investedValue;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  num? gainLoss;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  double? gainLossPercent;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  num? dayChange;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  double? dayChangePercent;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? holdingCount;

  @override
  bool operator ==(Object other) => identical(this, other) || other is PortfolioBreakdown &&
    other.portfolioId == portfolioId &&
    other.portfolioName == portfolioName &&
    other.portfolioType == portfolioType &&
    other.currentValue == currentValue &&
    other.investedValue == investedValue &&
    other.gainLoss == gainLoss &&
    other.gainLossPercent == gainLossPercent &&
    other.dayChange == dayChange &&
    other.dayChangePercent == dayChangePercent &&
    other.holdingCount == holdingCount;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (portfolioId == null ? 0 : portfolioId!.hashCode) +
    (portfolioName == null ? 0 : portfolioName!.hashCode) +
    (portfolioType == null ? 0 : portfolioType!.hashCode) +
    (currentValue == null ? 0 : currentValue!.hashCode) +
    (investedValue == null ? 0 : investedValue!.hashCode) +
    (gainLoss == null ? 0 : gainLoss!.hashCode) +
    (gainLossPercent == null ? 0 : gainLossPercent!.hashCode) +
    (dayChange == null ? 0 : dayChange!.hashCode) +
    (dayChangePercent == null ? 0 : dayChangePercent!.hashCode) +
    (holdingCount == null ? 0 : holdingCount!.hashCode);

  @override
  String toString() => 'PortfolioBreakdown[portfolioId=$portfolioId, portfolioName=$portfolioName, portfolioType=$portfolioType, currentValue=$currentValue, investedValue=$investedValue, gainLoss=$gainLoss, gainLossPercent=$gainLossPercent, dayChange=$dayChange, dayChangePercent=$dayChangePercent, holdingCount=$holdingCount]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.portfolioId != null) {
      json[r'portfolioId'] = this.portfolioId;
    } else {
      json[r'portfolioId'] = null;
    }
    if (this.portfolioName != null) {
      json[r'portfolioName'] = this.portfolioName;
    } else {
      json[r'portfolioName'] = null;
    }
    if (this.portfolioType != null) {
      json[r'portfolioType'] = this.portfolioType;
    } else {
      json[r'portfolioType'] = null;
    }
    if (this.currentValue != null) {
      json[r'currentValue'] = this.currentValue;
    } else {
      json[r'currentValue'] = null;
    }
    if (this.investedValue != null) {
      json[r'investedValue'] = this.investedValue;
    } else {
      json[r'investedValue'] = null;
    }
    if (this.gainLoss != null) {
      json[r'gainLoss'] = this.gainLoss;
    } else {
      json[r'gainLoss'] = null;
    }
    if (this.gainLossPercent != null) {
      json[r'gainLossPercent'] = this.gainLossPercent;
    } else {
      json[r'gainLossPercent'] = null;
    }
    if (this.dayChange != null) {
      json[r'dayChange'] = this.dayChange;
    } else {
      json[r'dayChange'] = null;
    }
    if (this.dayChangePercent != null) {
      json[r'dayChangePercent'] = this.dayChangePercent;
    } else {
      json[r'dayChangePercent'] = null;
    }
    if (this.holdingCount != null) {
      json[r'holdingCount'] = this.holdingCount;
    } else {
      json[r'holdingCount'] = null;
    }
    return json;
  }

  /// Returns a new [PortfolioBreakdown] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static PortfolioBreakdown? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "PortfolioBreakdown[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "PortfolioBreakdown[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return PortfolioBreakdown(
        portfolioId: mapValueOfType<String>(json, r'portfolioId'),
        portfolioName: mapValueOfType<String>(json, r'portfolioName'),
        portfolioType: mapValueOfType<String>(json, r'portfolioType'),
        currentValue: num.parse('${json[r'currentValue']}'),
        investedValue: num.parse('${json[r'investedValue']}'),
        gainLoss: num.parse('${json[r'gainLoss']}'),
        gainLossPercent: mapValueOfType<double>(json, r'gainLossPercent'),
        dayChange: num.parse('${json[r'dayChange']}'),
        dayChangePercent: mapValueOfType<double>(json, r'dayChangePercent'),
        holdingCount: mapValueOfType<int>(json, r'holdingCount'),
      );
    }
    return null;
  }

  static List<PortfolioBreakdown> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <PortfolioBreakdown>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = PortfolioBreakdown.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, PortfolioBreakdown> mapFromJson(dynamic json) {
    final map = <String, PortfolioBreakdown>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = PortfolioBreakdown.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of PortfolioBreakdown-objects as value to a dart map
  static Map<String, List<PortfolioBreakdown>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<PortfolioBreakdown>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = PortfolioBreakdown.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

