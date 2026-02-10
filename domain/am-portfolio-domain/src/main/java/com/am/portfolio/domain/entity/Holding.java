package com.am.portfolio.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "holdings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Column(name = "stock_symbol", nullable = false)
    private String stockSymbol;

    private String sector;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(name = "avg_price", nullable = false)
    private BigDecimal avgPrice;

    @Column(name = "invested_amount")
    private BigDecimal investedAmount;

    @Column(name = "current_price")
    private BigDecimal currentPrice;

    @Column(name = "current_value")
    private BigDecimal currentValue;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        calculateValues();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        calculateValues();
    }

    private void calculateValues() {
        if (quantity != null && avgPrice != null) {
            this.investedAmount = quantity.multiply(avgPrice);
        }
        if (quantity != null && currentPrice != null) {
            this.currentValue = quantity.multiply(currentPrice);
        }
    }
}
