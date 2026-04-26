package jaeik.bimillog.unit.infrastructure.monitoring;

import jaeik.bimillog.infrastructure.monitoring.ThreadCategory;
import jaeik.bimillog.infrastructure.monitoring.ThreadCategoryClassifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ThreadCategoryClassifier 단위 테스트")
@Tag("unit")
class ThreadCategoryClassifierTest {

    private final ThreadCategoryClassifier classifier = new ThreadCategoryClassifier();

    @Test
    @DisplayName("http-nio- prefix 스레드는 TOMCAT으로 분류된다")
    void classifyTomcatPrefix() {
        assertThat(classifier.classify("http-nio-8080-exec-1")).isEqualTo(ThreadCategory.TOMCAT);
        assertThat(classifier.classify("http-nio-8080-exec-7")).isEqualTo(ThreadCategory.TOMCAT);
    }

    @Test
    @DisplayName("등록된 비동기 prefix 스레드는 ASYNC로 분류된다")
    void classifyAsyncPrefixes() {
        String[] asyncPrefixes = {
                "task-1",
                "sse-notification-2",
                "fcm-notification-3",
                "save-notification-4",
                "member-event-5",
                "friend-update-6",
                "realtime-event-7",
                "cache-count-8",
                "cache-refresh-9",
                "report-event-10",
                "rebuild-producer-11",
                "rebuild-consumer-12",
                "interaction-producer-13",
                "interaction-consumer-14",
                "circuit-sync-15"
        };

        for (String name : asyncPrefixes) {
            assertThat(classifier.classify(name)).as("prefix=%s", name).isEqualTo(ThreadCategory.ASYNC);
        }
    }

    @Test
    @DisplayName("매칭되지 않는 스레드는 SYSTEM으로 분류된다")
    void classifySystemFallback() {
        assertThat(classifier.classify("HikariPool-1-housekeeper")).isEqualTo(ThreadCategory.SYSTEM);
        assertThat(classifier.classify("lettuce-eventExecutorLoop-1-1")).isEqualTo(ThreadCategory.SYSTEM);
        assertThat(classifier.classify("ForkJoinPool.commonPool-worker-1")).isEqualTo(ThreadCategory.SYSTEM);
        assertThat(classifier.classify("scheduling-1")).isEqualTo(ThreadCategory.SYSTEM);
    }

    @Test
    @DisplayName("null 스레드명은 SYSTEM으로 분류된다")
    void classifyNullThreadName() {
        assertThat(classifier.classify(null)).isEqualTo(ThreadCategory.SYSTEM);
    }
}
