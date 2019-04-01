package com.tango.services;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@ConfigurationProperties("stat")
@Validated
public class CalculatorConfig {
    @Min(1)
    @Max(5 * 60)
    private int maxDuration;

    @Min(1)
    private int granularity;

    public CalculatorConfig(int maxDuration, int granularity) {
        this.maxDuration = maxDuration;
        this.granularity = granularity;
    }

    public CalculatorConfig() {
    }

    public int getMaxDuration() {
        return maxDuration;
    }

    public int getGranularity() {
        return granularity;
    }

    public void setMaxDuration(int maxDuration) {
        this.maxDuration = maxDuration;
    }

    public void setGranularity(int granularity) {
        this.granularity = granularity;
    }

    @Override
    public String toString() {
        return "CalculatorConfig{" +
                "maxDuration=" + maxDuration +
                ", granularity=" + granularity +
                '}';
    }
}
