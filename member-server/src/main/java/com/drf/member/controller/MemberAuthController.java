package com.drf.member.controller;

import com.drf.member.common.model.AuthInfo;
import com.drf.member.common.model.CommonResponse;
import com.drf.member.model.request.MemberLoginRequest;
import com.drf.member.model.response.MemberLoginResponse;
import com.drf.member.service.MemberAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MemberAuthController {
    private final MemberAuthService memberAuthService;

    @PostMapping("/members/login")
    public ResponseEntity<CommonResponse<MemberLoginResponse>> memberLogin(@RequestBody @Valid MemberLoginRequest request) {
        MemberLoginResponse response = memberAuthService.memberLogin(request);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @PostMapping("/members/logout")
    public ResponseEntity<Void> memberLogout(
            @RequestHeader(value = "Authorization", required = false) String bearerToken, AuthInfo authInfo) {

        if (!StringUtils.hasText(bearerToken) || !bearerToken.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String accessToken = bearerToken.substring(7);
        memberAuthService.memberLogout(accessToken, authInfo);
        return ResponseEntity.noContent().build();
    }
}
