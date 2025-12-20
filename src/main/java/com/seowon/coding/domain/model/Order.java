package com.seowon.coding.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders") // "order" is a reserved keyword in SQL
@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String customerName;
    
    private String customerEmail;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    
    private LocalDateTime orderDate;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    private BigDecimal totalAmount;

    public static Order create(String customerName, String customerEmail, LocalDateTime orderDate) {
        if (customerName == null || customerEmail == null) {
            throw new IllegalArgumentException("customer info required");
        }

        return Order.builder()
                .customerName(customerName)
                .customerEmail(customerEmail)
                .status(OrderStatus.PENDING)
                .orderDate(orderDate)
                .totalAmount(BigDecimal.ZERO)
                .build();
    }

    // Business logic
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
        recalculateTotalAmount();
    }
    
    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
        recalculateTotalAmount();
    }
    
    public void recalculateTotalAmount() {
        this.totalAmount = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void recalculateTotalAmount(ShippingPolicy shippingPolicy, DiscountPolicy discountPolicy, String couponCode) {
        BigDecimal shipping = shippingPolicy.calculateShipping(this);
        BigDecimal discount = discountPolicy.calculateDiscount(couponCode);

        this.totalAmount = this.totalAmount.add(shipping).subtract(discount);
    }
    
    public void markAsProcessing() {
        this.status = OrderStatus.PROCESSING;
    }
    
    public void markAsShipped() {
        this.status = OrderStatus.SHIPPED;
    }
    
    public void markAsDelivered() {
        this.status = OrderStatus.DELIVERED;
    }
    
    public void markAsCancelled() {
        this.status = OrderStatus.CANCELLED;
    }

    public enum OrderStatus {
        PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED
    }
}