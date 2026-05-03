package com.drf.member.facade;

import com.drf.member.model.request.MemberSignUpRequest;
import com.drf.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberFacade {
    private final MemberService memberService;

    public Long signUp(MemberSignUpRequest request) {
        memberService.validateSignUp(request);
        return memberService.saveMember(request);
    }
}
