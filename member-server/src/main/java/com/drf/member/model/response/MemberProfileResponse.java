package com.drf.member.model.response;

import com.drf.member.entitiy.Member;
import com.drf.member.entitiy.MemberStatus;

import java.time.LocalDate;

public record MemberProfileResponse(
        Long id,
        String email,
        String name,
        String phone,
        LocalDate birthDate,
        MemberStatus status
) {
    public static MemberProfileResponse of(Member member) {
        return new MemberProfileResponse(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getPhone(),
                member.getBirthDate(),
                member.getStatus()
        );
    }
}