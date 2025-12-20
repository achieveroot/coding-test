package com.seowon.coding.service;

import com.seowon.coding.domain.model.Product;
import com.seowon.coding.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {
    private static final BigDecimal VAT = new BigDecimal("1.10");

    private final ProductRepository productRepository;


    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    public Product updateProduct(Long id, Product product) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found with id: " + id);
        }
        product.setId(id);
        return productRepository.save(product);
    }

    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Product> findProductsByCategory(String category) {
        // TODO #1: 구현 항목
        // Repository를 사용하여 category 로 찾을 제품목록 제공
        return productRepository.findByCategory(category);
    }

    /**
     * TODO #6 (리펙토링 – Pricing/RefData): 대량 가격 변경 로직을 도메인 친화적으로 리팩토링하세요.
     */
    public void applyBulkPriceChangeBad(List<Long> productIds, BigDecimal percentage, boolean includeTax) {
        if (productIds == null || productIds.isEmpty()) {
            throw new IllegalArgumentException("empty productIds");
        }

        for (Long id : productIds) {
            Product p = productRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));

            BigDecimal base = p.getPrice() == null ? BigDecimal.ZERO : p.getPrice();
            BigDecimal rate = percentage.movePointLeft(2);
            BigDecimal changed = base.multiply(BigDecimal.ONE.add(rate));

            if (includeTax) {
                changed = changed.multiply(VAT); // 하드코딩 VAT 10%, 지역/카테고리별 규칙 미반영
            }

            p.changePrice(changed);
        }
    }
}
