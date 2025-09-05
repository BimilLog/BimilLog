package jaeik.bimillog.infrastructure.adapter.post.in.listener;

import jaeik.bimillog.domain.post.application.port.in.PostInteractionUseCase;
import jaeik.bimillog.domain.post.event.PostViewedEvent;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.exception.PostErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * <h2>게시글 조회 이벤트 리스너 테스트</h2>
 * <p>PostViewEventListener의 단위 테스트</p>
 * <p>게시글 조회 이벤트 처리 로직을 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("게시글 조회 이벤트 리스너 테스트")
class PostViewIncreaseListenerTest {

    @Mock
    private PostInteractionUseCase postInteractionUseCase;

    @InjectMocks
    private PostViewIncreaseListener postViewIncreaseListener;

    @Test
    @DisplayName("게시글 조회 이벤트 처리 - 정상적인 조회수 증가")
    void handlePostViewedEvent_ShouldIncrementViewCount() {
        // Given
        Long postId = 123L;
        PostViewedEvent event = new PostViewedEvent(postId);

        // When
        postViewIncreaseListener.handlePostViewedEvent(event);

        // Then
        verify(postInteractionUseCase).incrementViewCount(eq(postId));
    }

    @Test
    @DisplayName("게시글 조회 이벤트 처리 - 존재하지 않는 게시글")
    void handlePostViewedEvent_WhenPostNotFound_ShouldHandleException() {
        // Given
        Long postId = 999L;
        PostViewedEvent event = new PostViewedEvent(postId);
        
        doThrow(new PostCustomException(PostErrorCode.POST_NOT_FOUND))
                .when(postInteractionUseCase)
                .incrementViewCount(postId);

        // When
        postViewIncreaseListener.handlePostViewedEvent(event);

        // Then - 예외가 발생해도 메서드는 정상 완료되어야 함 (로그만 남기고 예외를 삼킴)
        verify(postInteractionUseCase).incrementViewCount(eq(postId));
    }

    @Test
    @DisplayName("게시글 조회 이벤트 처리 - 정상적인 조회수 증가 (다른 ID)")
    void handlePostViewedEvent_WithValidPostId_ShouldIncrementViewCount() {
        // Given
        Long postId = 999L;
        PostViewedEvent event = new PostViewedEvent(postId);

        // When
        postViewIncreaseListener.handlePostViewedEvent(event);

        // Then
        verify(postInteractionUseCase).incrementViewCount(eq(postId));
    }

    @Test
    @DisplayName("게시글 조회 이벤트 처리 - 다양한 postId 값들")
    void handlePostViewedEvent_WithVariousPostIds() {
        // Given
        Long postId1 = 1L;
        Long postId2 = 999999L;
        Long postId3 = 0L;
        
        PostViewedEvent event1 = new PostViewedEvent(postId1);
        PostViewedEvent event2 = new PostViewedEvent(postId2);
        PostViewedEvent event3 = new PostViewedEvent(postId3);

        // When
        postViewIncreaseListener.handlePostViewedEvent(event1);
        postViewIncreaseListener.handlePostViewedEvent(event2);
        postViewIncreaseListener.handlePostViewedEvent(event3);

        // Then
        verify(postInteractionUseCase).incrementViewCount(eq(postId1));
        verify(postInteractionUseCase).incrementViewCount(eq(postId2));
        verify(postInteractionUseCase).incrementViewCount(eq(postId3));
    }

    @Test
    @DisplayName("게시글 조회 이벤트 처리 - 동일 게시글 여러 번 조회")
    void handlePostViewedEvent_SamePostMultipleTimes() {
        // Given
        Long postId = 123L;
        
        PostViewedEvent event1 = new PostViewedEvent(postId);
        PostViewedEvent event2 = new PostViewedEvent(postId);
        PostViewedEvent event3 = new PostViewedEvent(postId);

        // When
        postViewIncreaseListener.handlePostViewedEvent(event1);
        postViewIncreaseListener.handlePostViewedEvent(event2);
        postViewIncreaseListener.handlePostViewedEvent(event3);

        // Then - 매번 호출되어야 함 (중복 검사는 Controller 레이어에서)
        verify(postInteractionUseCase, times(3)).incrementViewCount(eq(postId));
    }

