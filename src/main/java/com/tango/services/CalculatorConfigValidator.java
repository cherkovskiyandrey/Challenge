package com.tango.services;

import com.google.common.collect.ImmutableSet;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class CalculatorConfigValidator implements Validator {

    private final static ImmutableSet<Integer> supportedGranularity = ImmutableSet.<Integer>builder()
            .add(1, 2, 4, 5, 8, 10)
            .build();

    @Override
    public boolean supports(Class<?> clazz) {
        return CalculatorConfig.class == clazz;
    }

    @Override
    public void validate(Object target, Errors errors) {
        CalculatorConfig calculatorConfig = (CalculatorConfig) target;
        if (!supportedGranularity.contains(calculatorConfig.getGranularity())) {
            errors.rejectValue("granularity", "","granularity has to be one of: 1,2,4,5,8");
        }
    }
}
