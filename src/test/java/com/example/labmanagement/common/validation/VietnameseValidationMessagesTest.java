package com.example.labmanagement.common.validation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;

class VietnameseValidationMessagesTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void usesVietnameseDefaultMessages() {
		var violations = validator.validate(new TestRequest(""));

		assertThat(violations).singleElement().extracting("message").isEqualTo("không được để trống");
	}

	record TestRequest(@NotBlank String name) {
	}
}
