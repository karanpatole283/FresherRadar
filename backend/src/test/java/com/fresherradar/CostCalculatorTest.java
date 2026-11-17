package com.fresherradar;

import com.fresherradar.service.CostCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CostCalculatorTest {

    @Test
    void costFollowsTokenCountsAndConfiguredPrices() {
        CostCalculator c = new CostCalculator(new BigDecimal("2"), new BigDecimal("10"));
        // 1,000,000 in * $2/M + 100,000 out * $10/M = $3
        assertEquals(0, new BigDecimal("3").compareTo(c.cost(1_000_000, 100_000)));
        assertTrue(c.pricesConfigured());
    }

    @Test
    void zeroPricesMeanNotConfigured() {
        CostCalculator c = new CostCalculator(BigDecimal.ZERO, BigDecimal.ZERO);
        assertFalse(c.pricesConfigured());
        assertEquals(0, BigDecimal.ZERO.compareTo(c.cost(500, 500)));
    }
}
