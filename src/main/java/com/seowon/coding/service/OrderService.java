package com.seowon.coding.service;

import com.seowon.coding.domain.model.Order;
import com.seowon.coding.domain.model.OrderItem;
import com.seowon.coding.domain.model.ProcessingStatus;
import com.seowon.coding.domain.model.Product;
import com.seowon.coding.domain.repository.OrderRepository;
import com.seowon.coding.domain.repository.ProcessingStatusRepository;
import com.seowon.coding.domain.repository.ProductRepository;
import com.seowon.coding.util.ListFun;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
        // Order를 생성할 때 빌더를 사용하지 않고 내부에 정적 팩토리 메서드를 만들고 Objects.requireNonNull을 사용할것 같습니다.
        // customerName, customerEmail을 체크합니다.
        if (customerName == null || customerEmail == null) {
            throw new IllegalArgumentException("customer info required");
        }
        if (productIds == null || quantities == null || productIds.size() != quantities.size()) {
            throw new IllegalArgumentException("products/quantities invalid");
        }

        Order order = Order.builder()
                .customerName(customerName)
                .customerEmail(customerEmail)
                .status(Order.OrderStatus.PENDING)
                .orderDate(LocalDateTime.now())
                .items(new ArrayList<>())
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;
        for (int i = 0; i < productIds.size(); i++) {
            Long pid = productIds.get(i);
            int qty = quantities.get(i);

            // 의도: 중간 Repository 조회로 설계 고민 유도
            Product product = productRepository.findById(pid)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + pid));
            if (qty <= 0) {
                throw new IllegalArgumentException("quantity must be positive: " + qty);
            }
            if (product.getStockQuantity() < qty) {
                throw new IllegalStateException("insufficient stock for product " + pid);
            }

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(qty)
                    .price(product.getPrice()) // 가격 스냅샷
                    .build();
            order.getItems().add(item);

            // 재고 차감(리팩토링 대상)
            product.decreaseStock(qty);

            subtotal = subtotal.add(product.getPrice().multiply(BigDecimal.valueOf(qty)));
        }

        // 배송비/할인 규칙(리팩토링 대상)
        // 배송비, 할인 도메인 서비스를 만들어 오더에 주입해주는 방식으로 리팩토링 할 것 같습니다.
        // 예를 들어 Order에 shipping(BigDecimal subtotal, ShippingService shippingService) 등으로 도메인 서비스를 주입해주어 해결하겠습니다.

        BigDecimal shipping = subtotal.compareTo(new BigDecimal("100.00")) >= 0 ? BigDecimal.ZERO : new BigDecimal("5.00");
        BigDecimal discount = (couponCode != null && couponCode.startsWith("SALE")) ? new BigDecimal("10.00") : BigDecimal.ZERO;

        order.setTotalAmount(subtotal.add(shipping).subtract(discount));
        order.setStatus(Order.OrderStatus.PROCESSING);
        return orderRepository.save(order);
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