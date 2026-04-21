# 주문 API 구현 계획

`docs/order.md` 설계 문서 기반 구현 계획.  
현재 order-server는 장바구니 + 체크아웃 + 쿠폰(order-checkout.md 범위)까지 완성.  
이 문서는 주문 생성 → 결제 → 이벤트 처리까지의 구현 단계를 정의한다.

---

## 사전 결정 사항

| 항목             | 결정                                                                   |
|----------------|----------------------------------------------------------------------|
| Payment Server | 미존재 → `StubPaymentClient`로 항상 성공 처리                                  |
| 배송지 조회         | member-server에 `/internal` 전용 API 추가 (인증 없음, memberId 경로 파라미터)       |
| 재고 선점/해제       | product-server StockController 경로를 `/internal/stocks/**`로 변경 (인증 제외) |
| 쿠폰 선점          | coupon-server에 이미 존재 → CouponClient에 메서드만 추가                         |

---

## Phase 1: 공통 인프라 (order-server) ✅

`IdempotencyAspect`(common-module)가 `@ConditionalOnBean`으로 조건부 활성화되므로,
order-server에 구현체를 등록하면 `@Idempotent` 어노테이션이 동작한다.

**생성 파일**

- `idempotency/IdempotencyKey.java` — 멱등성 키 엔티티 (DB 저장)
- `idempotency/IdempotencyKeyRepository.java`
- `idempotency/IdempotencyStoreImpl.java` — `IdempotencyStore` 구현
- `idempotency/RedisIdempotencyLock.java` — Redis 분산 락 (`IdempotencyLock` 구현)

참고: `product-server/.../idempotency/` 동일 패턴

---

## Phase 2: 엔티티 & 레포지토리 (order-server) ✅

`docs/order.md § 14` 테이블 설계 기준.

**생성 파일 — Enum**

- `entity/OrderStatus.java` — PENDING, PAID, PARTIAL_CANCELLED, CANCELLED, PAYMENT_FAILED, EXPIRED
- `entity/OrderItemStatus.java` — PENDING, ORDERED, PREPARING, SHIPPING, DELIVERED, CANCELLED, EXPIRED, PAYMENT_FAILED,
  RETURN_REQUESTED, RETURNED
- `entity/PaymentStatus.java` — READY, PAID, FAILED, REFUND_REQUESTED, PARTIAL_REFUNDED, REFUNDED
- `entity/OrderEventType.java` — ORDER_CREATED, ORDER_PARTIALLY_CANCELLED, ORDER_CANCELLED, RETURN_REQUESTED,
  RETURN_COMPLETED
- `entity/PaymentEventType.java` — PAYMENT_COMPLETED, REFUND_REQUESTED, REFUND_FAILED, PARTIAL_REFUNDED, FULL_REFUNDED

**생성 파일 — Entity**

- `entity/Order.java` — `order` 테이블
    - `orderNo` VARCHAR(50) UNIQUE (TSID)
    - `memberId`, `status(OrderStatus)`, `memberCouponId`
    - 금액: `totalAmount`, `deliveryFee`, `productDiscountAmount`, `couponDiscountAmount`, `finalAmount`, `refundedAmount`
    - 배송지 스냅샷: `receiverName`, `receiverPhone`, `zipCode`, `address`, `addressDetail`
- `entity/OrderItem.java` — `order_item` 테이블 (FK: order_id)
    - `orderId`, `productId`, `productName`, `unitPrice`, `discountedPrice`, `quantity`
    - `productCouponDiscountAmount`, `orderCouponDiscountAmount`, `finalAmount`
    - `memberCouponId`, `status(OrderItemStatus)`
- `entity/OrderEvent.java` — `order_event` 테이블 (FK: order_id)
    - `orderId`, `eventType(OrderEventType)`, `reason`
- `entity/OrderEventItem.java` — `order_event_item` 테이블 (FK: order_event_id, order_item_id)
    - `orderEventId`, `orderItemId`, `quantity`, `refundAmount`
- `entity/Payment.java` — `payment` 테이블 (FK: order_id UNIQUE)
    - `orderId`, `amount`, `method`, `status(PaymentStatus)`, `refundedAmount`, `pgTid`, `expiresAt`
- `entity/PaymentEvent.java` — `payment_event` 테이블 (FK: payment_id)
    - `paymentId`, `eventType(PaymentEventType)`, `amount`, `orderEventId`

**생성 파일 — Repository**

- `repository/OrderRepository.java`
- `repository/OrderItemRepository.java` — `findByOrderId(Long)`
- `repository/OrderEventRepository.java`
- `repository/OrderEventItemRepository.java`
- `repository/PaymentRepository.java`
- `repository/PaymentEventRepository.java`

---

## Phase 3: Client 추가 / 수정 ✅

### product-server 수정/신규

- `controller/StockController.java`: reserve/release 메서드 제거
- `controller/InternalStockController.java`: 신규 생성
    - `POST /internal/stocks/{productId}/reserve`
    - `POST /internal/stocks/{productId}/release`
    - RoleCheckInterceptor가 `/internal/**` 제외 → 인증 헤더 불필요
    - `StockService` 동일하게 위임

