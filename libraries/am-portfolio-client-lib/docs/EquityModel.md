

# EquityModel


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**id** | **UUID** |  |  [optional] |
|**symbol** | **String** |  |  [optional] |
|**name** | **String** |  |  [optional] |
|**description** | **String** |  |  [optional] |
|**assetType** | [**AssetTypeEnum**](#AssetTypeEnum) |  |  [optional] |
|**marketData** | [**MarketDataModel**](MarketDataModel.md) |  |  [optional] |
|**quantity** | **Double** |  |  [optional] |
|**avgBuyingPrice** | **Double** |  |  [optional] |
|**currentValue** | **Double** |  |  [optional] |
|**investmentValue** | **Double** |  |  [optional] |
|**brokerType** | [**BrokerTypeEnum**](#BrokerTypeEnum) |  |  [optional] |
|**profitLoss** | **Double** |  |  [optional] |
|**profitLossPercentage** | **Double** |  |  [optional] |
|**createdAt** | **OffsetDateTime** |  |  [optional] |
|**updatedAt** | **OffsetDateTime** |  |  [optional] |
|**isActive** | **Boolean** |  |  [optional] |
|**isin** | **String** |  |  [optional] |
|**companyName** | **String** |  |  [optional] |
|**sector** | **String** |  |  [optional] |
|**industry** | **String** |  |  [optional] |
|**marketCap** | **String** |  |  [optional] |
|**exchange** | **String** |  |  [optional] |
|**peRatio** | **Double** |  |  [optional] |
|**pbRatio** | **Double** |  |  [optional] |
|**dividendYield** | **Double** |  |  [optional] |
|**eps** | **Double** |  |  [optional] |
|**sharesOutstanding** | **Integer** |  |  [optional] |
|**stockType** | **String** |  |  [optional] |
|**countryOfIncorporation** | **String** |  |  [optional] |



## Enum: AssetTypeEnum

| Name | Value |
|---- | -----|
| EQUITY | &quot;EQUITY&quot; |
| FIXED_INCOME | &quot;FIXED_INCOME&quot; |
| MUTUAL_FUND | &quot;MUTUAL_FUND&quot; |
| ETF | &quot;ETF&quot; |
| COMMODITY | &quot;COMMODITY&quot; |
| NPS | &quot;NPS&quot; |
| REAL_ESTATE | &quot;REAL_ESTATE&quot; |
| CASH | &quot;CASH&quot; |
| CRYPTOCURRENCY | &quot;CRYPTOCURRENCY&quot; |



## Enum: BrokerTypeEnum

| Name | Value |
|---- | -----|
| DHAN | &quot;DHAN&quot; |
| ZERODHA | &quot;ZERODHA&quot; |
| MSTOCK | &quot;MSTOCK&quot; |
| GROW | &quot;GROW&quot; |
| KOTAK | &quot;KOTAK&quot; |



