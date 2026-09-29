package com.luxora.commerce.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.length() < 8 || value.length() > 100) {
            return false;
        }
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSymbol = false;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isUpperCase(current)) {
                hasUpper = true;
            } else if (Character.isLowerCase(current)) {
                hasLower = true;
            } else if (Character.isDigit(current)) {
                hasDigit = true;
            } else {
                hasSymbol = true;
            }
        }
        return hasUpper && hasLower && hasDigit && hasSymbol;
    }
}