### member-server 추가

- `controller/InternalDeliveryAddressController.java`: `GET /internal/delivery-addresses/{memberId}/{addressId}`
    - memberId 경로 파라미터로 소유권 검증, 인증 헤더 불필요
- `model/response/InternalDeliveryAddressResponse.java`
- `service/DeliveryAddressService.java` 수정: `findByIdAndMemberId(long memberId, long addressId)` 추가

### order-server 수정/추가

- `client/ProductClient.java` 수정: `reserveStock`, `releaseStock` 추가
  ```
  POST /internal/stocks/{productId}/reserve  (Idempotency-Key 헤더 필수)
  POST /internal/stocks/{productId}/release  (Idempotency-Key 헤더 필수)
  ```
- `client/dto/request/StockReserveRequest.java` — `record { Integer quantity }`
- `client/dto/request/StockReleaseRequest.java` — `record { Integer quantity }`
- `client/CouponClient.java` 수정: `reserveCoupon`, `releaseCoupon` 추가
  ```
  POST   /internal/coupons/{memberCouponId}/reserve
  DELETE /internal/coupons/{memberCouponId}/reserve
  ```
- `client/dto/request/CouponReserveRequest.java` — `record { long orderId }`
- `client/MemberClient.java` 신규 (Feign)
  ```
  GET /internal/delivery-addresses/{memberId}/{addressId}
  ```
- `client/dto/response/DeliveryAddressResponse.java`
- `client/PaymentClient.java` 신규 (인터페이스)
- `client/StubPaymentClient.java` 신규 — 항상 성공, `pgTid = "STUB-" + UUID`
- `resources/application.yml` 수정: `clients.member-server.url` 추가

### 재고/쿠폰 선점·해제 배치 API로 교체 ✅

단건 N번 호출 → 배치 1번 호출로 변경.

**product-server**

- `POST /internal/stocks/{productId}/reserve|release` → `POST /internal/stocks/reserve|release` (리스트 body)
- `StockBatchReserveRequest`, `StockBatchReleaseRequest` 신규
- `StockService.batchReserveStock()` — all-or-nothing (실패 시 서버 내부 보상)
- `StockService.batchReleaseStock()` — best-effort (실패해도 나머지 계속)

**coupon-server**

- `POST|DELETE /internal/coupons/{memberCouponId}/reserve` → `POST|DELETE /internal/coupons/reserve` (리스트 body)
- `InternalCouponBatchReserveRequest` 신규
- `InternalCouponService.batchReserveCoupon()` — `@Transactional` 단일 트랜잭션 all-or-nothing
- `InternalCouponService.batchReleaseCoupon()` — best-effort

**order-server**

- `ProductClient`, `CouponClient` 배치 시그니처로 교체
- `StockBatchReserveRequest`, `StockBatchReleaseRequest`, `CouponBatchReserveRequest` 신규
- 멱등키: `idempotencyKey + STOCK_RESERVE_KEY_SUFFIX` 상수로 관리

---

## Phase 4: Request / Response DTO (order-server) ✅

- `model/request/OrderCreateRequest.java`
  ```java
  record {
      List<Long> cartItemIds,   // @NotEmpty
      Long shippingAddressId,   // @NotNull
      String paymentMethodId,   // @NotBlank
      int expectedAmount        // @Min(0)
  }
  ```
- `model/response/OrderCreateResponse.java`
  ```java
  record { long orderId, String orderNo, String status, int finalAmount }
  ```

---

## Phase 5: ErrorCode 추가 (order-server) ✅

`common/exception/ErrorCode.java` 추가:

```java
ORDER_AMOUNT_MISMATCH(BAD_REQUEST, "주문 금액이 일치하지 않습니다. 주문서를 다시 확인해주세요."),
ORDER_STOCK_INSUFFICIENT(CONFLICT, "재고가 부족하여 주문을 처리할 수 없습니다."),
ORDER_COUPON_UNAVAILABLE(CONFLICT, "쿠폰 선점에 실패하였습니다."),
ORDER_PAYMENT_FAILED(BAD_REQUEST, "결제에 실패하였습니다."),
SHIPPING_ADDRESS_NOT_FOUND(NOT_FOUND, "배송지를 찾을 수 없습니다."),
CART_ITEM_NOT_OWNED(BAD_REQUEST, "유효하지 않은 장바구니 상품이 포함되어 있습니다."),
```

---

## Phase 6: 서비스 레이어 (order-server) ✅

### OrderService — DB 트랜잭션 전담

`service/OrderService.java`

```
createOrder(memberId, items, address, couponId, deliveryFee)  → Order  [status: PENDING, TSID orderNo 발급]
completePayment(orderId, pgTid)                               → void   [Order PAID, Payment PAID, OrderEvent ORDER_CREATED, PaymentEvent PAYMENT_COMPLETED]
failOrder(orderId)                                            → void   [Order PAYMENT_FAILED, OrderItem PAYMENT_FAILED]
```

### OrderFacade — 오케스트레이션 + 보상 트랜잭션

`service/OrderFacade.java`

