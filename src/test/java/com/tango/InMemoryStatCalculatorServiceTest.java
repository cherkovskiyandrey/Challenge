package com.tango;

import com.tango.domain.AggregatedStat;
import com.tango.domain.Stat;
import com.tango.services.CalculatorConfig;
import com.tango.services.InMemoryStatCalculatorService;
import com.tango.services.StatCalculatorService;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.concurrent.*;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest(InMemoryStatCalculatorService.class)
public class InMemoryStatCalculatorServiceTest {

    private void assertResult(int count, double sum, double max, double min, double avg, AggregatedStat aggregatedStat) {
        assertEquals(count, aggregatedStat.getCount());
        assertEquals(sum, aggregatedStat.getSum(), 0.1);
        assertEquals(max, aggregatedStat.getMax(), 0.1);
        assertEquals(min, aggregatedStat.getMin(), 0.1);
        assertEquals(avg, aggregatedStat.getAvg(), 0.1);
    }

    private InMemoryStatCalculatorService createStatService(int maxDuration, int granularity) {
        return new InMemoryStatCalculatorService(new CalculatorConfig(maxDuration, granularity));
    }

    @Test
    public void receivedOneMessageFromPastTest() throws Exception {
        mockStatic(System.class);
        PowerMockito.when(System.currentTimeMillis()).thenReturn(0L);

        try (InMemoryStatCalculatorService statCalculatorService = createStatService(5, 1)) {
            PowerMockito.when(System.currentTimeMillis()).thenReturn(6000L);
            statCalculatorService.add(new Stat(1, 1000));

            AggregatedStat aggregatedStat = statCalculatorService.getCurrentAggStat();
            assertResult(0, 0, 0, 0, 0, aggregatedStat);
        }
    }

    @Test
    public void receiveOneMessageInFutureTest() throws Exception {
        mockStatic(System.class);
        PowerMockito.when(System.currentTimeMillis()).thenReturn(1000L);

        try (InMemoryStatCalculatorService statCalculatorService = createStatService(5, 1)) {
            statCalculatorService.add(new Stat(1, 2000L));

            AggregatedStat aggregatedStat = statCalculatorService.getCurrentAggStat();
            assertResult(0, 0, 0, 0, 0, aggregatedStat);
        }
    }

    @Test
    public void receivedOneMessageEqualToLeftAllowedBorderTest() throws Exception {
        mockStatic(System.class);
        PowerMockito.when(System.currentTimeMillis()).thenReturn(0L);

        try (InMemoryStatCalculatorService statCalculatorService = createStatService(5, 1)) {
            PowerMockito.when(System.currentTimeMillis()).thenReturn(6000L);
            statCalculatorService.add(new Stat(1, 2000));

            AggregatedStat aggregatedStat = statCalculatorService.getCurrentAggStat();
            assertResult(1, 1, 1, 1, 1, aggregatedStat);
        }
    }

    @Test
    public void receivedOneMessageEqualToRightAllowedBorderTest() throws Exception {
        mockStatic(System.class);
        PowerMockito.when(System.currentTimeMillis()).thenReturn(0L);

        try (InMemoryStatCalculatorService statCalculatorService = createStatService(5, 1)) {
            PowerMockito.when(System.currentTimeMillis()).thenReturn(6000L);
            statCalculatorService.add(new Stat(1, 6500));

            AggregatedStat aggregatedStat = statCalculatorService.getCurrentAggStat();
            assertResult(1, 1, 1, 1, 1, aggregatedStat);
        }
    }

    @Test
    public void reduceCapacityTest() throws Exception {
        mockStatic(System.class);
        PowerMockito.when(System.currentTimeMillis()).thenReturn(0L);

        try (InMemoryStatCalculatorService statCalculatorService = createStatService(5, 2)) {
            for (int i = 0; i < 12; ++i) {
                PowerMockito.when(System.currentTimeMillis()).thenReturn(i * 500L);
                statCalculatorService.add(new Stat(1, i * 500L));
            }
            //don't relay on task scheduled
            Whitebox.invokeMethod(statCalculatorService, "reduceAggregatorIfRequired");
            AggregatedStat aggregatedStat = statCalculatorService.getCurrentAggStat();
            ConcurrentHashMap<Long, ?> innerMap = Whitebox.getInternalState(statCalculatorService, "aggregator");

            assertEquals(10, innerMap.size());
            assertFalse(innerMap.contains(1));
            assertResult(10, 10, 1, 1, 1, aggregatedStat);

            for (int i = 12; i < 22; ++i) {
                PowerMockito.when(System.currentTimeMillis()).thenReturn(i * 500L);
                statCalculatorService.add(new Stat(1, i * 500L));
            }
            Whitebox.invokeMethod(statCalculatorService, "reduceAggregatorIfRequired");
            assertEquals(10, innerMap.size());
            assertFalse(innerMap.contains(2));
            assertFalse(innerMap.contains(3));
            assertFalse(innerMap.contains(4));
            assertFalse(innerMap.contains(5));
            assertFalse(innerMap.contains(6));
            assertFalse(innerMap.contains(7));
            assertFalse(innerMap.contains(8));
            assertFalse(innerMap.contains(9));
            assertFalse(innerMap.contains(10));
            assertFalse(innerMap.contains(11));

            aggregatedStat = statCalculatorService.getCurrentAggStat();
            assertResult(10, 10, 1, 1, 1, aggregatedStat);
        }
    }

