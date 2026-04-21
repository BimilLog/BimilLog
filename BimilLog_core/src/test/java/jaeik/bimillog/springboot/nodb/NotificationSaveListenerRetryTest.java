package jaeik.bimillog.springboot.nodb;

import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.friend.event.FriendEvent.FriendRequestEvent;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.notification.listener.NotificationSaveListener;
import jaeik.bimillog.domain.notification.service.NotificationCommandService;
import jaeik.bimillog.domain.notification.service.NotificationEventCallback;
import jaeik.bimillog.domain.paper.event.PaperEvent.RollingPaperEvent;
import jaeik.bimillog.domain.post.event.PostEvent.PostFeaturedEvent;
import jaeik.bimillog.infrastructure.config.async.AsyncConfig;
import jaeik.bimillog.infrastructure.config.async.NotificationAsyncConfig;
import jaeik.bimillog.infrastructure.config.RetryConfig;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

/**
 * <h2>NotificationSaveListener 재시도 테스트</h2>
 * <p>DB 관련 예외 발생 시 재시도 로직이 정상 동작하는지 검증</p>
 * <p>AsyncConfig를 포함하여 실제 비동기 환경에서 재시도를 검증</p>
 */
@DisplayName("NotificationSaveListener 재시도 테스트")
@SpringBootTest(classes = {NotificationSaveListener.class, RetryConfig.class, AsyncConfig.class, NotificationAsyncConfig.class})
@Tag("springboot-nodb")
@TestPropertySource(properties = {
        "retry.max-attempts=3",
        "retry.backoff.delay=10",
        "retry.backoff.multiplier=1.0",
        "url=http://localhost:3000"
})
class NotificationSaveListenerRetryTest {

    @Autowired
    private NotificationSaveListener listener;

    @MockitoBean
    private NotificationCommandService notificationCommandService;

    private static final int MAX_ATTEMPTS = 3;
    private static final String BASE_URL = "http://localhost:3000";

    @BeforeEach
    void setUp() {
        Mockito.reset(notificationCommandService);
        ReflectionTestUtils.setField(listener, "baseUrl", BASE_URL);
    }

