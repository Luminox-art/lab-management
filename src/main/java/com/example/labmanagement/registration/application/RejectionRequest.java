package com.example.labmanagement.registration.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record RejectionRequest(@NotBlank @Size(max = 255) String reason, @NotNull @PositiveOrZero Long version) {
}
