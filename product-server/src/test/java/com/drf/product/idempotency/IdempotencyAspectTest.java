package com.drf.product.idempotency;

import com.drf.common.exception.BusinessException;
import com.drf.common.exception.errorcode.CommonErrorCode;
import com.drf.common.idempotency.*;
import com.drf.common.model.CommonResponse;
import com.drf.common.util.JsonConverter;
import com.drf.product.model.response.StockReserveResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyAspectTest {

    private static final String IDEMPOTENCY_KEY = "550e8400-e29b-41d4-a716-446655440000";
    private static final String SCOPE = "STOCK_RESERVE";
    @Mock
    private IdempotencyStore idempotencyStore;
    @Mock
    private IdempotencyLock idempotencyLock;
    @Mock
    private JsonConverter jsonConverter;
    @InjectMocks
    private IdempotencyAspect idempotencyAspect;
    @Mock
    private ProceedingJoinPoint joinPoint;
    @Mock
    private Idempotent idempotent;

    @BeforeEach
    void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Idempotency-Key", IDEMPOTENCY_KEY);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("Idempotency-Key 헤더가 없으면 BusinessException 발생")
    void missingHeader_throwsBusinessException() {
        // given
        MockHttpServletRequest requestWithoutHeader = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(requestWithoutHeader));

        // when & then
        assertThatThrownBy(() -> idempotencyAspect.checkIdempotency(joinPoint, idempotent))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("완료된 캐시가 있으면 락 없이 바로 반환한다")
    void cacheHit_returnsCachedWithoutLock() throws Throwable {
        // given
        String cachedBody = "{\"code\":\"SUCCESS\"}";
        given(idempotent.scope()).willReturn(SCOPE);
        given(idempotencyStore.findCachedResponse(IDEMPOTENCY_KEY, SCOPE))
                .willReturn(Optional.of(new CachedResponse(200, cachedBody)));
        given(jsonConverter.toJsonNode(cachedBody)).willReturn(mock(JsonNode.class));

        // when
        Object returned = idempotencyAspect.checkIdempotency(joinPoint, idempotent);

        // then
        assertThat(returned).isInstanceOf(ResponseEntity.class);
        then(idempotencyLock).shouldHaveNoInteractions();
        then(joinPoint).should(never()).proceed();
    }

    @Test
    @DisplayName("락 선점 실패 시 IDEMPOTENCY_CONFLICT 예외 발생")
    void lockAcquireFail_throwsConflict() {
        // given
        given(idempotent.scope()).willReturn(SCOPE);
        given(idempotencyStore.findCachedResponse(IDEMPOTENCY_KEY, SCOPE)).willReturn(Optional.empty());
        given(idempotencyLock.acquire(eq(IDEMPOTENCY_KEY), eq(SCOPE), anyString())).willReturn(false);

        // when & then
        assertThatThrownBy(() -> idempotencyAspect.checkIdempotency(joinPoint, idempotent))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.IDEMPOTENCY_CONFLICT);
    }

    @Test
    @DisplayName("캐시 미스 - acquire와 release에 동일한 token이 전달된다")
    void cacheMiss_executesAndSavesAndReleasesLock() throws Throwable {
        // given
        given(idempotent.scope()).willReturn(SCOPE);
        given(idempotencyStore.findCachedResponse(IDEMPOTENCY_KEY, SCOPE)).willReturn(Optional.empty());

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        given(idempotencyLock.acquire(eq(IDEMPOTENCY_KEY), eq(SCOPE), tokenCaptor.capture())).willReturn(true);

        CommonResponse<StockReserveResponse> body = CommonResponse.success(new StockReserveResponse(1L, 90));
        ResponseEntity<CommonResponse<StockReserveResponse>> result = ResponseEntity.ok(body);
        given(joinPoint.proceed()).willReturn(result);
        given(jsonConverter.toJson(body)).willReturn("{\"code\":\"SUCCESS\"}");

        // when
        Object returned = idempotencyAspect.checkIdempotency(joinPoint, idempotent);

        // then
        assertThat(returned).isSameAs(result);
        then(idempotencyStore).should().saveResponse(IDEMPOTENCY_KEY, SCOPE, 200, "{\"code\":\"SUCCESS\"}");
        String capturedToken = tokenCaptor.getValue();
        then(idempotencyLock).should().release(IDEMPOTENCY_KEY, SCOPE, capturedToken);
    }

    @Test
    @DisplayName("실행 중 예외가 발생해도 acquire와 동일한 token으로 락이 해제된다")
    void exceptionDuringExecution_lockAlwaysReleased() throws Throwable {
        // given
        given(idempotent.scope()).willReturn(SCOPE);
        given(idempotencyStore.findCachedResponse(IDEMPOTENCY_KEY, SCOPE)).willReturn(Optional.empty());

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        given(idempotencyLock.acquire(eq(IDEMPOTENCY_KEY), eq(SCOPE), tokenCaptor.capture())).willReturn(true);
        given(joinPoint.proceed()).willAnswer(inv -> {
            throw new RuntimeException("서비스 오류");
        });

        // when & then
        assertThatThrownBy(() -> idempotencyAspect.checkIdempotency(joinPoint, idempotent))
                .isInstanceOf(RuntimeException.class);
        String capturedToken = tokenCaptor.getValue();
        then(idempotencyLock).should().release(IDEMPOTENCY_KEY, SCOPE, capturedToken);
    }
}
