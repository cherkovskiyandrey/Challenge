package com.tango.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.validation.constraints.Min;

public class Stat {
    @Min(0)
    private final double amount;
    @Min(0)
    private final long timestamp;

    public Stat(double amount, long timestamp) {
        this.amount = amount;
        this.timestamp = timestamp;
    }

    public double getAmount() {
        return amount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @JsonIgnore
    public long getTimestampInSec() {
        return timestamp / 1000;
    }

    @Override
    public String toString() {
        return "Stat{" +
                "amount=" + amount +
                ", timestamp=" + timestamp +
                '}';
    }
}
