package com.am.portfolio.domain.mapper;

import com.am.portfolio.domain.dto.EquityHoldingDto;
import com.am.portfolio.domain.dto.PortfolioUpdateDto;
import com.am.portfolio.domain.model.EquityModel;
import com.am.portfolio.domain.events.PortfolioUpdateEvent;

import java.util.ArrayList;
import java.util.List;

public class PortfolioGenericMapper {

    public PortfolioUpdateDto mapToDto(PortfolioUpdateEvent event) {
        if (event == null) {
            return null;
        }

        PortfolioUpdateDto dto = PortfolioUpdateDto.builder()
                .userId(event.getUserId())
                .currentValue(event.getTotalValue())
                .investmentValue(event.getTotalInvestment())
                .totalGainLoss(event.getTotalGainLoss())
                .totalGainLossPercentage(event.getTotalGainLossPercentage())
                .todayGainLoss(event.getTodayGainLoss())
                .todayGainLossPercentage(event.getTodayGainLossPercentage())
                .build();

        if (event.getEquities() != null) {
            List<EquityHoldingDto> dtos = new ArrayList<>();
            for (EquityModel model : event.getEquities()) {
                dtos.add(mapEquity(model));
            }
            dto.setEquities(dtos);
        }

        return dto;
    }

    private EquityHoldingDto mapEquity(EquityModel model) {
        return EquityHoldingDto.builder()
                .isin(model.getIsin())
                .symbol(model.getSymbol())
                .quantity(model.getQuantity())
                .currentPrice(model.getCurrentPrice())
                .currentValue(model.getCurrentValue())
                .investmentValue(model.getInvestmentValue())
                .investmentCost(model.getInvestmentValue())
                .profitLoss(model.getProfitLoss())
                .profitLossPercentage(model.getProfitLossPercentage())
                .todayProfitLoss(model.getTodayProfitLoss())
                .todayProfitLossPercentage(model.getTodayProfitLossPercentage())
                .build();
    }
}
