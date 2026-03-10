package com.drf.member.model.request;

import com.drf.member.common.validation.annotation.ValidPassword;
import com.drf.member.common.validation.annotation.ValidPhone;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberSignUpRequest {

    @NotBlank
    @Email
    @Size(max = 100)
    private String email;

    @NotBlank
    @ValidPassword
    private String password;

    @NotBlank
    @Size(min = 2, max = 50)
    private String name;

    @NotBlank
    @ValidPhone
    private String phone;

    @NotNull
    @Past
    private LocalDate birthDate;
}
