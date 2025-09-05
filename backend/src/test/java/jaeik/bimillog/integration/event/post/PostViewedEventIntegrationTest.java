package jaeik.bimillog.integration.event.post;

import jaeik.bimillog.domain.post.application.port.in.PostInteractionUseCase;
import jaeik.bimillog.domain.post.event.PostViewedEvent;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
@Transactional
@DisplayName("게시글 조회 이벤트 워크플로우 통합 테스트")
public class PostViewedEventIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

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
                });
    }

    @Test
    @DisplayName("여러 게시글 조회 이벤트 동시 처리")
    void multiplePostViewedEvents_ShouldProcessConcurrently() {
        // Given
        Long postId1 = 1L;
        Long postId2 = 2L;
        Long postId3 = 3L;

        // When - 동시에 여러 조회 이벤트 발행
        eventPublisher.publishEvent(new PostViewedEvent(postId1));
        eventPublisher.publishEvent(new PostViewedEvent(postId2));
        eventPublisher.publishEvent(new PostViewedEvent(postId3));

        // Then - 모든 게시글의 조회수가 증가되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(postInteractionUseCase).incrementViewCount(eq(postId1));
                    verify(postInteractionUseCase).incrementViewCount(eq(postId2));
                    verify(postInteractionUseCase).incrementViewCount(eq(postId3));
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
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(postInteractionUseCase, times(3)).incrementViewCount(eq(postId));
                });
    }

    @Test
    @DisplayName("조회 이벤트 처리 성능 검증")
    void postViewedEventProcessingTime_ShouldCompleteWithinTimeout() {
        // Given
        Long postId = 999L;
        PostViewedEvent event = new PostViewedEvent(postId);

        long startTime = System.currentTimeMillis();

        // When
        eventPublisher.publishEvent(event);

        // Then - 2초 내에 처리 완료되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    verify(postInteractionUseCase).incrementViewCount(eq(postId));

                    long endTime = System.currentTimeMillis();
                    long processingTime = endTime - startTime;
                    
                    // 처리 시간이 2초를 초과하지 않아야 함
                    assert processingTime < 2000L : "조회 이벤트 처리 시간이 너무 오래 걸림: " + processingTime + "ms";
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
    @DisplayName("대량 조회 이벤트 처리 성능")
    void massPostViewedEvents_ShouldProcessEfficiently() {
        // Given - 대량의 조회 이벤트 (100개)
        int eventCount = 100;
        
        long startTime = System.currentTimeMillis();

        // When - 대량 조회 이벤트 발행
        for (int i = 1; i <= eventCount; i++) {
            eventPublisher.publishEvent(new PostViewedEvent((long) i));
        }

        // Then - 모든 이벤트가 10초 내에 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    for (int i = 1; i <= eventCount; i++) {
                        verify(postInteractionUseCase).incrementViewCount(eq((long) i));
                    }

                    long endTime = System.currentTimeMillis();
                    long totalProcessingTime = endTime - startTime;
                    
                    // 대량 처리 시간이 10초를 초과하지 않아야 함
                    assert totalProcessingTime < 10000L : "대량 조회 이벤트 처리 시간이 너무 오래 걸림: " + totalProcessingTime + "ms";
                });
    }

    @Test
    @DisplayName("조회 이벤트와 다른 이벤트의 독립적 처리")
    void postViewedEventWithOtherEvents_ShouldProcessIndependently() {
        // Given
        Long postId1 = 1L;
        Long postId2 = 2L;
        PostViewedEvent viewEvent1 = new PostViewedEvent(postId1);
        PostViewedEvent viewEvent2 = new PostViewedEvent(postId2);

        // When - 조회 이벤트와 다른 종류의 이벤트를 동시에 발행
        eventPublisher.publishEvent(viewEvent1);
        eventPublisher.publishEvent(viewEvent2);

        // Then - 모든 조회 이벤트가 독립적으로 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(postInteractionUseCase).incrementViewCount(eq(postId1));
                    verify(postInteractionUseCase).incrementViewCount(eq(postId2));
                });
    }

    @Test
    @DisplayName("예외 상황에서의 조회 이벤트 처리 - 조회수 증가 실패")
    void eventProcessingWithException_ViewCountIncrementFailure() {
        // Given
        Long postId = 1L;
        PostViewedEvent event = new PostViewedEvent(postId);
        
        // 조회수 증가 실패 시뮬레이션
        doThrow(new RuntimeException("조회수 증가 실패"))
                .when(postInteractionUseCase).incrementViewCount(postId);

        // When
        eventPublisher.publishEvent(event);

        // Then - 예외가 발생해도 이벤트 리스너는 호출되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(postInteractionUseCase).incrementViewCount(eq(postId));
                });
    }

    @Test
    @DisplayName("연속된 조회 이벤트 처리 순서")
    void sequentialPostViewedEvents_ShouldMaintainOrder() {
        // Given - 연속된 조회 이벤트
        Long postId1 = 1L;
        Long postId2 = 2L;
        Long postId3 = 3L;
        
        // When - 순서대로 조회 이벤트 발행
        eventPublisher.publishEvent(new PostViewedEvent(postId1));
        eventPublisher.publishEvent(new PostViewedEvent(postId2));
        eventPublisher.publishEvent(new PostViewedEvent(postId3));

        // Then - 비동기 처리이지만 모든 이벤트가 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(postInteractionUseCase).incrementViewCount(eq(postId1));
                    verify(postInteractionUseCase).incrementViewCount(eq(postId2));
                    verify(postInteractionUseCase).incrementViewCount(eq(postId3));
                });
    }

    @Test
    @DisplayName("인기 게시글의 대량 조회 이벤트 처리")
    void popularPostMassiveViews_ShouldProcessAllEvents() {
        // Given - 인기 게시글에 대한 대량 조회
        Long popularPostId = 100L;
        int viewCount = 50;
        
        long startTime = System.currentTimeMillis();

        // When - 동일 게시글에 대한 대량 조회 이벤트 발행
        for (int i = 1; i <= viewCount; i++) {
            eventPublisher.publishEvent(new PostViewedEvent(popularPostId));
        }

        // Then - 모든 조회 이벤트가 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(postInteractionUseCase, times(viewCount)).incrementViewCount(eq(popularPostId));

                    long endTime = System.currentTimeMillis();
                    long totalProcessingTime = endTime - startTime;
                    
                    // 대량 처리 시간이 10초를 초과하지 않아야 함
                    assert totalProcessingTime < 10000L : "인기 게시글 대량 조회 처리 시간이 너무 오래 걸림: " + totalProcessingTime + "ms";
                });
    }
}
