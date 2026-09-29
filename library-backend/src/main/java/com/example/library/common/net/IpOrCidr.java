package com.example.library.common.net;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 단일 IP 또는 CIDR 형식 검증.
 */
@Documented
@Constraint(validatedBy = IpOrCidr.Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface IpOrCidr {

	String message() default "IP 또는 CIDR 형식이 아닙니다.";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	/** IpPatterns 로 검증한다 */
	class Validator implements ConstraintValidator<IpOrCidr, String> {

		@Override
		public boolean isValid(String value, ConstraintValidatorContext context) {
			return IpPatterns.isValid(value);
		}
	}
}
