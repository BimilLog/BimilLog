package jaeik.bimillog.infrastructure.monitoring;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <h2>스레드 상태 스캐너</h2>
 * <p>1초 주기로 {@link Thread#getAllStackTraces()}를 스캔하여 스레드를
 * 카테고리(tomcat-idle / tomcat-busy / async / system)별로 집계한 뒤
 * Micrometer 게이지로 노출한다.</p>
 *
 * <p>{@code monitoring.thread-scanner.enabled=true}일 때만 빈으로 등록된다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Component
@ConditionalOnProperty(name = "monitoring.thread-scanner.enabled", havingValue = "true")
public class ThreadStateScanner {

    private static final String METRIC_NAME = "app.thread.states";
    private static final String CATEGORY_TAG = "category";
    private static final String STATE_TAG = "state";

    private final MeterRegistry meterRegistry;
    private final ThreadCategoryClassifier classifier;

    private final AtomicInteger tomcatIdle = new AtomicInteger();
    private final AtomicInteger asyncCount = new AtomicInteger();
    private final AtomicInteger systemCount = new AtomicInteger();
    private final Map<Thread.State, AtomicInteger> tomcatBusyByState = new EnumMap<>(Thread.State.class);

    public ThreadStateScanner(MeterRegistry meterRegistry, ThreadCategoryClassifier classifier) {
        this.meterRegistry = meterRegistry;
        this.classifier = classifier;
        for (Thread.State state : new Thread.State[]{Thread.State.RUNNABLE, Thread.State.WAITING, Thread.State.TIMED_WAITING, Thread.State.BLOCKED}) {
            tomcatBusyByState.put(state, new AtomicInteger());
        }
    }

    @PostConstruct
    public void registerGauges() {
        Gauge.builder(METRIC_NAME, tomcatIdle, AtomicInteger::get)
                .tag(CATEGORY_TAG, "tomcat-idle")
                .register(meterRegistry);

        tomcatBusyByState.forEach((state, counter) ->
                Gauge.builder(METRIC_NAME, counter, AtomicInteger::get)
                        .tag(CATEGORY_TAG, "tomcat-busy")
                        .tag(STATE_TAG, state.name())
                        .register(meterRegistry));

        Gauge.builder(METRIC_NAME, asyncCount, AtomicInteger::get)
                .tag(CATEGORY_TAG, "async")
                .register(meterRegistry);

        Gauge.builder(METRIC_NAME, systemCount, AtomicInteger::get)
                .tag(CATEGORY_TAG, "system")
                .register(meterRegistry);
    }

    @Scheduled(fixedRate = 1000)
    public void scan() {
        int idle = 0;
        int async = 0;
        int system = 0;
        Map<Thread.State, Integer> busy = new EnumMap<>(Thread.State.class);
        for (Thread.State state : tomcatBusyByState.keySet()) {
            busy.put(state, 0);
        }

        Map<Thread, StackTraceElement[]> traces = Thread.getAllStackTraces();
        for (Map.Entry<Thread, StackTraceElement[]> entry : traces.entrySet()) {
            Thread thread = entry.getKey();
            ThreadCategory category = classifier.classify(thread.getName(), entry.getValue());

            switch (category) {
                case TOMCAT_IDLE -> idle++;
                case TOMCAT_BUSY -> {
                    Thread.State state = thread.getState();
                    if (busy.containsKey(state)) {
                        busy.merge(state, 1, Integer::sum);
                    } else {
                        // NEW / TERMINATED 등 — RUNNABLE 버킷으로 합산 (드물게 발생)
                        busy.merge(Thread.State.RUNNABLE, 1, Integer::sum);
                    }
                }
                case ASYNC -> async++;
                case SYSTEM -> system++;
            }
        }

        tomcatIdle.set(idle);
        asyncCount.set(async);
        systemCount.set(system);
        busy.forEach((state, count) -> tomcatBusyByState.get(state).set(count));
    }
}
