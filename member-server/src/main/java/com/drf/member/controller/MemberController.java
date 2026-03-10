package com.drf.member.controller;

import com.drf.member.common.model.CommonResponse;
import com.drf.member.model.request.MemberSignUpRequest;
import com.drf.member.model.response.MemberSignUpResponse;
import com.drf.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    @PostMapping("/members/signup")
    public ResponseEntity<CommonResponse<?>> memberSignUp(@RequestBody @Valid MemberSignUpRequest request) {
        Long memberId = memberService.signUp(request);
        MemberSignUpResponse response = new MemberSignUpResponse(memberId);

        return ResponseEntity.ok(CommonResponse.success(response));
    }
}
