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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doThrow;

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
    @DisplayName("이벤트 생성 시 유효성 검증 - null postId")
    void postViewedEventCreation_ShouldValidateNullPostId() {
        // When & Then - null postId로 이벤트 생성 시 예외 발생
        assertThatThrownBy(() -> new PostViewedEvent(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("게시글 ID는 null일 수 없습니다.");
    }

    @Test
    @DisplayName("대량 게시글 조회 이벤트 독립적 처리")
    void massPostViewedEvents_ShouldProcessAllIndependently() {
        // Given - 서로 다른 게시글들에 대한 조회 이벤트
        int postCount = 10;

        // When - 여러 게시글에 대한 조회 이벤트 발행
        for (int i = 1; i <= postCount; i++) {
            eventPublisher.publishEvent(new PostViewedEvent((long) i));
        }

        // Then - 모든 게시글이 독립적으로 조회수 증가 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    for (int i = 1; i <= postCount; i++) {
                        verify(postInteractionUseCase).incrementViewCount(eq((long) i));
                    }
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

    @Test
    @DisplayName("특수한 postId 값들의 조회 이벤트 처리")
    void postViewedEventWithSpecialPostIds_ShouldProcessCorrectly() {
        // Given - 경계값 및 특수한 postId들
        Long maxLong = Long.MAX_VALUE;
        Long minValidId = 1L;
        Long largeId = 999999999L;

        // When
        eventPublisher.publishEvent(new PostViewedEvent(maxLong));
        eventPublisher.publishEvent(new PostViewedEvent(minValidId));
        eventPublisher.publishEvent(new PostViewedEvent(largeId));

        // Then - 모든 특수한 ID 값들이 정상 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(postInteractionUseCase).incrementViewCount(eq(maxLong));
                    verify(postInteractionUseCase).incrementViewCount(eq(minValidId));
                    verify(postInteractionUseCase).incrementViewCount(eq(largeId));
                    verifyNoMoreInteractions(postInteractionUseCase);
                });
    }

    @Test
    @DisplayName("조회 이벤트와 다른 이벤트의 독립적 처리 검증")
    void postViewedEventIndependentProcessing_ShouldNotAffectOtherEvents() {
        // Given
        Long postId1 = 1L;
        Long postId2 = 2L;
        PostViewedEvent viewEvent1 = new PostViewedEvent(postId1);
        PostViewedEvent viewEvent2 = new PostViewedEvent(postId2);

        // When - 여러 조회 이벤트를 연속으로 발행
        eventPublisher.publishEvent(viewEvent1);
        eventPublisher.publishEvent(viewEvent2);

        // Then - 각 이벤트가 독립적으로 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(postInteractionUseCase).incrementViewCount(eq(postId1));
                    verify(postInteractionUseCase).incrementViewCount(eq(postId2));
                    verifyNoMoreInteractions(postInteractionUseCase);
                });
    }
}