```
createOrder(AuthInfo, OrderCreateRequest):
  1. cart_item 조회 (cartItemIds 기반) + 소유권 검증 → CART_ITEM_NOT_OWNED
  2. 가격 재검증
     - ProductClient.getProductsBatch() 현재 가격 조회
     - cart coupon / item coupon 할인 재계산 + 배송비
     - expectedAmount 불일치 → ORDER_AMOUNT_MISMATCH
  3. MemberClient.getDeliveryAddress(memberId, shippingAddressId)
     - 실패 → SHIPPING_ADDRESS_NOT_FOUND
  4. OrderService.createOrder() [status: PENDING]
  5. 재고 선점: 각 item ProductClient.reserveStock()
     - 실패 → failOrder() → ORDER_STOCK_INSUFFICIENT
  6. 쿠폰 선점: CouponClient.reserveCoupon() (cart + item 쿠폰)
     - 실패 → 재고 전체 release → failOrder() → ORDER_COUPON_UNAVAILABLE
  7. 결제: PaymentClient.pay()
     - 실패 → 쿠폰 release → 재고 release → failOrder() → ORDER_PAYMENT_FAILED
  8. OrderService.completePayment() [PAID]
  9. Kafka ORDER_PAID 이벤트 발행
```

보상 API 실패 시 로그 기록 후 최대 3회 재시도 (설계문서 §6) → 배치로 교체 후 단일 호출, 실패 시 로그만.  
재고 선점 키: `{idempotencyKey}:RESERVE:{productId}` → `{idempotencyKey}:STOCK_RESERVE` (배치 전환 후 변경).

### OrderFacade 리팩터링 ✅

- `service/` → `facade/` 패키지 분리
- `CartRepository`, `CartItemRepository` 직접 의존 제거 → `CartService.getValidatedCartItems()` 위임
- `OrderLineItem` 도입 (`model/dto/OrderLineItem.java`) — CartItem + 상품정보 + 쿠폰할인액 응집
  - Map 4개(`productMap`, `itemProductCouponDiscounts`, `itemMemberCouponIds`, `itemOrderCouponDiscounts`) 제거
- `createOrder` 단계별 private 메서드 분리: `buildLineItems`, `applyProductCouponDiscounts`, `applyCartCouponDiscount`,
  `calculateAmounts`, `releaseStocks`, `releaseCoupons`

---

## Phase 7: Kafka 이벤트 (order-server) ✅

### 이벤트 발행

`event/OrderPaidEventPayload.java`

```java
record {
    long orderId, long memberId, List<Long > cartItemIds, List < Long > usedMemberCouponIds
}
```

`KafkaProducer.sendMessage(EventTopic.ORDER.getName(), ...)` 재사용 (common-module).

### 이벤트 소비 — 장바구니 정리

`event/OrderPaidEventConsumer.java`

- topic `"order"`, eventType `ORDER_PAID` 수신
- `CartItemRepository.deleteAllById(cartItemIds)` 실행

> product-server(재고 확정 차감), coupon-server(쿠폰 USED 처리)는 각 서버에서 별도 구현.

---

## Phase 8: 컨트롤러 (order-server) ✅

`controller/OrderController.java`

```java

@PostMapping("/orders")
@Idempotent(scope = "ORDER_CREATE")
public ResponseEntity<CommonResponse<OrderCreateResponse>> createOrder(
        AuthInfo authInfo,
        @Valid @RequestBody OrderCreateRequest request) {}
```

---

## Phase 9: 취소 & 반품 API (order-server)

### 취소 API — `POST /orders/{orderId}/cancel`

설계문서 §10 참고.

```java
record OrderCancelRequest { List<Long> orderItemIds, String reason }
```

처리 흐름:

1. 취소 가능 상태 검증 (ORDERED || PREPARING)
2. `OrderItem.status` → CANCELLED
3. `OrderEvent`(ORDER_CANCELLED or ORDER_PARTIALLY_CANCELLED) + `OrderEventItem` 생성
4. `Order.status` → CANCELLED or PARTIAL_CANCELLED
5. `Payment.status` → REFUND_REQUESTED + `PaymentEvent`(REFUND_REQUESTED) 생성

### 반품 요청 API — `POST /orders/{orderId}/returns`

설계문서 §11 참고.

```java
record OrderReturnRequest { List<Long> orderItemIds, String reason }
```

처리 흐름:

1. 반품 가능 상태 검증 (DELIVERED)
2. `OrderItem.status` → RETURN_REQUESTED
3. `OrderEvent`(RETURN_REQUESTED) + `OrderEventItem` 생성
4. `Payment.status` → REFUND_REQUESTED + `PaymentEvent`(REFUND_REQUESTED) 생성

### 반품 완료 (관리자) — `POST /admin/orders/{orderId}/returns/complete`

```java
record OrderReturnCompleteRequest(List<Long> orderItemIds) { }
```

처리 흐름:

1. `OrderItem.status` → RETURNED
2. `OrderEvent`(RETURN_COMPLETED) 생성
3. `PaymentEvent`(PARTIAL_REFUNDED or FULL_REFUNDED) 생성
4. `Payment.status` → PARTIAL_REFUNDED or REFUNDED
