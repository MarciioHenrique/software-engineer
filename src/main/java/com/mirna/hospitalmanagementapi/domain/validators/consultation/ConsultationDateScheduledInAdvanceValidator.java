package com.mirna.hospitalmanagementapi.domain.validators.consultation;

import java.time.Duration;
import java.time.LocalDateTime;

import com.mirna.hospitalmanagementapi.domain.validators.consultation.constraints.ConsultationDateScheduledInAdvance;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ConsultationDateScheduledInAdvanceValidator implements ConstraintValidator <ConsultationDateScheduledInAdvance, LocalDateTime> {

	@Override
	public boolean isValid(LocalDateTime value, ConstraintValidatorContext context) {
		LocalDateTime now = LocalDateTime.now();
		
		long diff = Duration.between(now, value).toMinutes();
		
		return diff >= 30L;
	}

}
