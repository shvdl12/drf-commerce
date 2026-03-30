package com.drf.common.idempotency;

import com.drf.common.exception.BusinessException;
import com.drf.common.exception.errorcode.CommonErrorCode;
import com.drf.common.util.JsonConverter;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.UUID;


@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnBean({IdempotencyLock.class, IdempotencyStore.class})
public class IdempotencyAspect {

    private final IdempotencyStore idempotencyStore;
    private final IdempotencyLock idempotencyLock;
    private final JsonConverter jsonConverter;

    @Around("@annotation(idempotent)")
    public Object checkIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();

        // 멱등키 헤더 확인
        String idempotencyKey = request.getHeader("Idempotency-Key");
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new BusinessException(CommonErrorCode.MISSING_IDEMPOTENCY_KEY);
        }
        String scope = idempotent.scope();

        // 완료된 캐시 확인
        Optional<CachedResponse> cached = idempotencyStore.findCachedResponse(idempotencyKey, scope);
        if (cached.isPresent()) {
            return restore(cached.get());
        }

        // 분산 락 선점 시도
        String lockToken = UUID.randomUUID().toString();
        if (!idempotencyLock.acquire(idempotencyKey, scope, lockToken)) {
            throw new BusinessException(CommonErrorCode.IDEMPOTENCY_CONFLICT);
        }

        try {
            // 락 획득 후 재확인: 대기 중 선행 스레드가 완료했을 수 있음
            Optional<CachedResponse> cachedAfterLock = idempotencyStore.findCachedResponse(idempotencyKey, scope);
            if (cachedAfterLock.isPresent()) {
                return restore(cachedAfterLock.get());
            }

            // 실제 비즈니스 로직 수행 및 응답 저장
            Object result = joinPoint.proceed();
            if (result instanceof ResponseEntity<?> responseEntity) {
                idempotencyStore.saveResponse(
                        idempotencyKey, scope,
                        responseEntity.getStatusCode().value(),
                        jsonConverter.toJson(responseEntity.getBody())
                );
            }
            return result;
        } finally {
            idempotencyLock.release(idempotencyKey, scope, lockToken);
        }
    }

    private Object restore(CachedResponse cachedResponse) {
        JsonNode body = jsonConverter.toJsonNode(cachedResponse.body());
        return ResponseEntity.status(cachedResponse.statusCode()).body(body);
    }
}
