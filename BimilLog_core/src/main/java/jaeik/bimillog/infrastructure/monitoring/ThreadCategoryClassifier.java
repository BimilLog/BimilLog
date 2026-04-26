package jaeik.bimillog.infrastructure.monitoring;

import java.util.Set;

/**
 * <h2>스레드 분류기</h2>
 * <p>스레드 이름 prefix와 스택 최상단 프레임을 보고 {@link ThreadCategory}를 판별한다.</p>
 * <p>순수 함수 — 외부 상태 의존 없이 단위 테스트 가능하다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
public class ThreadCategoryClassifier {

    private static final String TOMCAT_PREFIX = "http-nio-";

    /**
     * 톰캣 스레드가 풀 안에서 작업을 대기할 때 스택 최상단에 나타나는 클래스들.
     */
    private static final Set<String> TOMCAT_IDLE_TOP_CLASSES = Set.of(
            "org.apache.tomcat.util.threads.TaskQueue",
            "java.util.concurrent.LinkedBlockingQueue"
    );

    /**
     * 톰캣 풀 안 대기 시 호출되는 메서드 이름.
     */
    private static final Set<String> TOMCAT_IDLE_TOP_METHODS = Set.of("poll", "take");

    /**
     * 비동기 풀 스레드 이름 prefix 화이트리스트.
     * <p>{@code AsyncConfig}/{@code PostAsyncConfig}/{@code FriendAsyncConfig}/{@code NotificationAsyncConfig}에서
     * 정의하는 모든 풀의 {@code threadNamePrefix}가 여기에 포함되어야 한다.</p>
     * <p>또한 Spring 기본 비동기 실행기({@code task-})를 포함한다.</p>
     */
    private static final Set<String> ASYNC_PREFIXES = Set.of(
            "task-",
            "sse-notification-",
            "fcm-notification-",
            "save-notification-",
            "member-event-",
            "friend-update-",
            "realtime-event-",
            "cache-count-",
            "cache-refresh-",
            "report-event-",
            "rebuild-producer-",
            "rebuild-consumer-",
            "interaction-producer-",
            "interaction-consumer-",
            "circuit-sync-"
    );

    public ThreadCategory classify(String threadName, StackTraceElement[] stack) {
        if (threadName == null) {
            return ThreadCategory.SYSTEM;
        }

        if (threadName.startsWith(TOMCAT_PREFIX)) {
            return classifyTomcat(stack);
        }

        for (String prefix : ASYNC_PREFIXES) {
            if (threadName.startsWith(prefix)) {
                return ThreadCategory.ASYNC;
            }
        }

        return ThreadCategory.SYSTEM;
    }

    private ThreadCategory classifyTomcat(StackTraceElement[] stack) {
        if (stack == null || stack.length == 0) {
            return ThreadCategory.TOMCAT_IDLE;
        }

        StackTraceElement top = stack[0];
        boolean idleTopClass = TOMCAT_IDLE_TOP_CLASSES.contains(top.getClassName());
        boolean idleTopMethod = TOMCAT_IDLE_TOP_METHODS.contains(top.getMethodName());

        if (idleTopClass && idleTopMethod) {
            return ThreadCategory.TOMCAT_IDLE;
        }

        return ThreadCategory.TOMCAT_BUSY;
    }
}
