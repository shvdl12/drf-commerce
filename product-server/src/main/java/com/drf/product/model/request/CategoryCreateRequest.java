package com.drf.product.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryCreateRequest(

        Long parentId,

        @NotBlank
        @Size(max = 50)
        String name
) {
}
