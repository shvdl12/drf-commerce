# 주문 서비스 설계 문서

## 1. 개요

### 서비스 구성

- **Order Server** - 주문 오케스트레이션 (checkout, 쿠폰 위임, 주문 생성/관리)
- **Product Server** - 상품 조회, 재고 관리
- **Coupon Server** - 쿠폰 유효성 검증, 할인 금액 계산
- **Member Server** - 회원 정보, 배송지 관리
- **Payment Server** - 결제 수단, 결제 처리

### 핵심 설계 원칙

- 주문 서버가 BFF 역할 담당 (프론트는 주문 서버를 통해 여러 도메인 데이터 조합)
- 배송지, 결제 수단은 프론트가 각 서버 직접 호출
- 쿠폰 계산 책임은 쿠폰 서버, 안분 계산도 쿠폰 서버에서 처리 후 결과 전달

---

## 2. 쿠폰 정책

### 쿠폰 타입

| apply_type | apply_scope | apply_target_id | 의미                              |
|------------|-------------|-----------------|---------------------------------|
| ORDER      | NULL        | NULL            | 전체 주문 기준, 전체 할인                 |
| ORDER      | CATEGORY    | category_id     | 특정 카테고리 금액 기준으로 조건 체크, 전체 주문 할인 |
| PRODUCT    | CATEGORY    | category_id     | 특정 카테고리 상품에만 할인                 |

### 사용 정책

- 주문별(ORDER) 쿠폰: 주문당 1개 적용
- 상품별(PRODUCT) 쿠폰: 여러 상품에 각 1개씩 적용 가능
- 주문별 + 상품별 쿠폰 동시 적용 가능
- 하나의 쿠폰을 여러 장 보유 가능, 장당 상품 1개에 적용
- 무제한 쿠폰(is_unlimited=true): 매 주문 시 재사용 가능, 선점 API에서 RESERVED 처리 제외
- 이미 다른 상품에 적용된 쿠폰도 목록에 노출 (usedOnOtherItem 플래그로 구분)
- 이미 적용된 쿠폰을 다른 상품에 적용 시 기존 상품의 쿠폰 자동 해제

### 계산 정책

- 상품별 할인 먼저 계산 후 주문별 할인 적용
- 주문별 할인은 상품별 금액 비율로 안분 (마지막 상품에 나머지 원단위 처리)
    - 예시: 5,000원짜리 3개, 1,000원 할인 → 333 / 333 / 334원
- 카테고리 쿠폰: 상위 카테고리 쿠폰은 하위 카테고리 상품에도 적용 (category_path 기반 매칭)

### 부분 환불 정책

- 안분된 금액 기준으로 환불
- 중간에 최소 주문 조건 미달 시 쿠폰 할인가 제하고 환불
- 마지막 상품 환불 시 쿠폰 반환

---

## 3. API 설계

### 3-1. 장바구니

#### POST /members/me/cart/items

장바구니에 상품 추가. 이미 담긴 상품이면 수량 합산.

**Request**

```json
{
  "productId": 101,
  "quantity": 2
}
```

**Response**: 200 OK (body 없음)

---

#### PATCH /members/me/cart/items/{cartItemId}

장바구니 상품 수량 변경

**Request**

```json
{
  "quantity": 3
}
```

**Response**: 200 OK (body 없음)

---

#### DELETE /members/me/cart/items/{cartItemId}

장바구니 상품 삭제

**Response**: 200 OK (body 없음)

---

#### GET /members/me/cart

장바구니 전체 조회

**Response**

```json
[
  {
    "cartItemId": 1,
    "productId": 101,
    "productName": "상품A",
    "price": 50000,
    "quantity": 2,
    "subtotal": 100000,
    "status": "ON_SALE",
    "outOfStock": false,
    "insufficientStock": false
  }
]
```

---

### 3-2. Checkout

#### POST /orders/checkout

주문서 최초 진입 시 상품 검증 및 금액 계산

**Request**

```json
[
  {
    "productId": 101,
    "quantity": 2
  }
]
```

**Response**

```json
{
  "availableItems": [
    {
      "productId": 101,
      "name": "상품A",
      "price": 50000,
      "discountedPrice": 45000,
      "quantity": 2,
      "subtotal": 90000
    }
  ],
  "unavailableItems": [
    {
      "productId": 102,
      "name": "상품B",
      "reason": "OUT_OF_STOCK"
    }
  ],
  "itemTotal": 90000,
  "shippingFee": 3000,
  "totalAmount": 93000
}
```

### 3-3. 장바구니 쿠폰

#### POST /orders/coupons/cart/available

사용 가능한 장바구니 쿠폰 목록 조회 + 쿠폰별 할인 금액 계산

**Request**

```json
[
  {
    "cartItemId": 1,
    "productId": 101,
    "price": 45000,
    "quantity": 2,
    "categoryPath": [
      1,
      2,
      3
    ]
  }
]
```

**Response**

```json
{
  "coupons": [
    {
      "memberCouponId": 10,
      "name": "3만원 이상 5천원 할인",
      "discountAmount": 5000,
      "isBest": true,
      "items": [
        {
          "cartItemId": 1,
          "appliedYn": true,
          "discountAmount": 5000
        },
        {
          "cartItemId": 2,
          "appliedYn": false,
          "discountAmount": 0
        }
      ]
    }
  ]
}
```

---

#### POST /orders/coupons/cart/{memberCouponId}

장바구니 쿠폰 선택/변경 시 적용 및 재계산. 조건 미달 시 cart.coupon_id null 처리.

