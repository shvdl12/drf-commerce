package com.drf.member.controller;

import com.drf.common.exception.BusinessException;
import com.drf.member.common.exception.ErrorCode;
import com.drf.member.model.request.MemberLoginRequest;
import com.drf.member.model.response.MemberLoginResponse;
import com.drf.member.service.MemberAuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = MemberAuthController.class)
class MemberAuthControllerTest extends BaseControllerTest {

    @MockitoBean
    private MemberAuthService memberAuthService;


    @Nested
    @DisplayName("로그인")
    class Login {
        @Test
        @DisplayName("로그인 성공 시 토큰이 반환된다")
        void memberLogin_success() throws Exception {
            // given
            MemberLoginRequest request = new MemberLoginRequest("test@test.com", "password123!");
            MemberLoginResponse response = new MemberLoginResponse("accessToken", "refreshToken", "Bearer", 1800);
            given(memberAuthService.memberLogin(any(MemberLoginRequest.class))).willReturn(response);

            // when & then
            mockMvc.perform(post("/members/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").value("accessToken"))
                    .andExpect(jsonPath("$.data.refreshToken").value("refreshToken"))
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.data.expiresIn").value(1800));
        }

        @Test
        @DisplayName("이메일 또는 비밀번호 불일치 시 401이 반환된다")
        void memberLogin_invalidCredentials_returns401() throws Exception {
            // given
            MemberLoginRequest request = new MemberLoginRequest("test@test.com", "wrongPassword!");
            willThrow(new BusinessException(ErrorCode.INVALID_CREDENTIALS))
                    .given(memberAuthService).memberLogin(any(MemberLoginRequest.class));

            // when & then
            mockMvc.perform(post("/members/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }

        @Test
        @DisplayName("이메일이 빈 값이면 400이 반환된다")
        void memberLogin_blankEmail_returns400() throws Exception {
            // given
            MemberLoginRequest request = new MemberLoginRequest("", "password123!");

            // when & then
            mockMvc.perform(post("/members/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("비밀번호가 빈 값이면 400이 반환된다")
        void memberLogin_blankPassword_returns400() throws Exception {
            // given
            MemberLoginRequest request = new MemberLoginRequest("test@test.com", "");

            // when & then
            mockMvc.perform(post("/members/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("로그아웃")
    class Logout {

        @Test
        @DisplayName("로그아웃 성공")
        void logout_success() throws Exception {
            // when & then
            mockMvc.perform(post("/members/logout")
                            .header("Authorization", "Bearer validAccessToken")
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "USER"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Authorization 헤더 없으면 401")
        void logout_fail_noAuthorizationHeader() throws Exception {
            // when & then
            mockMvc.perform(post("/members/logout")
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "USER"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Authorization 헤더 유효하지 않으면 401")
        void logout_fail_invalidAuthorizationHeader() throws Exception {
            // when & then
            mockMvc.perform(post("/members/logout")
                            .header("Authorization", "Invalid Token")
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "USER"))
                    .andExpect(status().isUnauthorized());
        }
    }
}