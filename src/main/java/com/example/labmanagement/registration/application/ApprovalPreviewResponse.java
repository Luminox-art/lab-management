package com.example.labmanagement.registration.application;

import java.util.List;

public record ApprovalPreviewResponse(boolean canApprove, List<ApprovalWarningResponse> warnings) {
}
