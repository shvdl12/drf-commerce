package com.drf.coupon.event.consumer;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "processed_events",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "event_type"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long eventId;

    @Column(nullable = false, length = 50)
    private String eventType;

    @Column(nullable = false)
    private LocalDateTime processedAt;

    private ProcessedEvent(Long eventId, String eventType) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.processedAt = LocalDateTime.now();
    }

    public static ProcessedEvent of(Long eventId, String eventType) {
        return new ProcessedEvent(eventId, eventType);
    }
}
