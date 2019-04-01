package com.tango.services;

import com.tango.domain.AggregatedStat;
import com.tango.domain.Stat;

import javax.annotation.Nonnull;

public interface StatCalculatorService {

    void add(@Nonnull Stat statValue);

    @Nonnull
    AggregatedStat getCurrentAggStat();
}
