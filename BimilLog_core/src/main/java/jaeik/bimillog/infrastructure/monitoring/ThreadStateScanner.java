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
 * <p>1초 주기로 {@link Thread#getAllStackTraces()}를 스캔하여 모든 스레드를
 * (카테고리 × Thread.State) 조합으로 집계한 뒤 Micrometer 게이지로 노출한다.</p>
 *
 * <p>모든 게이지는 동일한 라벨 키 셋 {@code (category, state)}을 가진다.
 * 카테고리 3개 × {@link Thread.State} 6개 = 18개 게이지를 모두 등록한다 (값이 0이어도).</p>
 *
 * <p>톰캣 풀 안/밖(idle/busy) 구분은 Spring Boot Actuator의 {@code tomcat_threads_*} 메트릭으로 위임한다.</p>
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

    private final Map<ThreadCategory, EnumMap<Thread.State, AtomicInteger>> gauges = new EnumMap<>(ThreadCategory.class);

    public ThreadStateScanner(MeterRegistry meterRegistry, ThreadCategoryClassifier classifier) {
        this.meterRegistry = meterRegistry;
        this.classifier = classifier;
        for (ThreadCategory category : ThreadCategory.values()) {
            EnumMap<Thread.State, AtomicInteger> stateMap = new EnumMap<>(Thread.State.class);
            for (Thread.State state : Thread.State.values()) {
                stateMap.put(state, new AtomicInteger());
            }
            gauges.put(category, stateMap);
        }
    }

    @PostConstruct
    public void registerGauges() {
        gauges.forEach((category, stateMap) ->
                stateMap.forEach((state, counter) ->
                        Gauge.builder(METRIC_NAME, counter, AtomicInteger::get)
                                .tag(CATEGORY_TAG, toLabel(category))
                                .tag(STATE_TAG, state.name())
                                .register(meterRegistry)));
    }

    @Scheduled(fixedRate = 1000)
    public void scan() {
        Map<ThreadCategory, EnumMap<Thread.State, Integer>> counts = new EnumMap<>(ThreadCategory.class);
        for (ThreadCategory category : ThreadCategory.values()) {
            EnumMap<Thread.State, Integer> stateCounts = new EnumMap<>(Thread.State.class);
            for (Thread.State state : Thread.State.values()) {
                stateCounts.put(state, 0);
            }
            counts.put(category, stateCounts);
        }

        Thread.getAllStackTraces().keySet().forEach(thread -> {
            ThreadCategory category = classifier.classify(thread.getName());
            counts.get(category).merge(thread.getState(), 1, Integer::sum);
        });

        counts.forEach((category, stateCounts) ->
                stateCounts.forEach((state, count) ->
                        gauges.get(category).get(state).set(count)));
    }

    private static String toLabel(ThreadCategory category) {
        return switch (category) {
            case TOMCAT -> "tomcat";
            case ASYNC -> "async";
            case SYSTEM -> "system";
        };
    }
}
