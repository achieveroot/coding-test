package com.seowon.coding.service.policy;

import com.seowon.coding.domain.model.DiscountPolicy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DiscountPolicyImpl implements DiscountPolicy {

    private static final BigDecimal SALE_DISCOUNT = new BigDecimal("10.00");

    @Override
    public BigDecimal calculateDiscount(String couponCode) {
        return (couponCode != null && couponCode.startsWith("SALE")) ? SALE_DISCOUNT : BigDecimal.ZERO;
    }
}
