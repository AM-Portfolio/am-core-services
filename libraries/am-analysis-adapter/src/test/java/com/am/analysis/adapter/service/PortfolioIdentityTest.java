package com.am.analysis.adapter.service;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.adapter.model.AnalysisHolding;
import com.am.analysis.adapter.model.components.HoldingIdentity;
import com.am.analysis.adapter.model.components.InvestmentStats;
import com.am.analysis.adapter.model.components.PerformanceSummary;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PortfolioIdentityTest {

    @Test
    void canonicalizeBroker_mergesGrowAliases() {
        assertEquals("GROWW", PortfolioIdentity.canonicalizeBroker("GROW"));
        assertEquals("GROWW", PortfolioIdentity.canonicalizeBroker("GROWW"));
        assertEquals("GROWW", PortfolioIdentity.canonicalizeBroker("Groww"));
        assertEquals("UPSTOX", PortfolioIdentity.canonicalizeBroker("UPSTOX"));
        assertNull(PortfolioIdentity.canonicalizeBroker(" "));
    }

    @Test
    void keyOf_prefersBrokerOverName() {
        assertEquals("broker:GROWW", PortfolioIdentity.keyOf("GROW", "Ignored"));
        assertEquals("name:groww", PortfolioIdentity.keyOf(null, "Groww"));
        assertNull(PortfolioIdentity.keyOf(null, " "));
    }

    @Test
    void collapseToLatest_keepsGrowwAndUpstoxSeparate() {
        AnalysisEntity groww = entity("g1", "GROWW", "Groww", 100, LocalDateTime.now());
        AnalysisEntity upstox = entity("u1", "UPSTOX", "Upstox", 200, LocalDateTime.now());

        List<AnalysisEntity> collapsed = PortfolioIdentity.collapseToLatest(List.of(groww, upstox));

        assertEquals(2, collapsed.size());
    }

    @Test
    void collapseToLatest_sameGrowwTwice_keepsNewest() {
        LocalDateTime older = LocalDateTime.now().minusHours(1);
        LocalDateTime newer = LocalDateTime.now();
        AnalysisEntity first = entity("g1", "GROW", "Groww", 183.51, older);
        AnalysisEntity second = entity("g2", "GROWW", "Groww", 183.51, newer);

        List<AnalysisEntity> collapsed = PortfolioIdentity.collapseToLatest(List.of(first, second));

        assertEquals(1, collapsed.size());
        assertEquals("g2", collapsed.get(0).getSourceId());
    }

    @Test
    void collapseToLatest_legacyOrphansWithoutBroker_useHoldingsFingerprint() {
        LocalDateTime older = LocalDateTime.now().minusHours(1);
        LocalDateTime newer = LocalDateTime.now();
        AnalysisEntity first = holdingsOnly("a", older, "AONESILVER", 1.0);
        AnalysisEntity second = holdingsOnly("b", newer, "AONESILVER", 1.0);

        List<AnalysisEntity> collapsed = PortfolioIdentity.collapseToLatest(List.of(first, second));

        assertEquals(1, collapsed.size());
        assertEquals("b", collapsed.get(0).getSourceId());
    }

    private static AnalysisEntity entity(String sourceId, String broker, String name, double value,
                                         LocalDateTime updated) {
        return AnalysisEntity.builder()
                .id("PORTFOLIO_" + sourceId)
                .sourceId(sourceId)
                .type(AnalysisEntityType.PORTFOLIO)
                .ownerId("user-1")
                .brokerType(broker)
                .portfolioName(name)
                .performance(PerformanceSummary.builder().totalValue(value).build())
                .lastUpdated(updated)
                .build();
    }

    private static AnalysisEntity holdingsOnly(String sourceId, LocalDateTime updated, String symbol, double qty) {
        return AnalysisEntity.builder()
                .id("PORTFOLIO_" + sourceId)
                .sourceId(sourceId)
                .type(AnalysisEntityType.PORTFOLIO)
                .ownerId("user-1")
                .holdings(List.of(AnalysisHolding.builder()
                        .identity(HoldingIdentity.builder().symbol(symbol).build())
                        .investment(InvestmentStats.builder().quantity(qty).build())
                        .build()))
                .lastUpdated(updated)
                .build();
    }
}