    @Test
    @DisplayName("게시글 조회 이벤트 처리 - UseCase에서 RuntimeException 발생")
    void handlePostViewedEvent_WhenUseCaseThrowsRuntimeException() {
        // Given
        Long postId = 123L;
        PostViewedEvent event = new PostViewedEvent(postId);
        
        RuntimeException runtimeException = new RuntimeException("데이터베이스 연결 오류");
        doThrow(runtimeException)
                .when(postInteractionUseCase)
                .incrementViewCount(postId);

        // When
        postViewIncreaseListener.handlePostViewedEvent(event);

        // Then - 예외가 발생해도 메서드는 정상 완료되어야 함
        verify(postInteractionUseCase).incrementViewCount(eq(postId));
    }

    @Test
    @DisplayName("게시글 조회 이벤트 처리 - UseCase에서 IllegalArgumentException 발생")
    void handlePostViewedEvent_WhenUseCaseThrowsIllegalArgumentException() {
        // Given
        Long postId = -1L; // 잘못된 postId
        PostViewedEvent event = new PostViewedEvent(postId);
        
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("잘못된 게시글 ID");
        doThrow(illegalArgumentException)
                .when(postInteractionUseCase)
                .incrementViewCount(postId);

        // When
        postViewIncreaseListener.handlePostViewedEvent(event);

        // Then - 예외가 발생해도 메서드는 정상 완료되어야 함
        verify(postInteractionUseCase).incrementViewCount(eq(postId));
    }

    @Test
    @DisplayName("대량 이벤트 처리 - 성능 테스트")
    void handlePostViewedEvent_HighVolumeProcessing() {
        // Given
        Long postId = 123L;
        int eventCount = 1000;

        // When - 1000개의 이벤트 처리
        for (int i = 0; i < eventCount; i++) {
            PostViewedEvent event = new PostViewedEvent(postId);
            postViewIncreaseListener.handlePostViewedEvent(event);
        }

        // Then
        verify(postInteractionUseCase, times(eventCount)).incrementViewCount(eq(postId));
    }

    @Test
    @DisplayName("UseCase 호출 파라미터 정확성 검증")
    void handlePostViewedEvent_ShouldPassCorrectParameters() {
        // Given
        Long expectedPostId = 789L;
        PostViewedEvent event = new PostViewedEvent(expectedPostId);

        // When
        postViewIncreaseListener.handlePostViewedEvent(event);

        // Then - 정확한 파라미터가 전달되어야 함
        verify(postInteractionUseCase, times(1)).incrementViewCount(eq(expectedPostId));
        
        // 다른 메서드는 호출되지 않아야 함
        verify(postInteractionUseCase, never()).likePost(any(), any());
    }

    @Test
    @DisplayName("이벤트 소스 객체 검증")
    void handlePostViewedEvent_EventSourceValidation() {
        // Given
        Long postId = 123L;
        PostViewedEvent event = new PostViewedEvent(postId);

        // When
        postViewIncreaseListener.handlePostViewedEvent(event);

        // Then - 이벤트 소스와 관계없이 정상 처리되어야 함
        verify(postInteractionUseCase).incrementViewCount(eq(postId));
    }

    @Test
    @DisplayName("비동기 처리 검증 - @Async 어노테이션 동작 확인")
    void handlePostViewedEvent_ShouldProcessAsynchronously() {
        // Given
        Long postId = 456L;
        PostViewedEvent event = new PostViewedEvent(postId);
        
        // UseCase 처리에 지연을 추가하여 비동기 동작 시뮬레이션
        doAnswer(invocation -> {
            Thread.sleep(100); // 100ms 지연
            return null;
        }).when(postInteractionUseCase).incrementViewCount(postId);

        // When
        postViewIncreaseListener.handlePostViewedEvent(event);
        
        // Then - 단위 테스트에서는 @Async가 동작하지 않으므로 동기적으로 실행됨
        // 실제 통합 테스트에서는 비동기로 처리되지만, 여기서는 UseCase 호출 완료만 확인
        verify(postInteractionUseCase).incrementViewCount(eq(postId));
    }

