//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class DashboardSummary {
  /// Returns a new [DashboardSummary] instance.
  DashboardSummary({
    this.totalValue,
    this.totalInvested,
    this.totalGainLoss,
    this.totalGainLossPercentage,
    this.dayChange,
    this.dayChangePercentage,
    this.totalPortfolios,
    this.totalHoldings,
    this.portfolioBreakdown = const [],
    this.bestPerformer,
    this.worstPerformer,
    this.currency,
    this.complete,
  });

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
  num? totalInvested;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  num? totalGainLoss;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  double? totalGainLossPercentage;

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

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? totalPortfolios;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? totalHoldings;

  List<PortfolioBreakdown> portfolioBreakdown;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  PerformerItem? bestPerformer;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  PerformerItem? worstPerformer;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? currency;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  bool? complete;

  @override
  bool operator ==(Object other) => identical(this, other) || other is DashboardSummary &&
    other.totalValue == totalValue &&
    other.totalInvested == totalInvested &&
    other.totalGainLoss == totalGainLoss &&
    other.totalGainLossPercentage == totalGainLossPercentage &&
    other.dayChange == dayChange &&
    other.dayChangePercentage == dayChangePercentage &&
    other.totalPortfolios == totalPortfolios &&
    other.totalHoldings == totalHoldings &&
    _deepEquality.equals(other.portfolioBreakdown, portfolioBreakdown) &&
    other.bestPerformer == bestPerformer &&
    other.worstPerformer == worstPerformer &&
    other.currency == currency &&
    other.complete == complete;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (totalValue == null ? 0 : totalValue!.hashCode) +
    (totalInvested == null ? 0 : totalInvested!.hashCode) +
    (totalGainLoss == null ? 0 : totalGainLoss!.hashCode) +
    (totalGainLossPercentage == null ? 0 : totalGainLossPercentage!.hashCode) +
    (dayChange == null ? 0 : dayChange!.hashCode) +
    (dayChangePercentage == null ? 0 : dayChangePercentage!.hashCode) +
    (totalPortfolios == null ? 0 : totalPortfolios!.hashCode) +
    (totalHoldings == null ? 0 : totalHoldings!.hashCode) +
    (portfolioBreakdown.hashCode) +
    (bestPerformer == null ? 0 : bestPerformer!.hashCode) +
    (worstPerformer == null ? 0 : worstPerformer!.hashCode) +
    (currency == null ? 0 : currency!.hashCode) +
    (complete == null ? 0 : complete!.hashCode);

  @override
  String toString() => 'DashboardSummary[totalValue=$totalValue, totalInvested=$totalInvested, totalGainLoss=$totalGainLoss, totalGainLossPercentage=$totalGainLossPercentage, dayChange=$dayChange, dayChangePercentage=$dayChangePercentage, totalPortfolios=$totalPortfolios, totalHoldings=$totalHoldings, portfolioBreakdown=$portfolioBreakdown, bestPerformer=$bestPerformer, worstPerformer=$worstPerformer, currency=$currency, complete=$complete]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.totalValue != null) {
      json[r'totalValue'] = this.totalValue;
    } else {
      json[r'totalValue'] = null;
    }
    if (this.totalInvested != null) {
      json[r'totalInvested'] = this.totalInvested;
    } else {
      json[r'totalInvested'] = null;
    }
    if (this.totalGainLoss != null) {
      json[r'totalGainLoss'] = this.totalGainLoss;
    } else {
      json[r'totalGainLoss'] = null;
    }
    if (this.totalGainLossPercentage != null) {
      json[r'totalGainLossPercentage'] = this.totalGainLossPercentage;
    } else {
      json[r'totalGainLossPercentage'] = null;
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
    if (this.totalPortfolios != null) {
      json[r'totalPortfolios'] = this.totalPortfolios;
    } else {
      json[r'totalPortfolios'] = null;
    }
    if (this.totalHoldings != null) {
      json[r'totalHoldings'] = this.totalHoldings;
    } else {
      json[r'totalHoldings'] = null;
    }
      json[r'portfolioBreakdown'] = this.portfolioBreakdown;
    if (this.bestPerformer != null) {
      json[r'bestPerformer'] = this.bestPerformer;
    } else {
      json[r'bestPerformer'] = null;
    }
    if (this.worstPerformer != null) {
      json[r'worstPerformer'] = this.worstPerformer;
    } else {
      json[r'worstPerformer'] = null;
    }
    if (this.currency != null) {
      json[r'currency'] = this.currency;
    } else {
      json[r'currency'] = null;
    }
    if (this.complete != null) {
      json[r'complete'] = this.complete;
    } else {
      json[r'complete'] = null;
    }
    return json;
  }

  /// Returns a new [DashboardSummary] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static DashboardSummary? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "DashboardSummary[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "DashboardSummary[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return DashboardSummary(
        totalValue: num.parse('${json[r'totalValue']}'),
        totalInvested: num.parse('${json[r'totalInvested']}'),
        totalGainLoss: num.parse('${json[r'totalGainLoss']}'),
        totalGainLossPercentage: mapValueOfType<double>(json, r'totalGainLossPercentage'),
        dayChange: num.parse('${json[r'dayChange']}'),
        dayChangePercentage: mapValueOfType<double>(json, r'dayChangePercentage'),
        totalPortfolios: mapValueOfType<int>(json, r'totalPortfolios'),
        totalHoldings: mapValueOfType<int>(json, r'totalHoldings'),
        portfolioBreakdown: PortfolioBreakdown.listFromJson(json[r'portfolioBreakdown']),
        bestPerformer: PerformerItem.fromJson(json[r'bestPerformer']),
        worstPerformer: PerformerItem.fromJson(json[r'worstPerformer']),
        currency: mapValueOfType<String>(json, r'currency'),
        complete: mapValueOfType<bool>(json, r'complete'),
      );
    }
    return null;
  }

  static List<DashboardSummary> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <DashboardSummary>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = DashboardSummary.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, DashboardSummary> mapFromJson(dynamic json) {
    final map = <String, DashboardSummary>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = DashboardSummary.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of DashboardSummary-objects as value to a dart map
  static Map<String, List<DashboardSummary>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<DashboardSummary>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = DashboardSummary.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

