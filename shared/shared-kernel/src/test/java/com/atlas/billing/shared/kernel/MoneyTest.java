package com.atlas.billing.shared.kernel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void addsMoneyWithSameCurrency() {
        Money total = Money.eur("10.00").add(Money.eur("2.50"));

        assertEquals(new BigDecimal("12.5"), total.amount());
        assertEquals("EUR", total.currency().getCurrencyCode());
    }
}
