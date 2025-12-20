package com.seowon.coding.domain.model;

import java.math.BigDecimal;

public interface DiscountPolicy {
    BigDecimal calculateDiscount(String couponCode);
}
