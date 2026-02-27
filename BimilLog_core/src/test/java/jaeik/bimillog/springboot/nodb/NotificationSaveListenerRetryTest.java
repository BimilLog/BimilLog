package jaeik.bimillog.springboot.nodb;

import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.friend.event.FriendEvent;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.notification.listener.NotificationSaveListener;
import jaeik.bimillog.domain.notification.service.NotificationCommandService;
import jaeik.bimillog.domain.paper.event.RollingPaperEvent;
import jaeik.bimillog.domain.post.event.PostFeaturedEvent;
import jaeik.bimillog.infrastructure.config.AsyncConfig;
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

import java.time.Duration;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

/**
 * <h2>NotificationSaveListener 재시도 테스트</h2>
 * <p>DB 관련 예외 발생 시 재시도 로직이 정상 동작하는지 검증</p>
 * <p>AsyncConfig를 포함하여 실제 비동기 환경에서 재시도를 검증</p>
 */
@DisplayName("NotificationSaveListener 재시도 테스트")
@Tag("springboot-nodb")
@SpringBootTest(classes = {NotificationSaveListener.class, RetryConfig.class, AsyncConfig.class})
@TestPropertySource(properties = {
        "retry.max-attempts=3",
        "retry.backoff.delay=10",
        "retry.backoff.multiplier=1.0"
})
class NotificationSaveListenerRetryTest {

    @Autowired
    private NotificationSaveListener listener;

    @MockitoBean
    private NotificationCommandService notificationCommandService;

    private static final int MAX_ATTEMPTS = 3;

    @BeforeEach
    void setUp() {
        Mockito.reset(notificationCommandService);
    }

