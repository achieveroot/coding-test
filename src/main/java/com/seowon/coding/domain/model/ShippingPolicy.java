package com.seowon.coding.domain.model;

import java.math.BigDecimal;

public interface ShippingPolicy {
    BigDecimal calculateShipping(Order order);
}