    @Test
    public void minMaxAvgTest() throws Exception {
        mockStatic(System.class);
        PowerMockito.when(System.currentTimeMillis()).thenReturn(0L);

        try (InMemoryStatCalculatorService statCalculatorService = createStatService(5, 1)) {
            PowerMockito.when(System.currentTimeMillis()).thenReturn(4000L);
            for (int i = 0; i < 10; ++i) {
                for (int j = 0; j < 5; ++j) {
                    statCalculatorService.add(new Stat(i + i / 10.d, j * 1000));
                }
            }

            AggregatedStat aggregatedStat = statCalculatorService.getCurrentAggStat();
            assertResult(50, 247.5, 9.9, 0, 4.95, aggregatedStat);
        }
    }

    @Test
    public void overflowSum() throws Exception {
        mockStatic(System.class);
        PowerMockito.when(System.currentTimeMillis()).thenReturn(0L);

        try (InMemoryStatCalculatorService statCalculatorService = createStatService(5, 1)) {
            statCalculatorService.add(new Stat(Double.MAX_VALUE, 100));
            statCalculatorService.add(new Stat(Double.MAX_VALUE, 200));
            AggregatedStat aggregatedStat = statCalculatorService.getCurrentAggStat();

            assertEquals(2, aggregatedStat.getCount());
            assertTrue(Double.isInfinite(aggregatedStat.getSum()));
            assertEquals(Double.MAX_VALUE, aggregatedStat.getMax(), 0.1);
            assertEquals(Double.MAX_VALUE, aggregatedStat.getMin(), 0.1);
            assertTrue(Double.isInfinite(aggregatedStat.getAvg()));
        }
    }

    @Test
    public void invalidValues() throws Exception {
        mockStatic(System.class);
        PowerMockito.when(System.currentTimeMillis()).thenReturn(0L);

        try (InMemoryStatCalculatorService statCalculatorService = createStatService(5, 1)) {
            statCalculatorService.add(new Stat(-10.123, 100));
            statCalculatorService.add(new Stat(1, -200));

            AggregatedStat aggregatedStat = statCalculatorService.getCurrentAggStat();
            assertResult(0, 0, 0, 0, 0, aggregatedStat);
        }
    }

    @Test
    //@Ignore
    public void highLoadAccuracyTest() throws Exception {
        final CalculatorConfig calculatorConfig = new CalculatorConfig(60, 1);
        final int highLoadRange = 3; //last 3 second
        final int threads = 100;
        final int addCycles = 1000;
        final int cycles = 10;

        for (int d = 0; d < cycles; ++d) {
            ExecutorService executorService = Executors.newFixedThreadPool(threads);
            StatCalculatorService statCalculatorService = new InMemoryStatCalculatorService(calculatorConfig);

            StopWatch stopWatch = new StopWatch();
            final CyclicBarrier cyclicBarrierForStart = new CyclicBarrier(threads, stopWatch::start);
            final CyclicBarrier cyclicBarrierForStop = new CyclicBarrier(threads, stopWatch::stop);
            for (int k = 0; k < threads; ++k) {
                executorService.submit(() -> {
                    try {
                        cyclicBarrierForStart.await();
                    } catch (InterruptedException | BrokenBarrierException ignore) {
                    }
                    for (int i = 0; i < addCycles; ++i) {
                        statCalculatorService.add(new Stat(1, System.currentTimeMillis() - (i % highLoadRange)));
                        //statCalculatorService.getCurrentAggStat();
                    }
                    try {
                        cyclicBarrierForStop.await();
                    } catch (InterruptedException | BrokenBarrierException ignore) {
                    }
                });
            }
            executorService.shutdown();
            executorService.awaitTermination(calculatorConfig.getMaxDuration(), TimeUnit.HOURS);

            System.out.println("calculated: " + stopWatch.getTime());
            AggregatedStat aggregatedStat = statCalculatorService.getCurrentAggStat();
            System.out.println(aggregatedStat);

            assertResult(threads * addCycles, threads * addCycles, 1, 1, 1, aggregatedStat);
        }
    }
}
