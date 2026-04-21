package com.drf.order.idempotency;

import com.drf.common.idempotency.CachedResponse;
import com.drf.common.idempotency.IdempotencyStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyStoreImpl implements IdempotencyStore {

    private final IdempotencyKeyRepository idempotencyKeyRepository;

    @Override
    public Optional<CachedResponse> findCachedResponse(String idempotencyKey, String scope) {
        return idempotencyKeyRepository.findByIdempotencyKeyAndScope(idempotencyKey, scope)
                .map(entity -> new CachedResponse(entity.getStatusCode(), entity.getResponse()));
    }

    @Override
    @Transactional
    public void saveResponse(String idempotencyKey, String scope, int statusCode, String response) {
        try {
            idempotencyKeyRepository.save(IdempotencyKey.create(idempotencyKey, scope, statusCode, response));
        } catch (DataIntegrityViolationException e) {
            log.error("Failed to save idempotency response", e);
        }
    }
}
