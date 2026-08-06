package com.example.labmanagement.registration.dto;

import java.util.List;

public record ApprovalPreviewResponse(boolean canApprove, List<ApprovalWarningResponse> warnings) {
}
