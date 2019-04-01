package com.tango;

import com.tango.services.CalculatorConfig;
import com.tango.services.CalculatorConfigValidator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.validation.Validator;

@SpringBootApplication
@EnableConfigurationProperties(CalculatorConfig.class)
public class Application {

    @Bean
    public static Validator configurationPropertiesValidator() {
        return new CalculatorConfigValidator();
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
