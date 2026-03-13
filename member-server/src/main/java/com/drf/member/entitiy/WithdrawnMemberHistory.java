package com.drf.member.entitiy;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
        name = "withdrawn_member_history",
        indexes = {
                @Index(name = "idx_withdrawn_email_rejoin", columnList = "email, rejoin_allowed_at")
        }
)
public class WithdrawnMemberHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false)
    private LocalDate withdrawnAt;

    @Column(nullable = false)
    private LocalDate rejoinAllowedAt;

    public static WithdrawnMemberHistory create(String email) {
        LocalDate now = LocalDate.now();
        LocalDate rejoinAllowedAt = now.plusDays(30);
        return WithdrawnMemberHistory.builder()
                .email(email)
                .withdrawnAt(now)
                .rejoinAllowedAt(rejoinAllowedAt)
                .build();
    }
}
