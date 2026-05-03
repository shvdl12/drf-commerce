package com.drf.member.controller;

import com.drf.common.model.AuthInfo;
import com.drf.common.model.CommonResponse;
import com.drf.member.facade.MemberFacade;
import com.drf.member.model.request.MemberSignUpRequest;
import com.drf.member.model.request.PasswordUpdateRequest;
import com.drf.member.model.request.ProfileUpdateRequest;
import com.drf.member.model.response.MemberProfileResponse;
import com.drf.member.model.response.MemberSignUpResponse;
import com.drf.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/members")
public class MemberController {
    private final MemberFacade memberFacade;
    private final MemberService memberService;

    @PostMapping("/signup")
    public ResponseEntity<CommonResponse<MemberSignUpResponse>> memberSignUp(@RequestBody @Valid MemberSignUpRequest request) {
        Long memberId = memberFacade.signUp(request);
        MemberSignUpResponse response = new MemberSignUpResponse(memberId);

        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @GetMapping("/me")
    public ResponseEntity<CommonResponse<MemberProfileResponse>> getMemberProfile(AuthInfo authInfo) {
        MemberProfileResponse response = memberService.getMemberProfile(authInfo);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @PatchMapping("/me")
    public ResponseEntity<Void> updateProfile(
            @RequestBody @Valid ProfileUpdateRequest request, AuthInfo authInfo) {
        memberService.updateProfile(request, authInfo);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/password")
    public ResponseEntity<Void> updatePassword(
            @RequestBody @Valid PasswordUpdateRequest request, AuthInfo authInfo) {
        memberService.updatePassword(request, authInfo);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/withdraw")
    public ResponseEntity<Void> withdrawMember(AuthInfo authInfo) {
        memberService.withdrawMember(authInfo);
        return ResponseEntity.noContent().build();
    }
}
