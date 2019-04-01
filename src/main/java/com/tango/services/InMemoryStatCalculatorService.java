package com.tango.services;

import com.tango.domain.AggregatedStat;
import com.tango.domain.Stat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service
public class InMemoryStatCalculatorService implements StatCalculatorService, AutoCloseable {
    private final ConcurrentHashMap<Long, StatCell> aggregator;
    @Nonnull
    private final CalculatorConfig calculatorConfig;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    @Autowired
    public InMemoryStatCalculatorService(@Nonnull CalculatorConfig calculatorConfig) {
        this.calculatorConfig = calculatorConfig;

        int capacity = calculatorConfig.getMaxDuration() * calculatorConfig.getGranularity() * 2;
        this.aggregator = new ConcurrentHashMap<>(capacity);
        scheduledExecutorService.scheduleWithFixedDelay(
                this::reduceAggregatorIfRequired,
                calculatorConfig.getMaxDuration(),
                calculatorConfig.getMaxDuration(),
                TimeUnit.SECONDS
        );
    }

    @Override
    public void add(@Nonnull Stat statValue) {
        long currentTime = System.currentTimeMillis() / 1000;
        long beginInterval = currentTime - calculatorConfig.getMaxDuration() + 1;

        if (statValue.getAmount() < 0 || statValue.getTimestamp() < 0) {
            return;
        }
        if (statValue.getTimestampInSec() < beginInterval || statValue.getTimestampInSec() > currentTime) {
            //too late to registry or from future - doesn't handle it
            return;
        }

        long index = statValue.getTimestamp() / (1000 / calculatorConfig.getGranularity());
        aggregator.compute(
                index, (key, value) -> value == null ?
                        new StatCell(statValue.getAmount(), 1, statValue.getAmount(), statValue.getAmount()) :
                        value.add(statValue.getAmount())
        );
    }

    private void reduceAggregatorIfRequired() {
        long currentTime = System.currentTimeMillis() / 1000;
        long min = (currentTime - calculatorConfig.getMaxDuration() + 1) * calculatorConfig.getGranularity();
        aggregator.entrySet().removeIf(entry -> entry.getKey() < min);
    }

    @Nonnull
    @Override
    public AggregatedStat getCurrentAggStat() {
        long currentTime = System.currentTimeMillis() / 1000;
        long beginInterval = (currentTime - calculatorConfig.getMaxDuration() + 1) * calculatorConfig.getGranularity();

        List<StatCell> currentDump = aggregator.entrySet().stream()
                .filter(entry -> entry.getKey() >= beginInterval)
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());


        double sum = currentDump.stream().mapToDouble(StatCell::getSum).sum();
        long count = currentDump.stream().mapToLong(StatCell::getCount).sum();
        return new AggregatedStat(
                sum,
                count > 0 ? sum / count : 0,
                currentDump.stream().mapToDouble(StatCell::getMax).max().orElse(0),
                currentDump.stream().mapToDouble(StatCell::getMin).min().orElse(0),
                count
        );
    }

    @PreDestroy
    @Override
    public void close() throws Exception {
        scheduledExecutorService.shutdownNow();
        scheduledExecutorService.awaitTermination(1, TimeUnit.MINUTES);
    }

    private static class StatCell {
        private final double sum;
        private final long count;
        private final double max;
        private final double min;

        StatCell(double sum, long count, double max, double min) {
            this.sum = sum;
            this.count = count;
            this.max = max;
            this.min = min;
        }

        StatCell add(double amount) {
            return new StatCell(sum + amount, count + 1, Math.max(max, amount), Math.min(min, amount));
        }

        double getSum() {
            return sum;
        }

        long getCount() {
            return count;
        }

        double getMax() {
            return max;
        }

        double getMin() {
            return min;
        }
    }
}
