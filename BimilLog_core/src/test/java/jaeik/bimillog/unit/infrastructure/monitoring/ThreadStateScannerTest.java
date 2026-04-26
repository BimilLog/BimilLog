package jaeik.bimillog.unit.infrastructure.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jaeik.bimillog.infrastructure.monitoring.ThreadCategoryClassifier;
import jaeik.bimillog.infrastructure.monitoring.ThreadStateScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ThreadStateScanner 단위 테스트")
@Tag("unit")
class ThreadStateScannerTest {

    private MeterRegistry meterRegistry;
    private ThreadStateScanner scanner;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        scanner = new ThreadStateScanner(meterRegistry, new ThreadCategoryClassifier());
        scanner.registerGauges();
    }

    @Test
    @DisplayName("초기화 시 7개 게이지가 등록된다 (tomcat-idle, tomcat-busy×4 state, async, system)")
    void registersSevenGauges() {
        long count = meterRegistry.find("app.thread.states").gauges().size();

        assertThat(count).isEqualTo(7);
    }

    @Test
    @DisplayName("등록된 게이지의 라벨이 정확하다")
    void gaugeLabelsAreCorrect() {
        assertThat(meterRegistry.find("app.thread.states").tag("category", "tomcat-idle").gauge()).isNotNull();
        assertThat(meterRegistry.find("app.thread.states").tag("category", "tomcat-busy").tag("state", "RUNNABLE").gauge()).isNotNull();
        assertThat(meterRegistry.find("app.thread.states").tag("category", "tomcat-busy").tag("state", "WAITING").gauge()).isNotNull();
        assertThat(meterRegistry.find("app.thread.states").tag("category", "tomcat-busy").tag("state", "TIMED_WAITING").gauge()).isNotNull();
        assertThat(meterRegistry.find("app.thread.states").tag("category", "tomcat-busy").tag("state", "BLOCKED").gauge()).isNotNull();
        assertThat(meterRegistry.find("app.thread.states").tag("category", "async").gauge()).isNotNull();
        assertThat(meterRegistry.find("app.thread.states").tag("category", "system").gauge()).isNotNull();
    }

    @Test
    @DisplayName("scan() 호출 후 게이지 합계가 JVM 라이브 스레드 수와 비슷하다")
    void scanPopulatesGauges() {
        scanner.scan();

        int sum = readGauge("tomcat-idle")
                + readBusyGauge("RUNNABLE")
                + readBusyGauge("WAITING")
                + readBusyGauge("TIMED_WAITING")
                + readBusyGauge("BLOCKED")
                + readGauge("async")
                + readGauge("system");

        int liveThreads = Thread.activeCount();
        assertThat(sum).isBetween((int) (liveThreads * 0.5), liveThreads * 3);
        assertThat(sum).isGreaterThan(0);
    }

    @Test
    @DisplayName("scan() 호출 후 SYSTEM 카테고리에 최소 1개 스레드가 있다 (테스트 자체 스레드 등)")
    void scanCountsSystemThreads() {
        scanner.scan();

        assertThat(readGauge("system")).isGreaterThan(0);
    }

    private int readGauge(String category) {
        return (int) meterRegistry.find("app.thread.states").tag("category", category).gauge().value();
    }

    private int readBusyGauge(String state) {
        return (int) meterRegistry.find("app.thread.states")
                .tag("category", "tomcat-busy")
                .tag("state", state)
                .gauge().value();
    }
}
