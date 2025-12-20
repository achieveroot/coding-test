package com.seowon.coding.service;

import com.seowon.coding.domain.model.Order;
import com.seowon.coding.domain.model.OrderItem;
import com.seowon.coding.domain.model.ProcessingStatus;
import com.seowon.coding.domain.model.Product;
import com.seowon.coding.domain.repository.OrderRepository;
import com.seowon.coding.domain.repository.ProcessingStatusRepository;
import com.seowon.coding.domain.repository.ProductRepository;
import com.seowon.coding.domain.model.DiscountPolicy;
import com.seowon.coding.domain.model.ShippingPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProcessingStatusRepository processingStatusRepository;
    private final ShippingPolicy shippingPolicy;
    private final DiscountPolicy discountPolicy;

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }


    public Order updateOrder(Long id, Order order) {
        if (!orderRepository.existsById(id)) {
            throw new RuntimeException("Order not found with id: " + id);
        }
        order.setId(id);
        return orderRepository.save(order);
    }

    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new RuntimeException("Order not found with id: " + id);
        }
        orderRepository.deleteById(id);
    }


    public Order placeOrder(String customerName, String customerEmail, List<Long> productIds, List<Integer> quantities) {
        // TODO #3: 구현 항목
        // * 주어진 고객 정보로 새 Order를 생성
        // * 지정된 Product를 주문에 추가
        // * order 의 상태를 PENDING 으로 변경
        // * orderDate 를 현재시간으로 설정
        // * order 를 저장
        // * 각 Product 의 재고를 수정
        // * placeOrder 메소드의 시그니처는 변경하지 않은 채 구현하세요
        Order order = Order.builder()
                .customerName(customerName)
                .customerEmail(customerEmail)
                .status(Order.OrderStatus.PENDING)
                .orderDate(LocalDateTime.now())
                .build();

        List<Product> products = productRepository.findAllById(productIds);
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(product -> product.getId(), product -> product));

        for (int index = 0; index < productIds.size(); index++) {
            Long productId = productIds.get(index);
            Integer quantity = quantities.get(index);

            Product product = productMap.get(productId);
            if (product == null) {
                throw new IllegalArgumentException("Product Not Found: " + productId);
            }

            product.decreaseStock(quantity);

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(quantity)
                    .price(product.getPrice())
                    .build();

            order.addItem(orderItem);
        }

        return orderRepository.save(order);
    }


    /**
     * TODO #4 (리펙토링 – Unified): Service 에 몰린 주문/가격/배송/할인/재고 로직을 도메인으로 이동
     * - 도메인(`Order`,`OrderItem` 혹은 정책/도메인서비스)에서 불변식과 계산을 책임집니다.
     * - Repository 조회는 도메인 밖에서 해결하여 의존을 차단합니다.
     */
    public Order checkoutOrderBad(String customerName,
                                  String customerEmail,
                                  List<Long> productIds,
                                  List<Integer> quantities,
                                  String couponCode) {
        // 의도적으로 Service 에 도메인 로직을 몰아넣은 구현 (리팩토링 대상)
        invalidCheck(productIds, quantities);
        Order order = Order.create(customerName, customerEmail, LocalDateTime.now());

        for (int i = 0; i < productIds.size(); i++) {
            Long pid = productIds.get(i);
            int qty = quantities.get(i);

            // 의도: 중간 Repository 조회로 설계 고민 유도
            Product product = productRepository.findById(pid)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + pid));

            // 재고 차감(리팩토링 대상)
            product.decreaseStock(qty);

            OrderItem item = OrderItem.builder()
                    .product(product)
                    .quantity(qty)
                    .price(product.getPrice()) // 가격 스냅샷
                    .build();
            order.addItem(item);
        }

        // 배송비/할인 규칙(리팩토링 대상)
        order.recalculateTotalAmount(shippingPolicy, discountPolicy, couponCode);
        order.markAsProcessing();

        return orderRepository.save(order);
    }

    private void invalidCheck(List<Long> productIds, List<Integer> quantities) {
        if (productIds == null || quantities == null || productIds.size() != quantities.size()) {
            throw new IllegalArgumentException("products/quantities invalid");
        }
        for (Integer quantity : quantities) {
            if (quantity <= 0) {
                throw new IllegalArgumentException("quantity must be positive: " + quantity);
            }
        }
    }

    /**
     * TODO #5: 코드 리뷰 - 장시간 작업과 진행률 저장의 트랜잭션 분리
     * - 시나리오: 일괄 배송 처리 중 진행률을 저장하여 다른 사용자가 조회 가능해야 함.
     * - 리뷰 포인트: proxy 및 transaction 분리, 예외 전파/롤백 범위, 격리수준/가시성 등
     */
    @Transactional
    public void bulkShipOrdersParent(String jobId, List<Long> orderIds) {
        ProcessingStatus ps = processingStatusRepository.findByJobId(jobId)
                .orElseGet(() -> processingStatusRepository.save(ProcessingStatus.builder().jobId(jobId).build()));
        ps.markRunning(orderIds == null ? 0 : orderIds.size());
        processingStatusRepository.save(ps);

        int processed = 0;
        for (Long orderId : (orderIds == null ? List.<Long>of() : orderIds)) {
            try {
                // 오래 걸리는 작업 이라는 가정 시뮬레이션 (예: 외부 시스템 연동, 대용량 계산 등)
                orderRepository.findById(orderId).ifPresent(o -> o.setStatus(Order.OrderStatus.PROCESSING));
                // 중간 진행률 저장
                this.updateProgressRequiresNew(jobId, ++processed, orderIds.size());
            } catch (Exception e) {
                // REVIEW: 예외를 적절히 전파/로깅하고 중단/계속 정책을 정의해야 합니다
            }
        }
        ps = processingStatusRepository.findByJobId(jobId).orElse(ps);
        ps.markCompleted();
        processingStatusRepository.save(ps);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateProgressRequiresNew(String jobId, int processed, int total) {
        ProcessingStatus ps = processingStatusRepository.findByJobId(jobId)
                .orElseGet(() -> ProcessingStatus.builder().jobId(jobId).build());
        ps.updateProgress(processed, total);
        processingStatusRepository.save(ps);
    }

}