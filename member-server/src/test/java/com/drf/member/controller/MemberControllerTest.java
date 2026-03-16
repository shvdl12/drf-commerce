package com.drf.member.controller;

import com.drf.member.model.request.MemberSignUpRequest;
import com.drf.member.model.request.PasswordUpdateRequest;
import com.drf.member.model.request.ProfileUpdateRequest;
import com.drf.member.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = MemberController.class)
class MemberControllerTest extends BaseControllerTest {

    @MockitoBean
    private MemberService memberService;

    @Nested
    @DisplayName("회원가입")
    class SignUp {

        @Test
        @DisplayName("회원가입 성공")
        void signUp_success() throws Exception {
            // given
            MemberSignUpRequest request = MemberSignUpRequest.builder()
                    .email("test@test.com")
                    .password("Password1!")
                    .name("홍길동")
                    .phone("010-1234-5678")
                    .birthDate(LocalDate.of(2000, 1, 1))
                    .build();

            given(memberService.signUp(any(MemberSignUpRequest.class))).willReturn(1L);

            // when & then
            mockMvc.perform(post("/members/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(1L));
        }
    }

    @Nested
    @DisplayName("프로필 수정")
    class UpdateProfile {

        @Test
        @DisplayName("프로필 수정 성공")
        void updateProfile_success() throws Exception {
            // given
            ProfileUpdateRequest request = new ProfileUpdateRequest("홍길동", "010-9999-8888");

            // when & then
            mockMvc.perform(patch("/members/me")
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("이름이 1자 이하면 400")
        void updateProfile_fail_nameTooShort() throws Exception {
            // given
            ProfileUpdateRequest request = new ProfileUpdateRequest("김", "010-9999-8888");

            // when & then
            mockMvc.perform(patch("/members/me")
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("이름, 전화번호 둘 다 null이면 성공 - 아무것도 수정 안 함")
        void updateProfile_success_nothingChanged() throws Exception {
            // given
            ProfileUpdateRequest request = new ProfileUpdateRequest(null, null);

            // when & then
            mockMvc.perform(patch("/members/me")
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("비밀번호 변경")
    class UpdatePassword {

        @Test
        @DisplayName("비밀번호 변경 성공")
        void updatePassword_success() throws Exception {
            // given
            PasswordUpdateRequest request = new PasswordUpdateRequest("currentPassword1!", "NewPassword1!");

            // when & then
            mockMvc.perform(patch("/members/me/password")
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("현재 비밀번호 공백이면 400")
        void updatePassword_fail_blankCurrentPassword() throws Exception {
            // given
            PasswordUpdateRequest request = new PasswordUpdateRequest("", "NewPassword1!");

            // when & then
            mockMvc.perform(patch("/members/me/password")
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("새 비밀번호가 유효하지 않으면 400")
        void updatePassword_fail_invalidNewPassword() throws Exception {
            // given
            PasswordUpdateRequest request = new PasswordUpdateRequest("currentPassword1!", "invalid");

            // when & then
            mockMvc.perform(patch("/members/me/password")
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }
}