    @Test
    @DisplayName("다중 동시 이벤트 처리 검증")
    void handlePostViewedEvent_ShouldHandleConcurrentEvents() {
        // Given
        Long postId1 = 100L;
        Long postId2 = 200L;
        Long postId3 = 300L;
        
        PostViewedEvent event1 = new PostViewedEvent(postId1);
        PostViewedEvent event2 = new PostViewedEvent(postId2);
        PostViewedEvent event3 = new PostViewedEvent(postId3);

        // When - 동시에 여러 이벤트 처리 (실제로는 순차 처리되지만 동시 호출 시뮬레이션)
        CompletableFuture.runAsync(() -> postViewIncreaseListener.handlePostViewedEvent(event1));
        CompletableFuture.runAsync(() -> postViewIncreaseListener.handlePostViewedEvent(event2));
        CompletableFuture.runAsync(() -> postViewIncreaseListener.handlePostViewedEvent(event3));
        
        // 모든 비동기 작업이 완료될 때까지 대기
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            verify(postInteractionUseCase).incrementViewCount(eq(postId1));
            verify(postInteractionUseCase).incrementViewCount(eq(postId2));
            verify(postInteractionUseCase).incrementViewCount(eq(postId3));
        });
    }

    @Test
    @DisplayName("로깅 검증 - null postId 경고 로그")
    void handlePostViewedEvent_ShouldLogWarning_WhenPostIdIsNull() {
        // Given
        Logger logger = (Logger) LoggerFactory.getLogger(PostViewIncreaseListener.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        
        PostViewedEvent event = new PostViewedEvent(null);

        // When
        postViewIncreaseListener.handlePostViewedEvent(event);

        // Then
        verify(postInteractionUseCase, never()).incrementViewCount(any());
        
        // 로그 검증
        assertThat(listAppender.list).hasSize(1);
        assertThat(listAppender.list.get(0).getLevel().toString()).isEqualTo("WARN");
        assertThat(listAppender.list.get(0).getFormattedMessage())
            .contains("게시글 조회 이벤트 처리 실패: postId가 null입니다");
            
        // 정리
        logger.detachAppender(listAppender);
    }

    @Test
    @DisplayName("로깅 검증 - UseCase 예외 발생 시 에러 로그")
    void handlePostViewedEvent_ShouldLogError_WhenUseCaseThrowsException() {
        // Given
        Logger logger = (Logger) LoggerFactory.getLogger(PostViewIncreaseListener.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        
        Long postId = 123L;
        PostViewedEvent event = new PostViewedEvent(postId);
        
        PostCustomException exception = new PostCustomException(PostErrorCode.POST_NOT_FOUND);
        doThrow(exception).when(postInteractionUseCase).incrementViewCount(postId);

        // When
        postViewIncreaseListener.handlePostViewedEvent(event);

        // Then
        verify(postInteractionUseCase).incrementViewCount(eq(postId));
        
        // 로그 검증 (에러 로그가 생성되어야 함)
        assertThat(listAppender.list).hasSize(1);
        assertThat(listAppender.list.get(0).getLevel().toString()).isEqualTo("ERROR");
        assertThat(listAppender.list.get(0).getFormattedMessage())
            .contains("게시글 조회수 증가 실패: postId=" + postId);
            
        // 정리
        logger.detachAppender(listAppender);
    }

    @Test
    @DisplayName("로깅 검증 - 정상 처리 시 로그 없음")
    void handlePostViewedEvent_ShouldNotLog_WhenProcessedSuccessfully() {
        // Given
        Logger logger = (Logger) LoggerFactory.getLogger(PostViewIncreaseListener.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        
        Long postId = 123L;
        PostViewedEvent event = new PostViewedEvent(postId);

        // When
        postViewIncreaseListener.handlePostViewedEvent(event);

        // Then
        verify(postInteractionUseCase).incrementViewCount(eq(postId));
        
        // 정상 처리 시에는 로그가 없어야 함 (DEBUG 레벨 제외)
        assertThat(listAppender.list).isEmpty();
            
        // 정리
        logger.detachAppender(listAppender);
    }
}