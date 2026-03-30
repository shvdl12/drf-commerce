package com.drf.common.idempotency;

public interface IdempotencyLock {
    boolean acquire(String idempotencyKey, String scope, String token);

    void release(String idempotencyKey, String scope, String token);
}
