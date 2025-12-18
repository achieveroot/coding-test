package com.seowon.coding.controller.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.util.Pair;

import java.util.List;

@Getter
@Setter
public class CreateOrderRequest {
    private String customerName;
    private String customerEmail;
    private List<Pair<Long, Integer>> products;
}
