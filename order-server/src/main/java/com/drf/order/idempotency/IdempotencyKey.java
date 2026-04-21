package com.drf.order.idempotency;

import com.drf.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "idempotency_keys",
        uniqueConstraints = @UniqueConstraint(columnNames = {"idempotency_key", "scope"}))
public class IdempotencyKey extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, length = 36)
    private String idempotencyKey;

    @Column(nullable = false, length = 50)
    private String scope;

    @Column(nullable = false)
    private int statusCode;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String response;

    public static IdempotencyKey create(String idempotencyKey, String scope, int statusCode, String response) {
        return IdempotencyKey.builder()
                .idempotencyKey(idempotencyKey)
                .scope(scope)
                .statusCode(statusCode)
                .response(response)
                .build();
    }
}
