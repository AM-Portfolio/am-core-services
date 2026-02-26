

# PortfolioModelV1


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**id** | **UUID** |  |  [optional] |
|**name** | **String** |  |  [optional] |
|**description** | **String** |  |  [optional] |
|**owner** | **String** |  |  [optional] |
|**currency** | **String** |  |  [optional] |
|**fundType** | [**FundTypeEnum**](#FundTypeEnum) |  |  [optional] |
|**status** | **String** |  |  [optional] |
|**tags** | **String** |  |  [optional] |
|**notes** | **String** |  |  [optional] |
|**equityModels** | [**List&lt;EquityModel&gt;**](EquityModel.md) |  |  [optional] |
|**totalValue** | **Double** |  |  [optional] |
|**brokerType** | [**BrokerTypeEnum**](#BrokerTypeEnum) |  |  [optional] |
|**assetCount** | **Integer** |  |  [optional] |
|**createdAt** | **OffsetDateTime** |  |  [optional] |
|**updatedAt** | **OffsetDateTime** |  |  [optional] |
|**createdBy** | **String** |  |  [optional] |
|**updatedBy** | **String** |  |  [optional] |
|**version** | **Long** |  |  [optional] |



## Enum: FundTypeEnum

| Name | Value |
|---- | -----|
| LARGE_CAP_EQUITY_FUND | &quot;Large Cap Equity Fund&quot; |
| MID_CAP_EQUITY_FUND | &quot;Mid Cap Equity Fund&quot; |
| SMALL_CAP_EQUITY_FUND | &quot;Small Cap Equity Fund&quot; |
| MULTI_CAP_EQUITY_FUND | &quot;Multi Cap Equity Fund&quot; |
| LARGE_MID_CAP_EQUITY_FUND | &quot;Large &amp; Mid Cap Equity Fund&quot; |
| DIVIDEND_YIELD_FUND | &quot;Dividend Yield Fund&quot; |
| FOCUSED_EQUITY_FUND | &quot;Focused Equity Fund&quot; |
| CONTRA_FUND | &quot;Contra Fund&quot; |
| VALUE_FUND | &quot;Value Fund&quot; |
| ULTRA_SHORT_DURATION_FUND | &quot;Ultra Short Duration Fund&quot; |
| LOW_DURATION_FUND | &quot;Low Duration Fund&quot; |
| SHORT_DURATION_FUND | &quot;Short Duration Fund&quot; |
| MEDIUM_DURATION_FUND | &quot;Medium Duration Fund&quot; |
| LONG_DURATION_FUND | &quot;Long Duration Fund&quot; |
| DYNAMIC_BOND_FUND | &quot;Dynamic Bond Fund&quot; |
| CORPORATE_BOND_FUND | &quot;Corporate Bond Fund&quot; |
| CREDIT_RISK_FUND | &quot;Credit Risk Fund&quot; |
| BANKING_AND_PSU_FUND | &quot;Banking and PSU Fund&quot; |
| FLOATER_FUND | &quot;Floater Fund&quot; |
| AGGRESSIVE_HYBRID_FUND | &quot;Aggressive Hybrid Fund&quot; |
| BALANCED_HYBRID_FUND | &quot;Balanced Hybrid Fund&quot; |
| CONSERVATIVE_HYBRID_FUND | &quot;Conservative Hybrid Fund&quot; |
| EQUITY_SAVINGS_FUND | &quot;Equity Savings Fund&quot; |
| MULTI_ASSET_ALLOCATION_FUND | &quot;Multi Asset Allocation Fund&quot; |
| NIFTY_50_INDEX_FUND | &quot;Nifty 50 Index Fund&quot; |
| SENSEX_INDEX_FUND | &quot;Sensex Index Fund&quot; |
| NIFTY_NEXT_50_INDEX_FUND | &quot;Nifty Next 50 Index Fund&quot; |
| MIDCAP_INDEX_FUND | &quot;Midcap Index Fund&quot; |
| SMALLCAP_INDEX_FUND | &quot;Smallcap Index Fund&quot; |
| SECTORAL_INDEX_FUND | &quot;Sectoral Index Fund&quot; |
| BANKING_SECTOR_FUND | &quot;Banking Sector Fund&quot; |
| IT_SECTOR_FUND | &quot;IT Sector Fund&quot; |
| PHARMA_SECTOR_FUND | &quot;Pharma Sector Fund&quot; |
| FMCG_SECTOR_FUND | &quot;FMCG Sector Fund&quot; |
| INFRASTRUCTURE_FUND | &quot;Infrastructure Fund&quot; |
| AUTO_SECTOR_FUND | &quot;Auto Sector Fund&quot; |
| POWER_SECTOR_FUND | &quot;Power Sector Fund&quot; |
| METAL_SECTOR_FUND | &quot;Metal Sector Fund&quot; |
| CONSUMPTION_SECTOR_FUND | &quot;Consumption Sector Fund&quot; |
| ESG_FUND | &quot;ESG Fund&quot; |
| MNC_FUND | &quot;MNC Fund&quot; |
| GLOBAL_ADVANTAGE_FUND | &quot;Global Advantage Fund&quot; |
| RETIREMENT_FUND | &quot;Retirement Fund&quot; |
| CHILDREN_S_FUND | &quot;Children&#39;s Fund&quot; |
| PENSION_FUND | &quot;Pension Fund&quot; |
| US_EQUITY_FUND | &quot;US Equity Fund&quot; |
| EMERGING_MARKETS_FUND | &quot;Emerging Markets Fund&quot; |
| GLOBAL_EQUITY_FUND | &quot;Global Equity Fund&quot; |
| EUROPEAN_MARKETS_FUND | &quot;European Markets Fund&quot; |
| JAPANESE_MARKETS_FUND | &quot;Japanese Markets Fund&quot; |
| CHINA_MARKETS_FUND | &quot;China Markets Fund&quot; |
| LIQUID_FUND | &quot;Liquid Fund&quot; |
| OVERNIGHT_FUND | &quot;Overnight Fund&quot; |
| ARBITRAGE_FUND | &quot;Arbitrage Fund&quot; |
| GILT_FUND | &quot;Gilt Fund&quot; |
| GILT_WITH_CONSTANT_MATURITY | &quot;Gilt with Constant Maturity&quot; |
| ELSS_TAX_SAVER | &quot;ELSS Tax Saver&quot; |
| FUND_OF_FUNDS_DOMESTIC | &quot;Fund of Funds - Domestic&quot; |
| FUND_OF_FUNDS_OVERSEAS | &quot;Fund of Funds - Overseas&quot; |
| DEFAULT | &quot;Default&quot; |



## Enum: BrokerTypeEnum

| Name | Value |
|---- | -----|
| DHAN | &quot;DHAN&quot; |
| ZERODHA | &quot;ZERODHA&quot; |
| MSTOCK | &quot;MSTOCK&quot; |
| GROW | &quot;GROW&quot; |
| KOTAK | &quot;KOTAK&quot; |



