package com.dmh.accountservice.dto;

import java.math.BigDecimal;

/**
 * Rangos de monto predefinidos para filtrar actividades.
 */
public enum AmountRange {
    RANGE_0_1000(BigDecimal.ZERO, new BigDecimal("1000")),
    RANGE_1000_5000(new BigDecimal("1000"), new BigDecimal("5000")),
    RANGE_5000_20000(new BigDecimal("5000"), new BigDecimal("20000")),
    RANGE_20000_100000(new BigDecimal("20000"), new BigDecimal("100000")),
    RANGE_OVER_100000(new BigDecimal("100000"), null); // null = sin límite superior

    private final BigDecimal min;
    private final BigDecimal max;

    AmountRange(BigDecimal min, BigDecimal max) {
        this.min = min;
        this.max = max;
    }

    public BigDecimal getMin() {
        return min;
    }

    public BigDecimal getMax() {
        return max;
    }

    /**
     * Verifica si un monto está dentro del rango.
     */
    public boolean contains(BigDecimal amount) {
        if (amount == null) {
            return false;
        }
        
        boolean aboveMin = amount.compareTo(min) >= 0;
        boolean belowMax = max == null || amount.compareTo(max) < 0;
        
        return aboveMin && belowMax;
    }
}
