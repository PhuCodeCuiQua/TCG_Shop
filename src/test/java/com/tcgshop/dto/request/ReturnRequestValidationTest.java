package com.tcgshop.dto.request;

import static org.junit.jupiter.api.Assertions.*;
import com.tcgshop.entity.enums.ReturnReason;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ReturnRequestValidationTest {
 private static ValidatorFactory factory;
 private static Validator validator;
 @BeforeAll static void init(){factory=Validation.buildDefaultValidatorFactory();validator=factory.getValidator();}
 @AfterAll static void close(){factory.close();}
 @Test void createRequestValidatesDetailBoundariesWhitespaceAndMissingReason(){
   CreateReturnRequest form=new CreateReturnRequest(); form.setReason(ReturnReason.OTHER); form.setDetail("123456789"); assertFalse(validator.validate(form).isEmpty());
   form.setDetail("1234567890"); assertTrue(validator.validate(form).isEmpty());
   form.setDetail("x".repeat(500)); assertTrue(validator.validate(form).isEmpty());
   form.setDetail("x".repeat(501)); assertFalse(validator.validate(form).isEmpty());
   form.setDetail("          "); assertFalse(validator.validate(form).isEmpty());
   form.setDetail("valid detail"); form.setReason(null); assertFalse(validator.validate(form).isEmpty());
 }
 @Test void escalationRequestValidatesDetailBoundariesAndWhitespace(){
   EscalateReturnRequest form=new EscalateReturnRequest(); form.setDetail("123456789"); assertFalse(validator.validate(form).isEmpty());
   form.setDetail("1234567890"); assertTrue(validator.validate(form).isEmpty());
   form.setDetail("x".repeat(500)); assertTrue(validator.validate(form).isEmpty());
   form.setDetail("x".repeat(501)); assertFalse(validator.validate(form).isEmpty());
   form.setDetail("      "); assertFalse(validator.validate(form).isEmpty());
 }
}
