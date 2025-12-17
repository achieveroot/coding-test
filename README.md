# 백엔드개발 Coding Test

## Overview

- 이 프로젝트는 Product 와 Order 가 있는 간단한 Spring Boot 응용 프로그램입니다. 
- 과제는 기능 구현, 리팩토링, 코드 리뷰로 구성되어 있으며 가능한 한 많이 완료해 주세요.

## 과제

코드베이스에는 구현/리뷰해야 하는 `TODO`가 있습니다. 각 TODO는 코드의 주석으로 표시되어 있습니다.

### TODO List

1. `ProductService`에서 카테고리별 제품 조회 메소드 구현
2. `OrderController`에 주문 생성 API 구현
3. `OrderService`에 주문 생성 로직(`placeOrder`) 구현
4. 리팩토링: `OrderService#checkoutOrderBad`에 모인 도메인 로직을 도메인으로 이동 (Repository 의존은 외부로 유지)
5. 코드 리뷰(트랜잭션): 장시간 작업 `bulkShipOrdersParent`와 진행률 저장
6. 리팩토링(가격/기준정보): `ProductService#applyBulkPriceChangeBad` 개선 (금액 타입/정책/일괄 처리 등)
7. 추가: 가능한 한 많은 테스트 통과 및 작성/보완
