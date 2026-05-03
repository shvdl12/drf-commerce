package com.drf.member.service;

import com.drf.common.exception.BusinessException;
import com.drf.common.model.AuthInfo;
import com.drf.member.common.exception.ErrorCode;
import com.drf.member.entitiy.Member;
import com.drf.member.entitiy.MemberStatus;
import com.drf.member.entitiy.WithdrawnMemberHistory;
import com.drf.member.event.internal.MemberSignUpEvent;
import com.drf.member.model.request.MemberSignUpRequest;
import com.drf.member.model.request.PasswordUpdateRequest;
import com.drf.member.model.request.ProfileUpdateRequest;
import com.drf.member.model.response.MemberProfileResponse;
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


    @Transactional(readOnly = true)
    public void validateSignUp(MemberSignUpRequest request) {
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        if (withdrawnMemberHistoryRepository.existsByEmailAndRejoinAllowedAtAfter(request.getEmail(), LocalDate.now())) {
            throw new BusinessException(ErrorCode.REJOIN_NOT_ALLOWED);
        }
    }

    @Transactional
    public Long saveMember(MemberSignUpRequest request) {
        Member member = Member.create(request.getEmail(), passwordEncoder.encode(request.getPassword()),
                request.getName(), request.getPhone(), request.getBirthDate());

        try {
            Member savedMember = memberRepository.save(member);
            eventPublisher.publishEvent(new MemberSignUpEvent(savedMember.getId()));
            return savedMember.getId();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
    }

    @Transactional(readOnly = true)
    public MemberProfileResponse getMemberProfile(AuthInfo authInfo) {
        Member member = memberRepository.findById(authInfo.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return MemberProfileResponse.from(member);
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

    @Transactional
    public void withdrawMember(AuthInfo authInfo) {
        Member member = memberRepository.findById(authInfo.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            throw new BusinessException(ErrorCode.CANNOT_WITHDRAW);
        }

        WithdrawnMemberHistory history = WithdrawnMemberHistory.create(member.getEmail());
        withdrawnMemberHistoryRepository.save(history);

        member.withdraw();
    }
}
