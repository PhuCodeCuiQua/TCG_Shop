package com.tcgshop.validation;

import java.util.Objects;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class FieldMatchValidator implements ConstraintValidator<FieldMatch, Object> {
	private String first;
	private String second;

	@Override
	public void initialize(FieldMatch constraintAnnotation) {
		first = constraintAnnotation.first();
		second = constraintAnnotation.second();
	}

	@Override
	public boolean isValid(Object value, ConstraintValidatorContext context) {
		if (value == null) {
			return true;
		}
		BeanWrapper beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(value);
		return Objects.equals(beanWrapper.getPropertyValue(first), beanWrapper.getPropertyValue(second));
	}
}
