package com.example.labmanagement.registration.application;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ApprovalRequest(@Size(max = 500) List<String> deviceIds, @NotNull @PositiveOrZero Long version) {
}
