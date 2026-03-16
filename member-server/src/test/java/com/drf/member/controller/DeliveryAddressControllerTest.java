package com.drf.member.controller;

import com.drf.member.model.request.DeliveryAddressCreateRequest;
import com.drf.member.service.DeliveryAddressService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = DeliveryAddressController.class)
class DeliveryAddressControllerTest extends BaseControllerTest {

    @MockitoBean
    private DeliveryAddressService deliveryAddressService;

    @Nested
    @DisplayName("배송지 등록")
    class Register {

        @Test
        @DisplayName("배송지 등록 성공")
        void register_success() throws Exception {
            // given
            DeliveryAddressCreateRequest request = new DeliveryAddressCreateRequest(
                    "집", "010-1234-5678", "서울시 강남구", "101호", "12345", false
            );

            // when & then
            mockMvc.perform(post("/members/me/delivery-addresses")
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("이름이 공백이면 400")
        void register_fail_blankName() throws Exception {
            // given
            DeliveryAddressCreateRequest request = new DeliveryAddressCreateRequest(
                    "", "010-1234-5678", "서울시 강남구", "101호", "12345", false
            );

            // when & then
            mockMvc.perform(post("/members/me/delivery-addresses")
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("전화번호가 공백이면 400")
        void register_fail_blankPhone() throws Exception {
            // given
            DeliveryAddressCreateRequest request = new DeliveryAddressCreateRequest(
                    "집", "", "서울시 강남구", "101호", "12345", false
            );

            // when & then
            mockMvc.perform(post("/members/me/delivery-addresses")
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("주소가 공백이면 400")
        void register_fail_blankAddress() throws Exception {
            // given
            DeliveryAddressCreateRequest request = new DeliveryAddressCreateRequest(
                    "집", "010-1234-5678", "", "101호", "12345", false
            );

            // when & then
            mockMvc.perform(post("/members/me/delivery-addresses")
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("우편번호가 공백이면 400")
        void register_fail_blankZipCode() throws Exception {
            // given
            DeliveryAddressCreateRequest request = new DeliveryAddressCreateRequest(
                    "집", "010-1234-5678", "서울시 강남구", "101호", "", false
            );

            // when & then
            mockMvc.perform(post("/members/me/delivery-addresses")
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }
}
