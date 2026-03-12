package com.drf.member.service;

import com.drf.member.common.exception.BusinessException;
import com.drf.member.common.exception.ErrorCode;
import com.drf.member.common.model.AuthInfo;
import com.drf.member.entitiy.Member;
import com.drf.member.event.signup.MemberSignUpEvent;
import com.drf.member.model.request.MemberSignUpRequest;
import com.drf.member.model.request.PasswordUpdateRequest;
import com.drf.member.model.request.ProfileUpdateRequest;
import com.drf.member.repository.MemberRepository;
import com.drf.member.repository.WithdrawnMemberHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;


@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final WithdrawnMemberHistoryRepository withdrawnMemberHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;


    @Transactional
    public Long signUp(MemberSignUpRequest request) {
        // 이메일 중복 체크
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        // 탈퇴 재가입 가능 여부 체크
        if (withdrawnMemberHistoryRepository.existsByEmailAndRejoinAllowedAtAfter(request.getEmail(), LocalDate.now())) {
            throw new BusinessException(ErrorCode.REJOIN_NOT_ALLOWED);
        }

        Member member = Member.create(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getName(),
                request.getPhone(),
                request.getBirthDate()
        );

        try {
            Member savedMember = memberRepository.save(member);

            // 이벤트 발행
            MemberSignUpEvent event = new MemberSignUpEvent(savedMember.getId());
            eventPublisher.publishEvent(event);

            return savedMember.getId();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
    }

    @Transactional
    public void updateProfile(ProfileUpdateRequest request, AuthInfo authInfo) {
        Member member = memberRepository.findById(authInfo.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        member.updateProfile(request.getName(), request.getPhone());
    }

    @Transactional
    public void updatePassword(PasswordUpdateRequest request, AuthInfo authInfo) {
        Member member = memberRepository.findById(authInfo.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new BusinessException(ErrorCode.NEW_PASSWORD_MUST_BE_DIFFERENT);
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        member.updatePassword(passwordEncoder.encode(request.getNewPassword()));
    }
}
