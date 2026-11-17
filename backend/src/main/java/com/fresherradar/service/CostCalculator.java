package com.fresherradar.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class CostCalculator {

    private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000);

    private final BigDecimal inputPricePerMillion;
    private final BigDecimal outputPricePerMillion;

    public CostCalculator(@Value("${fresherradar.llm.input-price-per-million}") BigDecimal inputPricePerMillion,
                          @Value("${fresherradar.llm.output-price-per-million}") BigDecimal outputPricePerMillion) {
        this.inputPricePerMillion = inputPricePerMillion;
        this.outputPricePerMillion = outputPricePerMillion;
    }

    public boolean pricesConfigured() {
        return inputPricePerMillion.signum() > 0 || outputPricePerMillion.signum() > 0;
    }

    /** USD cost of one call from the token counts the provider reported. */
    public BigDecimal cost(int inputTokens, int outputTokens) {
        return BigDecimal.valueOf(inputTokens).multiply(inputPricePerMillion).divide(MILLION, 8, RoundingMode.HALF_UP)
                .add(BigDecimal.valueOf(outputTokens).multiply(outputPricePerMillion).divide(MILLION, 8, RoundingMode.HALF_UP));
    }
}