    @ParameterizedTest(name = "{0} 발생 시 3회 재시도 - 댓글 알림")
    @MethodSource("provideRetryableExceptions")
    @DisplayName("댓글 작성 알림 저장 - DB 예외 발생 시 재시도")
    void handleCommentCreatedEvent_shouldRetryOnDatabaseExceptions(String exceptionName, RuntimeException exception) {
        // Given
        CommentCreatedEvent event = new CommentCreatedEvent(1L, "작성자", 2L, 100L);
        willThrow(exception)
                .given(notificationCommandService).saveCommentNotification(anyLong(), anyString(), anyLong());

        // When: 비동기로 실행되며 @Recover 메서드가 있으므로 예외가 외부로 전파되지 않음
        listener.handleCommentCreatedEvent(event);

        // Then: 비동기 완료 대기 후 재시도 횟수만큼 호출되었는지 검증
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(notificationCommandService, times(MAX_ATTEMPTS))
                        .saveCommentNotification(1L, "작성자", 100L));
    }

    @ParameterizedTest(name = "{0} 발생 시 3회 재시도 - 롤링페이퍼 알림")
    @MethodSource("provideRetryableExceptions")
    @DisplayName("롤링페이퍼 알림 저장 - DB 예외 발생 시 재시도")
    void handleRollingPaperEvent_shouldRetryOnDatabaseExceptions(String exceptionName, RuntimeException exception) {
        // Given
        RollingPaperEvent event = new RollingPaperEvent(1L, "작성자");
        willThrow(exception)
                .given(notificationCommandService).saveMessageNotification(anyLong(), anyString());

        // When: 비동기로 실행되며 @Recover 메서드가 있으므로 예외가 외부로 전파되지 않음
        listener.handleRollingPaperEvent(event);

        // Then: 비동기 완료 대기 후 재시도 횟수만큼 호출되었는지 검증
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(notificationCommandService, times(MAX_ATTEMPTS))
                        .saveMessageNotification(1L, "작성자"));
    }

    @ParameterizedTest(name = "{0} 발생 시 3회 재시도 - 인기글 알림")
    @MethodSource("provideRetryableExceptions")
    @DisplayName("인기글 알림 저장 - DB 예외 발생 시 재시도")
    void handlePostFeaturedEvent_shouldRetryOnDatabaseExceptions(String exceptionName, RuntimeException exception) {
        // Given
        PostFeaturedEvent event = new PostFeaturedEvent(1L, "인기글 등극!", 100L, NotificationType.POST_FEATURED_WEEKLY, "테스트 게시글");
        willThrow(exception)
                .given(notificationCommandService).savePopularNotification(anyLong(), anyString(), anyLong(), any(NotificationType.class), anyString());

        // When: 비동기로 실행되며 @Recover 메서드가 있으므로 예외가 외부로 전파되지 않음
        listener.handlePostFeaturedEvent(event);

        // Then: 비동기 완료 대기 후 재시도 횟수만큼 호출되었는지 검증
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(notificationCommandService, times(MAX_ATTEMPTS))
                        .savePopularNotification(1L, "인기글 등극!", 100L, NotificationType.POST_FEATURED_WEEKLY, "테스트 게시글"));
    }

    @ParameterizedTest(name = "{0} 발생 시 3회 재시도 - 친구 알림")
    @MethodSource("provideRetryableExceptions")
    @DisplayName("친구 알림 저장 - DB 예외 발생 시 재시도")
    void handleFriendEvent_shouldRetryOnDatabaseExceptions(String exceptionName, RuntimeException exception) {
        // Given
        FriendEvent event = new FriendEvent(1L, "친구 요청이 도착했습니다!", "친구이름");
        willThrow(exception)
                .given(notificationCommandService).saveFriendNotification(anyLong(), anyString(), anyString());

        // When: 비동기로 실행되며 @Recover 메서드가 있으므로 예외가 외부로 전파되지 않음
        listener.handleFriendEvent(event);

        // Then: 비동기 완료 대기 후 재시도 횟수만큼 호출되었는지 검증
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(notificationCommandService, times(MAX_ATTEMPTS))
                        .saveFriendNotification(1L, "친구 요청이 도착했습니다!", "친구이름"));
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
        // Given
        CommentCreatedEvent event = new CommentCreatedEvent(1L, "작성자", 2L, 100L);
        willThrow(new DataAccessResourceFailureException("실패"))
                .willThrow(new QueryTimeoutException("타임아웃"))
                .willDoNothing()
                .given(notificationCommandService).saveCommentNotification(1L, "작성자", 100L);

        // When
        listener.handleCommentCreatedEvent(event);

        // Then: 비동기 완료 대기
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(notificationCommandService, times(3))
                        .saveCommentNotification(1L, "작성자", 100L));
    }

    @Test
    @DisplayName("댓글 알림 - 1회 성공 시 재시도 없음")
    void handleCommentCreatedEvent_shouldNotRetryOnSuccess() {
        // Given
        CommentCreatedEvent event = new CommentCreatedEvent(1L, "작성자", 2L, 100L);
        doNothing().when(notificationCommandService).saveCommentNotification(anyLong(), anyString(), anyLong());

        // When
        listener.handleCommentCreatedEvent(event);

        // Then: 비동기 완료 대기
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(notificationCommandService, times(1))
                        .saveCommentNotification(1L, "작성자", 100L));
    }

    @Test
    @DisplayName("롤링페이퍼 알림 - 1회 성공 시 재시도 없음")
    void handleRollingPaperEvent_shouldNotRetryOnSuccess() {
        // Given
        RollingPaperEvent event = new RollingPaperEvent(1L, "작성자");
        doNothing().when(notificationCommandService).saveMessageNotification(anyLong(), anyString());

        // When
        listener.handleRollingPaperEvent(event);

        // Then: 비동기 완료 대기
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(notificationCommandService, times(1))
                        .saveMessageNotification(1L, "작성자"));
    }

    @Test
    @DisplayName("인기글 알림 - 1회 성공 시 재시도 없음")
    void handlePostFeaturedEvent_shouldNotRetryOnSuccess() {
        // Given
        PostFeaturedEvent event = new PostFeaturedEvent(1L, "주간 인기글!", 100L, NotificationType.POST_FEATURED_WEEKLY, "테스트 게시글");
        doNothing().when(notificationCommandService).savePopularNotification(anyLong(), anyString(), anyLong(), any(NotificationType.class), anyString());

        // When
        listener.handlePostFeaturedEvent(event);

        // Then: 비동기 완료 대기
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(notificationCommandService, times(1))
                        .savePopularNotification(1L, "주간 인기글!", 100L, NotificationType.POST_FEATURED_WEEKLY, "테스트 게시글"));
    }
}
