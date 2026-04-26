# AM Analysis Service - Postman Collection

## Overview
This Postman collection provides comprehensive API testing coverage for the AM Analysis Service, which delivers portfolio and basket analytics including allocation, performance tracking, and top movers identification.

## Service Details
- **Service Name**: AM Analysis Service
- **Base URL (Local)**: `http://localhost:8090`
- **Base Path**: `/api/v1/analysis`
- **Authentication**: Bearer Token (JWT)

## Quick Start

### 1. Import Collection
1. Open Postman
2. Click **Import** → **File**
3. Select `AM-Analysis-Service.postman_collection.json`
4. Collection will appear in your workspace

### 2. Configure Variables
Update the collection variables before testing:

| Variable | Default Value | Description |
|----------|---------------|-------------|
| `base_url` | `http://localhost:8090` | Service base URL |
| `auth_token` | `your-jwt-token-here` | JWT authentication token |
| `portfolio_id` | `sample-portfolio-id` | Portfolio ID for testing |
| `basket_id` | `sample-basket-id` | Basket ID for testing |

**To update variables:**
- Right-click collection → **Edit**
- Navigate to **Variables** tab
- Update **Current Value** column

### 3. Get Authentication Token
Before testing, obtain a valid JWT token from the AM Auth service and update the `auth_token` variable.

## API Endpoints

### 1. Allocation Analysis
Retrieve asset allocation breakdowns by sector and asset class.

#### Endpoints
- **GET** `/api/v1/analysis/portfolio/{id}/allocation` - Portfolio allocation
- **GET** `/api/v1/analysis/basket/{id}/allocation` - Basket allocation

#### Response Structure
```json
{
  "portfolioId": "string",
  "sectors": [
    {
      "name": "string",
      "value": "BigDecimal",
      "percentage": "double"
    }
  ],
  "assetClasses": [
    {
      "name": "string",
      "value": "BigDecimal",
      "percentage": "double"
    }
  ]
}
```

### 2. Performance Analysis
Track portfolio and basket performance over various time frames.

#### Endpoints
- **GET** `/api/v1/analysis/portfolio/{id}/performance?timeFrame={timeFrame}` - Portfolio performance
- **GET** `/api/v1/analysis/basket/{id}/performance?timeFrame={timeFrame}` - Basket performance

#### Query Parameters
| Parameter | Required | Default | Options |
|-----------|----------|---------|---------|
| `timeFrame` | No | `1M` | `1D`, `1W`, `1M`, `3M`, `6M`, `1Y`, `YTD`, `ALL` |

#### Response Structure
```json
{
  "portfolioId": "string",
  "timeFrame": "string",
  "totalReturnPercentage": "double",
  "totalReturnValue": "BigDecimal",
  "chartData": [
    {
      "date": "LocalDate",
      "value": "BigDecimal"
    }
  ]
}
```

### 3. Top Movers Analysis
Identify top gainers and losers within portfolios and baskets.

#### Endpoints
- **GET** `/api/v1/analysis/portfolio/{id}/top-movers?timeFrame={timeFrame}` - Portfolio top movers
- **GET** `/api/v1/analysis/basket/{id}/top-movers?timeFrame={timeFrame}` - Basket top movers
- **GET** `/api/v1/analysis/portfolio/top-movers?timeFrame={timeFrame}` - All portfolios top movers
- **GET** `/api/v1/analysis/basket/top-movers?timeFrame={timeFrame}` - All baskets top movers

#### Query Parameters
| Parameter | Required | Default | Options |
|-----------|----------|---------|---------|
| `timeFrame` | No | `1D` | `1D`, `1W`, `1M`, `YTD` |

#### Response Structure
```json
{
  "gainers": [
    {
      "symbol": "string",
      "name": "string",
      "price": "BigDecimal",
      "changePercentage": "double",
      "changeAmount": "BigDecimal"
    }
  ],
  "losers": [
    {
      "symbol": "string",
      "name": "string",
      "price": "BigDecimal",
      "changePercentage": "double",
      "changeAmount": "BigDecimal"
    }
  ]
}
```

### 4. Health Check
Monitor service health and status.

#### Endpoint
- **GET** `/actuator/health` - Spring Boot Actuator health endpoint

## Entity Types

The API supports two entity types for analysis:
- **PORTFOLIO** - Portfolio-level analysis
- **BASKET** - Basket-level analysis

Entity types are specified as path variables and are case-insensitive (e.g., `portfolio`, `PORTFOLIO`, or `Portfolio` all work).

## Authentication

All endpoints (except health check) require a valid JWT token in the Authorization header:

```
Authorization: Bearer <your-jwt-token>
```

The collection is configured to use the `{{auth_token}}` variable automatically.

## Error Responses

### 400 Bad Request
Returned when:
- Invalid entity type is provided
- Invalid time frame parameter

### 401 Unauthorized
Returned when:
- Missing Authorization header
- Invalid or expired JWT token

### 500 Internal Server Error
Returned when:
- Unexpected server errors occur

## Testing Workflow

### Recommended Test Sequence

1. **Verify Service Health**
   - Run `Health Check` to ensure service is running

2. **Test Portfolio Analysis**
   - Update `portfolio_id` variable
   - Run `Get Portfolio Allocation`
   - Run `Get Portfolio Performance (1 Month)`
   - Run `Get Portfolio Top Movers (1 Day)`

3. **Test Basket Analysis**
   - Update `basket_id` variable
   - Run `Get Basket Allocation`
   - Run `Get Basket Performance`
   - Run `Get Basket Top Movers`

4. **Test Category-Level Analysis**
   - Run `Get Category Top Movers (Portfolio)`
   - Run `Get Category Top Movers (Basket)`

5. **Test Different Time Frames**
   - Run performance endpoints with different `timeFrame` values
   - Run top movers endpoints with different `timeFrame` values

## Collection Features

### Pre-request Scripts
- Validates that `auth_token` is configured
- Warns if using default placeholder token

### Test Scripts
- Validates response status is 200
- Checks response time is under 5 seconds
- Verifies response is valid JSON

### Sample Responses
The collection includes sample responses for key endpoints to help understand expected data structures.

## Environment Variables (Optional)

For testing across multiple environments (local, dev, staging), create Postman environments:

1. Click **Environments** → **Create Environment**
2. Add variables:
   - `base_url`
   - `auth_token`
   - `portfolio_id`
   - `basket_id`
3. Select the environment from the dropdown

## Troubleshooting

### Common Issues

#### "Unauthorized" Error
- Ensure `auth_token` variable is set with a valid JWT
- Check token hasn't expired
- Verify token was obtained from the correct auth service

#### "Bad Request" Error
- Verify entity type is either `portfolio` or `basket`
- Check time frame parameter is valid
- Ensure portfolio/basket IDs exist in the system

#### Connection Refused
- Verify service is running on port 8090
- Check `base_url` variable is correct
- Confirm Docker/local environment is properly configured

#### Empty Response / No Data
- Ensure the portfolio/basket has holdings
- Check that market data is available
- Verify the specified time frame has data

## Support

For issues or questions:
- Check service logs at `am-analysis` service console
- Review MongoDB collections for data availability
- Verify Kafka consumers are running for real-time updates

## Version History

- **v1.0** - Initial collection with all endpoints
  - Allocation analysis (portfolio & basket)
  - Performance tracking (multiple time frames)
  - Top movers identification (category & entity level)
  - Health monitoring
