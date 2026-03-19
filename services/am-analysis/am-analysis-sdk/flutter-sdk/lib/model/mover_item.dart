//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class MoverItem {
  /// Returns a new [MoverItem] instance.
  MoverItem({
    this.symbol,
    this.name,
    this.price,
    this.changePercentage,
    this.changeAmount,
    this.sector,
    this.assetClass,
    this.marketCapType,
    this.quantity,
    this.currentValue,
    this.investedValue,
    this.allocationPercentage,
    this.pnlPercentage,
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
  num? price;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  double? changePercentage;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  num? changeAmount;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? sector;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? assetClass;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? marketCapType;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  double? quantity;

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
  double? allocationPercentage;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  double? pnlPercentage;

  @override
  bool operator ==(Object other) => identical(this, other) || other is MoverItem &&
    other.symbol == symbol &&
    other.name == name &&
    other.price == price &&
    other.changePercentage == changePercentage &&
    other.changeAmount == changeAmount &&
    other.sector == sector &&
    other.assetClass == assetClass &&
    other.marketCapType == marketCapType &&
    other.quantity == quantity &&
    other.currentValue == currentValue &&
    other.investedValue == investedValue &&
    other.allocationPercentage == allocationPercentage &&
    other.pnlPercentage == pnlPercentage;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (symbol == null ? 0 : symbol!.hashCode) +
    (name == null ? 0 : name!.hashCode) +
    (price == null ? 0 : price!.hashCode) +
    (changePercentage == null ? 0 : changePercentage!.hashCode) +
    (changeAmount == null ? 0 : changeAmount!.hashCode) +
    (sector == null ? 0 : sector!.hashCode) +
    (assetClass == null ? 0 : assetClass!.hashCode) +
    (marketCapType == null ? 0 : marketCapType!.hashCode) +
    (quantity == null ? 0 : quantity!.hashCode) +
    (currentValue == null ? 0 : currentValue!.hashCode) +
    (investedValue == null ? 0 : investedValue!.hashCode) +
    (allocationPercentage == null ? 0 : allocationPercentage!.hashCode) +
    (pnlPercentage == null ? 0 : pnlPercentage!.hashCode);

  @override
  String toString() => 'MoverItem[symbol=$symbol, name=$name, price=$price, changePercentage=$changePercentage, changeAmount=$changeAmount, sector=$sector, assetClass=$assetClass, marketCapType=$marketCapType, quantity=$quantity, currentValue=$currentValue, investedValue=$investedValue, allocationPercentage=$allocationPercentage, pnlPercentage=$pnlPercentage]';

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
    if (this.price != null) {
      json[r'price'] = this.price;
    } else {
      json[r'price'] = null;
    }
    if (this.changePercentage != null) {
      json[r'changePercentage'] = this.changePercentage;
    } else {
      json[r'changePercentage'] = null;
    }
    if (this.changeAmount != null) {
      json[r'changeAmount'] = this.changeAmount;
    } else {
      json[r'changeAmount'] = null;
    }
    if (this.sector != null) {
      json[r'sector'] = this.sector;
    } else {
      json[r'sector'] = null;
    }
    if (this.assetClass != null) {
      json[r'assetClass'] = this.assetClass;
    } else {
      json[r'assetClass'] = null;
    }
    if (this.marketCapType != null) {
      json[r'marketCapType'] = this.marketCapType;
    } else {
      json[r'marketCapType'] = null;
    }
    if (this.quantity != null) {
      json[r'quantity'] = this.quantity;
    } else {
      json[r'quantity'] = null;
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
    if (this.allocationPercentage != null) {
      json[r'allocationPercentage'] = this.allocationPercentage;
    } else {
      json[r'allocationPercentage'] = null;
    }
    if (this.pnlPercentage != null) {
      json[r'pnlPercentage'] = this.pnlPercentage;
    } else {
      json[r'pnlPercentage'] = null;
    }
    return json;
  }

  /// Returns a new [MoverItem] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static MoverItem? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "MoverItem[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "MoverItem[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return MoverItem(
        symbol: mapValueOfType<String>(json, r'symbol'),
        name: mapValueOfType<String>(json, r'name'),
        price: num.parse('${json[r'price']}'),
        changePercentage: mapValueOfType<double>(json, r'changePercentage'),
        changeAmount: num.parse('${json[r'changeAmount']}'),
        sector: mapValueOfType<String>(json, r'sector'),
        assetClass: mapValueOfType<String>(json, r'assetClass'),
        marketCapType: mapValueOfType<String>(json, r'marketCapType'),
        quantity: mapValueOfType<double>(json, r'quantity'),
        currentValue: num.parse('${json[r'currentValue']}'),
        investedValue: num.parse('${json[r'investedValue']}'),
        allocationPercentage: mapValueOfType<double>(json, r'allocationPercentage'),
        pnlPercentage: mapValueOfType<double>(json, r'pnlPercentage'),
      );
    }
    return null;
  }

  static List<MoverItem> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <MoverItem>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = MoverItem.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, MoverItem> mapFromJson(dynamic json) {
    final map = <String, MoverItem>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = MoverItem.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of MoverItem-objects as value to a dart map
  static Map<String, List<MoverItem>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<MoverItem>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = MoverItem.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

