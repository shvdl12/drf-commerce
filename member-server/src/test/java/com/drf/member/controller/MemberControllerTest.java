package com.drf.member.controller;

import com.drf.member.model.request.MemberSignUpRequest;
import com.drf.member.service.MemberService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = MemberController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MemberService memberService;

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
