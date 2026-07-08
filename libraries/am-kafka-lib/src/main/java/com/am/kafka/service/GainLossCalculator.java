package com.am.kafka.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class GainLossCalculator {
    private GainLossCalculator() {
    }

    public static double todayGainLoss(double qty, double currentPrice, double prevClose) {
        return qty * (currentPrice - prevClose);
    }

    public static double totalGainLoss(double qty, double currentPrice, double avgBuy) {
        return qty * (currentPrice - avgBuy);
    }

    public static double gainLossPercent(double gainLoss, double investmentValue) {
        if (investmentValue == 0.0) {
            return 0.0;
        }
        return (gainLoss / investmentValue) * 100.0;
    }

    public static double currentValue(double qty, double price) {
        return qty * price;
    }

    // BigDecimal overloads
    public static BigDecimal todayGainLoss(BigDecimal qty, BigDecimal currentPrice, BigDecimal prevClose) {
        if (qty == null || currentPrice == null || prevClose == null) {
            return BigDecimal.ZERO;
        }
        return qty.multiply(currentPrice.subtract(prevClose));
    }

    public static BigDecimal totalGainLoss(BigDecimal qty, BigDecimal currentPrice, BigDecimal avgBuy) {
        if (qty == null || currentPrice == null || avgBuy == null) {
            return BigDecimal.ZERO;
        }
        return qty.multiply(currentPrice.subtract(avgBuy));
    }

    public static BigDecimal currentValue(BigDecimal qty, BigDecimal price) {
        if (qty == null || price == null) {
            return BigDecimal.ZERO;
        }
        return qty.multiply(price);
    }

    public static double gainLossPercent(BigDecimal gainLoss, BigDecimal investmentValue) {
        if (gainLoss == null || investmentValue == null || investmentValue.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return gainLoss.divide(investmentValue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue();
    }
}
