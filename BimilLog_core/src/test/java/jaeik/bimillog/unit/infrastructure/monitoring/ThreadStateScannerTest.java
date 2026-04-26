package jaeik.bimillog.unit.infrastructure.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jaeik.bimillog.infrastructure.monitoring.ThreadCategory;
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
    @DisplayName("초기화 시 카테고리 × Thread.State 조합 게이지가 모두 등록된다 (3×6=18)")
    void registersAllCategoryStateCombinations() {
        long count = meterRegistry.find("app.thread.states").gauges().size();
        int expected = ThreadCategory.values().length * Thread.State.values().length;

        assertThat(count).isEqualTo(expected);
    }

    @Test
    @DisplayName("등록된 모든 게이지는 동일한 라벨 키 셋 (category, state)을 가진다")
    void allGaugesHaveConsistentLabelKeys() {
        meterRegistry.find("app.thread.states").gauges().forEach(gauge -> {
            assertThat(gauge.getId().getTag("category")).isNotNull();
            assertThat(gauge.getId().getTag("state")).isNotNull();
        });
    }

    @Test
    @DisplayName("category 라벨 값은 3개 (tomcat / async / system)")
    void categoryLabelsCoverAllThree() {
        assertThat(meterRegistry.find("app.thread.states").tag("category", "tomcat").gauges()).hasSize(Thread.State.values().length);
        assertThat(meterRegistry.find("app.thread.states").tag("category", "async").gauges()).hasSize(Thread.State.values().length);
        assertThat(meterRegistry.find("app.thread.states").tag("category", "system").gauges()).hasSize(Thread.State.values().length);
    }

    @Test
    @DisplayName("scan() 호출 후 모든 게이지의 합계가 0보다 크다")
    void scanPopulatesGauges() {
        scanner.scan();

        int sum = meterRegistry.find("app.thread.states").gauges().stream()
                .mapToInt(g -> (int) g.value())
                .sum();

        assertThat(sum).isGreaterThan(0);
    }

    @Test
    @DisplayName("scan() 호출 후 SYSTEM 카테고리 합계가 0보다 크다 (테스트 자체 스레드 등)")
    void scanCountsSystemThreads() {
        scanner.scan();

        int systemSum = meterRegistry.find("app.thread.states")
                .tag("category", "system")
                .gauges().stream()
                .mapToInt(g -> (int) g.value())
                .sum();

        assertThat(systemSum).isGreaterThan(0);
    }
}
