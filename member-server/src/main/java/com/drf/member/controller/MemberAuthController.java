package com.drf.member.controller;

import com.drf.member.common.model.CommonResponse;
import com.drf.member.model.request.MemberLoginRequest;
import com.drf.member.model.response.MemberLoginResponse;
import com.drf.member.service.MemberAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MemberAuthController {
    private final MemberAuthService memberAuthService;

    @PostMapping("/members/login")
    public ResponseEntity<CommonResponse<?>> memberLogin(@RequestBody @Valid MemberLoginRequest request) {
        MemberLoginResponse response = memberAuthService.memberLogin(request);
        return ResponseEntity.ok(CommonResponse.success(response));
    }
}
