package jaeik.bimillog.infrastructure.monitoring;

import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * <h2>스레드 분류기</h2>
 * <p>스레드 이름 prefix만으로 {@link ThreadCategory}를 판별한다.</p>
 * <p>톰캣 풀 안/밖 구분은 Spring Boot Actuator의 {@code tomcat_threads_*} 메트릭으로 위임한다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Component
public class ThreadCategoryClassifier {

    private static final String TOMCAT_PREFIX = "http-nio-";

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

    public ThreadCategory classify(String threadName) {
        if (threadName == null) {
            return ThreadCategory.SYSTEM;
        }

        if (threadName.startsWith(TOMCAT_PREFIX)) {
            return ThreadCategory.TOMCAT;
        }

        for (String prefix : ASYNC_PREFIXES) {
            if (threadName.startsWith(prefix)) {
                return ThreadCategory.ASYNC;
            }
        }

        return ThreadCategory.SYSTEM;
    }
}
