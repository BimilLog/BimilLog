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
    @DisplayName("http-nio- prefix 스레드는 톰캣으로 분류된다")
    void classifyTomcatPrefix() {
        ThreadCategory result = classifier.classify("http-nio-8080-exec-1", new StackTraceElement[0]);

        assertThat(result).isEqualTo(ThreadCategory.TOMCAT_IDLE);
    }

    @Test
    @DisplayName("톰캣 스레드 스택 최상단이 TaskQueue.poll이면 풀 안(TOMCAT_IDLE)으로 분류된다")
    void classifyTomcatIdleByStackTop() {
        StackTraceElement[] stack = {
                new StackTraceElement("org.apache.tomcat.util.threads.TaskQueue", "poll", "TaskQueue.java", 100),
                new StackTraceElement("java.util.concurrent.ThreadPoolExecutor", "getTask", "ThreadPoolExecutor.java", 1000)
        };

        ThreadCategory result = classifier.classify("http-nio-8080-exec-3", stack);

        assertThat(result).isEqualTo(ThreadCategory.TOMCAT_IDLE);
    }

    @Test
    @DisplayName("톰캣 스레드 스택 최상단이 LinkedBlockingQueue.take면 풀 안(TOMCAT_IDLE)으로 분류된다")
    void classifyTomcatIdleByLinkedBlockingQueue() {
        StackTraceElement[] stack = {
                new StackTraceElement("java.util.concurrent.LinkedBlockingQueue", "take", "LinkedBlockingQueue.java", 433),
                new StackTraceElement("java.util.concurrent.ThreadPoolExecutor", "getTask", "ThreadPoolExecutor.java", 1000)
        };

        ThreadCategory result = classifier.classify("http-nio-8080-exec-7", stack);

        assertThat(result).isEqualTo(ThreadCategory.TOMCAT_IDLE);
    }

    @Test
    @DisplayName("톰캣 스레드가 요청 처리 중(스택 최상단이 큐가 아님)이면 TOMCAT_BUSY로 분류된다")
    void classifyTomcatBusyWhenProcessingRequest() {
        StackTraceElement[] stack = {
                new StackTraceElement("jaeik.bimillog.domain.member.service.MemberQueryService", "getProfile", "MemberQueryService.java", 50),
                new StackTraceElement("org.springframework.web.servlet.DispatcherServlet", "doDispatch", "DispatcherServlet.java", 1000)
        };

        ThreadCategory result = classifier.classify("http-nio-8080-exec-5", stack);

        assertThat(result).isEqualTo(ThreadCategory.TOMCAT_BUSY);
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
            ThreadCategory result = classifier.classify(name, new StackTraceElement[0]);
            assertThat(result).as("prefix=%s", name).isEqualTo(ThreadCategory.ASYNC);
        }
    }

    @Test
    @DisplayName("매칭되지 않는 스레드는 SYSTEM으로 분류된다")
    void classifySystemFallback() {
        assertThat(classifier.classify("HikariPool-1-housekeeper", new StackTraceElement[0]))
                .isEqualTo(ThreadCategory.SYSTEM);
        assertThat(classifier.classify("lettuce-eventExecutorLoop-1-1", new StackTraceElement[0]))
                .isEqualTo(ThreadCategory.SYSTEM);
        assertThat(classifier.classify("ForkJoinPool.commonPool-worker-1", new StackTraceElement[0]))
                .isEqualTo(ThreadCategory.SYSTEM);
        assertThat(classifier.classify("scheduling-1", new StackTraceElement[0]))
                .isEqualTo(ThreadCategory.SYSTEM);
    }
}
