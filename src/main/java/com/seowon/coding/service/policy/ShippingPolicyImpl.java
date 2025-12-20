package com.seowon.coding.service.policy;

import com.seowon.coding.domain.model.Order;
import com.seowon.coding.domain.model.ShippingPolicy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ShippingPolicyImpl implements ShippingPolicy {

    private static final BigDecimal FREE_THRESHOLD = new BigDecimal("100.00");
    private static final BigDecimal DEFAULT_FEE = new BigDecimal("5.00");

    @Override
    public BigDecimal calculateShipping(Order order) {
        return order.getTotalAmount().compareTo(FREE_THRESHOLD) >= 0 ? BigDecimal.ZERO : DEFAULT_FEE;
    }
}