**Request**

```json
[
  {
    "cartItemId": 1,
    "productId": 101,
    "price": 45000,
    "quantity": 2,
    "categoryPath": [
      1,
      2,
      3
    ]
  }
]
```

**Response**

```json
{
  "applicable": true,
  "totalDiscountAmount": 5000,
  "items": [
    {
      "cartItemId": 1,
      "appliedYn": true,
      "discountAmount": 5000
    },
    {
      "cartItemId": 2,
      "appliedYn": false,
      "discountAmount": 0
    }
  ]
}
```

---

### 3-4. 상품 쿠폰

#### POST /orders/coupons/products/{productId}/available

상품별 쿠폰 버튼 클릭 시 사용 가능한 쿠폰 목록 조회

- 주문 서버가 cart_item에서 이미 사용 중인 coupon_id 목록 파악하여 쿠폰 서버에 전달
- 클라이언트가 price, quantity, categoryPath 전달

**Request**

```json
{
  "cartItemId": 1,
  "productId": 101,
  "price": 45000,
  "quantity": 2,
  "categoryPath": [
    1,
    2,
    3
  ]
}
```

**Response**

```json
{
  "coupons": [
    {
      "memberCouponId": 10,
      "name": "스킨케어 20% 할인",
      "discountAmount": 9000,
      "isBest": true,
      "usedOnOtherItem": false
    }
  ]
}
```

---

#### POST /orders/coupons/products/{productId}/{memberCouponId}

상품 쿠폰 선택 시 적용. 동일 쿠폰이 다른 상품에 적용된 경우 자동 해제.

**Request**

```json
{
  "cartItemId": 1,
  "price": 45000,
  "quantity": 2,
  "categoryPath": [
    1,
    2,
    3
  ]
}
```

**Response**

```json
{
  "discountAmount": 9000
}
```

---

## 4. 시퀀스 다이어그램

```
sequenceDiagram
    autonumber
    participant f as Front
    participant o as Order
    participant p as Product
    participant c as Coupon

    note over f: 주문서 최초 진입

    par
        f->>f: GET /payments/methods (결제 API 직접 호출)
    and
        f->>f: GET /members/addresses (회원 API 직접 호출)
    and
        f->>o: POST /orders/checkout
        note over f,o: [{ productId, quantity }]
    end

    o->>p: 상품 목록 배치 조회
    p-->>o: 상품 정보 (이름, 가격, 할인가, 재고, 상태)

    alt 결격 상품 존재
        o-->>f: 200 OK
        note over o,f: availableItems + unavailableItems(reason) + itemTotal + shippingFee + totalAmount
        note over f: 유저가 결격 상품 제거 후 재호출
    else 전체 정상
        o-->>f: 200 OK
        note over o,f: availableItems + itemTotal + shippingFee + totalAmount
    end

    note over f: 장바구니 쿠폰 목록 조회

    f->>o: POST /orders/coupons/cart/available
    note over f,o: [{ cartItemId, productId, price, quantity, categoryPath }]
    o->>c: 사용 가능한 장바구니 쿠폰 목록 조회
    c-->>o: 쿠폰 목록 + 쿠폰별 할인 금액 + 상품별 적용 여부
    o-->>f: 200 OK
    note over o,f: coupons[{ memberCouponId, name, discountAmount, isBest, items }]

    note over f: 장바구니 쿠폰 선택 시

    f->>o: POST /orders/coupons/cart/{memberCouponId}
    note over f,o: [{ cartItemId, productId, price, quantity, categoryPath }]
    o->>c: 쿠폰 재검증 + 재계산
    c-->>o: applicable + 상품별 할인 금액
    o->>o: cart.coupon_id 업데이트 (조건 미달 시 null)
    o-->>f: 200 OK
    note over o,f: { applicable, totalDiscountAmount, items[{ cartItemId, appliedYn, discountAmount }] }

    note over f: 상품 쿠폰 목록 조회 (상품별 쿠폰 버튼 클릭 시)

    f->>o: POST /orders/coupons/products/{productId}/available
    note over f,o: { cartItemId, productId, price, quantity, categoryPath }
    o->>o: cart_item에서 사용 중인 coupon_id 목록 조회
    o->>c: 사용 가능한 상품 쿠폰 목록 조회 (used_coupon_ids 포함)
    c-->>o: 쿠폰 목록 + 쿠폰별 할인 금액 + 타 상품 사용 여부
    o-->>f: 200 OK
    note over o,f: coupons[{ memberCouponId, name, discountAmount, isBest, usedOnOtherItem }]

    note over f: 상품 쿠폰 선택 시

    f->>o: POST /orders/coupons/products/{productId}/{memberCouponId}
    note over f,o: { cartItemId, price, quantity, categoryPath }
    o->>c: 쿠폰 재검증 + 재계산
    c-->>o: applicable + discountAmount
    o->>o: cart_item.coupon_id 업데이트 (기존 적용 상품 자동 해제)
    o-->>f: 200 OK
    note over o,f: { discountAmount }

    note over f: 상품 쿠폰 적용 후 장바구니 쿠폰 재계산 필요 시

    f->>o: POST /orders/coupons/cart/{memberCouponId}
    note over f,o: 상품 쿠폰 적용된 price 반영
    o->>c: 쿠폰 재검증 + 재계산
    c-->>o: applicable + 상품별 할인 금액 (재계산)
    o->>o: cart.coupon_id 업데이트
    o-->>f: 200 OK
```