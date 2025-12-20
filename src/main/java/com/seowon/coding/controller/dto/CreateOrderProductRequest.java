package com.seowon.coding.controller.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateOrderProductRequest {
    private Long productId;
    private Integer quantity;
}
