package com.drf.member.model.request;

import jakarta.validation.constraints.NotBlank;


public record TokenRefreshRequest(@NotBlank String refreshToken) {
}
