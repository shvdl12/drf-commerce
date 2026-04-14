package com.drf.coupon.event.consumer;

import com.drf.coupon.repository.MemberCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventProcessor {

    private final ProcessedEventRepository processedEventRepository;
    private final MemberCouponRepository memberCouponRepository;

    @Transactional
    public void processPaymentCompleted(long eventId, Long memberCouponId) {
        processedEventRepository.save(ProcessedEvent.of(eventId, "PAYMENT_COMPLETED"));

        if (memberCouponId == null) {
            return;
        }

        int updated = memberCouponRepository.use(memberCouponId, LocalDateTime.now());
        if (updated == 0) {
            log.error("processPaymentCompleted - coupon use failed (not RESERVED or not found). memberCouponId={}", memberCouponId);
        }
    }
}