    @ParameterizedTest(name = "{0} 발생 시 3회 재시도 - 댓글 알림")
    @MethodSource("provideRetryableExceptions")
    @DisplayName("댓글 작성 알림 저장 - DB 예외 발생 시 재시도")
    void handleCommentCreatedEvent_shouldRetryOnDatabaseExceptions(String exceptionName, RuntimeException exception) {
        CommentCreatedEvent event = CommentCreatedEvent.of(1L, "작성자", 2L, 100L);
        willThrow(exception)
                .given(notificationCommandService)
                .saveNotification(anyLong(), any(NotificationType.class), anyString(), anyString(), any(NotificationEventCallback.class));

        listener.handleCommentCreatedEvent(event);

        String expectedMessage = "작성자님이 댓글을 남겼습니다!";
        String expectedUrl = BASE_URL + "/board/post/100";
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(notificationCommandService, times(MAX_ATTEMPTS))
                        .saveNotification(eq(1L), eq(NotificationType.COMMENT), eq(expectedMessage), eq(expectedUrl), any(NotificationEventCallback.class)));
    }

    @ParameterizedTest(name = "{0} 발생 시 3회 재시도 - 롤링페이퍼 알림")
    @MethodSource("provideRetryableExceptions")
    @DisplayName("롤링페이퍼 알림 저장 - DB 예외 발생 시 재시도")
    void handleRollingPaperEvent_shouldRetryOnDatabaseExceptions(String exceptionName, RuntimeException exception) {
        RollingPaperEvent event = new RollingPaperEvent(1L, "작성자");
        willThrow(exception)
                .given(notificationCommandService)
                .saveNotification(anyLong(), any(NotificationType.class), anyString(), anyString(), any(NotificationEventCallback.class));

        listener.handleRollingPaperEvent(event);

        String expectedMessage = "롤링페이퍼에 메시지가 작성되었어요!";
        String expectedUrl = BASE_URL + "/rolling-paper/작성자";
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(notificationCommandService, times(MAX_ATTEMPTS))
                        .saveNotification(eq(1L), eq(NotificationType.MESSAGE), eq(expectedMessage), eq(expectedUrl), any(NotificationEventCallback.class)));
    }

    @ParameterizedTest(name = "{0} 발생 시 3회 재시도 - 인기글 알림")
    @MethodSource("provideRetryableExceptions")
    @DisplayName("인기글 알림 저장 - DB 예외 발생 시 재시도")
    void handlePostFeaturedEvent_shouldRetryOnDatabaseExceptions(String exceptionName, RuntimeException exception) {
        PostFeaturedEvent event = new PostFeaturedEvent(1L, "인기글 등극!", 100L, NotificationType.POST_FEATURED_WEEKLY, "테스트 게시글");
        willThrow(exception)
                .given(notificationCommandService)
                .saveNotification(anyLong(), any(NotificationType.class), anyString(), anyString(), any(NotificationEventCallback.class));

        listener.handlePostFeaturedEvent(event);

        String expectedUrl = BASE_URL + "/board/post/100";
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(notificationCommandService, times(MAX_ATTEMPTS))
                        .saveNotification(eq(1L), eq(NotificationType.POST_FEATURED_WEEKLY), eq("인기글 등극!"), eq(expectedUrl), any(NotificationEventCallback.class)));
    }

    @ParameterizedTest(name = "{0} 발생 시 3회 재시도 - 친구 알림")
    @MethodSource("provideRetryableExceptions")
    @DisplayName("친구 알림 저장 - DB 예외 발생 시 재시도")
    void handleFriendEvent_shouldRetryOnDatabaseExceptions(String exceptionName, RuntimeException exception) {
        FriendRequestEvent event = new FriendRequestEvent(1L, "친구 요청이 도착했습니다!", "친구이름");
        willThrow(exception)
                .given(notificationCommandService)
                .saveNotification(anyLong(), any(NotificationType.class), anyString(), anyString(), any(NotificationEventCallback.class));

        listener.handleFriendEvent(event);

        String expectedUrl = BASE_URL + "/friends?tab=received";
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(notificationCommandService, times(MAX_ATTEMPTS))
                        .saveNotification(eq(1L), eq(NotificationType.FRIEND), eq("친구 요청이 도착했습니다!"), eq(expectedUrl), any(NotificationEventCallback.class)));
    }

    private static Stream<Arguments> provideRetryableExceptions() {
        return Stream.of(
                Arguments.of("TransientDataAccessException",
                        new TransientDataAccessException("일시적 DB 오류") {}),
                Arguments.of("DataAccessResourceFailureException",
                        new DataAccessResourceFailureException("DB 리소스 획득 실패")),
                Arguments.of("QueryTimeoutException",
                        new QueryTimeoutException("쿼리 타임아웃"))
        );
    }

    @Test
    @DisplayName("댓글 알림 - 2회 실패 후 3회차에 성공")
    void handleCommentCreatedEvent_shouldSucceedAfterTwoFailures() {
        CommentCreatedEvent event = CommentCreatedEvent.of(1L, "작성자", 2L, 100L);
        willThrow(new DataAccessResourceFailureException("실패"))
                .willThrow(new QueryTimeoutException("타임아웃"))
                .willDoNothing()
                .given(notificationCommandService)
                .saveNotification(anyLong(), any(NotificationType.class), anyString(), anyString(), any(NotificationEventCallback.class));

        listener.handleCommentCreatedEvent(event);

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(notificationCommandService, times(3))
                        .saveNotification(eq(1L), eq(NotificationType.COMMENT), anyString(), anyString(), any(NotificationEventCallback.class)));
    }

    @Test
    @DisplayName("댓글 알림 - 1회 성공 시 재시도 없음")
    void handleCommentCreatedEvent_shouldNotRetryOnSuccess() {
        CommentCreatedEvent event = CommentCreatedEvent.of(1L, "작성자", 2L, 100L);
        doNothing().when(notificationCommandService)
                .saveNotification(anyLong(), any(NotificationType.class), anyString(), anyString(), any(NotificationEventCallback.class));

        listener.handleCommentCreatedEvent(event);

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(notificationCommandService, times(1))
                        .saveNotification(eq(1L), eq(NotificationType.COMMENT), anyString(), anyString(), any(NotificationEventCallback.class)));
    }

    @Test
    @DisplayName("롤링페이퍼 알림 - 1회 성공 시 재시도 없음")
    void handleRollingPaperEvent_shouldNotRetryOnSuccess() {
        RollingPaperEvent event = new RollingPaperEvent(1L, "작성자");
        doNothing().when(notificationCommandService)
                .saveNotification(anyLong(), any(NotificationType.class), anyString(), anyString(), any(NotificationEventCallback.class));

        listener.handleRollingPaperEvent(event);

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(notificationCommandService, times(1))
                        .saveNotification(eq(1L), eq(NotificationType.MESSAGE), anyString(), anyString(), any(NotificationEventCallback.class)));
    }

    @Test
    @DisplayName("인기글 알림 - 1회 성공 시 재시도 없음")
    void handlePostFeaturedEvent_shouldNotRetryOnSuccess() {
        PostFeaturedEvent event = new PostFeaturedEvent(1L, "주간 인기글!", 100L, NotificationType.POST_FEATURED_WEEKLY, "테스트 게시글");
        doNothing().when(notificationCommandService)
                .saveNotification(anyLong(), any(NotificationType.class), anyString(), anyString(), any(NotificationEventCallback.class));

        listener.handlePostFeaturedEvent(event);

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(notificationCommandService, times(1))
                        .saveNotification(eq(1L), eq(NotificationType.POST_FEATURED_WEEKLY), eq("주간 인기글!"), anyString(), any(NotificationEventCallback.class)));
    }
}
