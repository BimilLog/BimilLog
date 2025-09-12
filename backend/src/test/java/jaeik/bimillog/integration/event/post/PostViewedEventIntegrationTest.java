package jaeik.bimillog.integration.event.post;

import jaeik.bimillog.domain.post.application.port.in.PostInteractionUseCase;
import jaeik.bimillog.domain.post.event.PostViewedEvent;
import jaeik.bimillog.testutil.TestContainersConfiguration;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * <h2>게시글 조회 이벤트 워크플로우 통합 테스트</h2>
 * <p>게시글 조회 시 발생하는 모든 후속 처리를 검증하는 통합 테스트</p>
 * <p>비동기 이벤트 처리와 실제 스프링 컨텍스트를 사용하여 전체 워크플로우를 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest
@Testcontainers
@Import(TestContainersConfiguration.class)
@Transactional
@DisplayName("게시글 조회 이벤트 워크플로우 통합 테스트")
public class PostViewedEventIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @MockitoBean
    private PostInteractionUseCase postInteractionUseCase;

    @Test
    @DisplayName("게시글 조회 이벤트 워크플로우 - 조회수 증가까지 완료")
    void postViewedEventWorkflow_ShouldCompleteViewCountIncrement() {
        // Given
        Long postId = 1L;
        PostViewedEvent event = new PostViewedEvent(postId);

        // When
        eventPublisher.publishEvent(event);

        // Then - 비동기 처리를 고려하여 Awaitility 사용
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(postInteractionUseCase).incrementViewCount(eq(postId));
                    verifyNoMoreInteractions(postInteractionUseCase);
                });
    }

    @Test
    @DisplayName("여러 다른 게시글 조회 이벤트 동시 처리")
    void multipleDifferentPostViewedEvents_ShouldProcessIndependently() {
        // Given
        Long postId1 = 1L;
        Long postId2 = 2L;
        Long postId3 = 3L;

        // When - 동시에 여러 조회 이벤트 발행
        eventPublisher.publishEvent(new PostViewedEvent(postId1));
        eventPublisher.publishEvent(new PostViewedEvent(postId2));
        eventPublisher.publishEvent(new PostViewedEvent(postId3));

        // Then - 모든 게시글의 조회수가 독립적으로 증가되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(postInteractionUseCase).incrementViewCount(eq(postId1));
                    verify(postInteractionUseCase).incrementViewCount(eq(postId2));
                    verify(postInteractionUseCase).incrementViewCount(eq(postId3));
                    verifyNoMoreInteractions(postInteractionUseCase);
                });
    }

    @Test
    @DisplayName("동일 게시글의 여러 조회 이벤트 처리")
    void multipleViewEventsForSamePost_ShouldProcessAll() {
        // Given - 동일 게시글의 여러 조회 (여러 사용자가 동시 조회)
        Long postId = 1L;

        // When - 동일 게시글에 대한 조회 이벤트 여러 번 발행
        eventPublisher.publishEvent(new PostViewedEvent(postId));
        eventPublisher.publishEvent(new PostViewedEvent(postId));
        eventPublisher.publishEvent(new PostViewedEvent(postId));

        // Then - 모든 조회 이벤트가 처리되어 조회수가 3회 증가되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(postInteractionUseCase, times(3)).incrementViewCount(eq(postId));
                    verifyNoMoreInteractions(postInteractionUseCase);
                });
    }

    @Test
    @DisplayName("조회수 증가 실패 시 예외 처리")
    void postViewedEventWithException_ShouldHandleGracefully() {
        // Given
        Long postId = 1L;
        PostViewedEvent event = new PostViewedEvent(postId);
        
        // 조회수 증가 실패 시뮬레이션
        doThrow(new RuntimeException("조회수 증가 실패"))
                .when(postInteractionUseCase).incrementViewCount(postId);

        // When
        eventPublisher.publishEvent(event);

        // Then - 예외가 발생해도 이벤트 리스너는 호출되고 예외를 삼켜야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(postInteractionUseCase).incrementViewCount(eq(postId));
                    verifyNoMoreInteractions(postInteractionUseCase);
                });
    }

    @Test
    @DisplayName("비동기 이벤트 리스너 정상 작동 검증")
    void postViewedEventAsync_ShouldTriggerListenerCorrectly() {
        // Given
        Long postId = 999L;
        PostViewedEvent event = new PostViewedEvent(postId);

        // When
        eventPublisher.publishEvent(event);

        // Then - 비동기 처리가 정상 완료되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> {
                    verify(postInteractionUseCase).incrementViewCount(eq(postId));
                    verifyNoMoreInteractions(postInteractionUseCase);
                });
    }
}