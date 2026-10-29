package com.tcgshop.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {
	String message() default "Mật khẩu phải có từ 8 đến 64 ký tự, gồm chữ hoa, chữ thường và chữ số";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
