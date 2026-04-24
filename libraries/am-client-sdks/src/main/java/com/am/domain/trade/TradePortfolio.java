package com.am.domain.trade;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradePortfolio {
    private String id;
    private String name;
    private String userId;
    private String type; // INTRA_DAY, POSITIONAL, etc.
    /**
     * Links this trade portfolio to its master am-portfolio ID. Used for
     * de-duplication in aggregation.
     */
    private String externalPortfolioId;
    private BigDecimal totalValue;
    private BigDecimal totalInvested;
    private BigDecimal currentPnl;
    private Double pnlPercentage;
    private List<TradeHolding> holdings;
}
