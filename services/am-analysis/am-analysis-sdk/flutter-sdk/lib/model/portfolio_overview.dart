//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class PortfolioOverview {
  /// Returns a new [PortfolioOverview] instance.
  PortfolioOverview({
    this.portfolioId,
    this.portfolioName,
    this.type,
    this.portfolioCount,
    this.holdingCount,
    this.totalValue,
    this.investedValue,
    this.totalReturn,
    this.returnPercentage,
    this.dayChange,
    this.dayChangePercentage,
    this.topSymbols = const [],
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
  String? type;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? portfolioCount;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? holdingCount;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  num? totalValue;

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
  num? totalReturn;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  double? returnPercentage;

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
  double? dayChangePercentage;

  List<String> topSymbols;

  @override
  bool operator ==(Object other) => identical(this, other) || other is PortfolioOverview &&
    other.portfolioId == portfolioId &&
    other.portfolioName == portfolioName &&
    other.type == type &&
    other.portfolioCount == portfolioCount &&
    other.holdingCount == holdingCount &&
    other.totalValue == totalValue &&
    other.investedValue == investedValue &&
    other.totalReturn == totalReturn &&
    other.returnPercentage == returnPercentage &&
    other.dayChange == dayChange &&
    other.dayChangePercentage == dayChangePercentage &&
    _deepEquality.equals(other.topSymbols, topSymbols);

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (portfolioId == null ? 0 : portfolioId!.hashCode) +
    (portfolioName == null ? 0 : portfolioName!.hashCode) +
    (type == null ? 0 : type!.hashCode) +
    (portfolioCount == null ? 0 : portfolioCount!.hashCode) +
    (holdingCount == null ? 0 : holdingCount!.hashCode) +
    (totalValue == null ? 0 : totalValue!.hashCode) +
    (investedValue == null ? 0 : investedValue!.hashCode) +
    (totalReturn == null ? 0 : totalReturn!.hashCode) +
    (returnPercentage == null ? 0 : returnPercentage!.hashCode) +
    (dayChange == null ? 0 : dayChange!.hashCode) +
    (dayChangePercentage == null ? 0 : dayChangePercentage!.hashCode) +
    (topSymbols.hashCode);

  @override
  String toString() => 'PortfolioOverview[portfolioId=$portfolioId, portfolioName=$portfolioName, type=$type, portfolioCount=$portfolioCount, holdingCount=$holdingCount, totalValue=$totalValue, investedValue=$investedValue, totalReturn=$totalReturn, returnPercentage=$returnPercentage, dayChange=$dayChange, dayChangePercentage=$dayChangePercentage, topSymbols=$topSymbols]';

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
    if (this.type != null) {
      json[r'type'] = this.type;
    } else {
      json[r'type'] = null;
    }
    if (this.portfolioCount != null) {
      json[r'portfolioCount'] = this.portfolioCount;
    } else {
      json[r'portfolioCount'] = null;
    }
    if (this.holdingCount != null) {
      json[r'holdingCount'] = this.holdingCount;
    } else {
      json[r'holdingCount'] = null;
    }
    if (this.totalValue != null) {
      json[r'totalValue'] = this.totalValue;
    } else {
      json[r'totalValue'] = null;
    }
    if (this.investedValue != null) {
      json[r'investedValue'] = this.investedValue;
    } else {
      json[r'investedValue'] = null;
    }
    if (this.totalReturn != null) {
      json[r'totalReturn'] = this.totalReturn;
    } else {
      json[r'totalReturn'] = null;
    }
    if (this.returnPercentage != null) {
      json[r'returnPercentage'] = this.returnPercentage;
    } else {
      json[r'returnPercentage'] = null;
    }
    if (this.dayChange != null) {
      json[r'dayChange'] = this.dayChange;
    } else {
      json[r'dayChange'] = null;
    }
    if (this.dayChangePercentage != null) {
      json[r'dayChangePercentage'] = this.dayChangePercentage;
    } else {
      json[r'dayChangePercentage'] = null;
    }
      json[r'topSymbols'] = this.topSymbols;
    return json;
  }

  /// Returns a new [PortfolioOverview] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static PortfolioOverview? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "PortfolioOverview[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "PortfolioOverview[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return PortfolioOverview(
        portfolioId: mapValueOfType<String>(json, r'portfolioId'),
        portfolioName: mapValueOfType<String>(json, r'portfolioName'),
        type: mapValueOfType<String>(json, r'type'),
        portfolioCount: mapValueOfType<int>(json, r'portfolioCount'),
        holdingCount: mapValueOfType<int>(json, r'holdingCount'),
        totalValue: num.parse('${json[r'totalValue']}'),
        investedValue: num.parse('${json[r'investedValue']}'),
        totalReturn: num.parse('${json[r'totalReturn']}'),
        returnPercentage: mapValueOfType<double>(json, r'returnPercentage'),
        dayChange: num.parse('${json[r'dayChange']}'),
        dayChangePercentage: mapValueOfType<double>(json, r'dayChangePercentage'),
        topSymbols: json[r'topSymbols'] is Iterable
            ? (json[r'topSymbols'] as Iterable).cast<String>().toList(growable: false)
            : const [],
      );
    }
    return null;
  }

  static List<PortfolioOverview> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <PortfolioOverview>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = PortfolioOverview.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, PortfolioOverview> mapFromJson(dynamic json) {
    final map = <String, PortfolioOverview>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = PortfolioOverview.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of PortfolioOverview-objects as value to a dart map
  static Map<String, List<PortfolioOverview>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<PortfolioOverview>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = PortfolioOverview.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

