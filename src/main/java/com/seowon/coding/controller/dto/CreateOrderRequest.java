package com.seowon.coding.controller.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateOrderRequest {
    private String customerName;
    private String customerEmail;
    private List<CreateOrderProductRequest> products;
}
