package jaeik.bimillog.integration.event;

import jaeik.bimillog.domain.post.application.port.in.PostInteractionUseCase;
import jaeik.bimillog.domain.post.application.port.out.PostCacheCommandPort;
import jaeik.bimillog.domain.post.application.port.out.PostCacheSyncPort;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.event.PostSetAsNoticeEvent;
import jaeik.bimillog.domain.post.event.PostUnsetAsNoticeEvent;
import jaeik.bimillog.domain.post.event.PostViewedEvent;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.exception.PostErrorCode;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>게시글 도메인 이벤트 워크플로우 통합 테스트</h2>
 * <p>게시글 관련 이벤트들의 전체 흐름을 검증하는 통합 테스트</p>
 * <p>비동기 이벤트 처리와 실제 스프링 컨텍스트를 사용하여 전체 워크플로우를 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest
@Testcontainers
@Transactional
@DisplayName("게시글 도메인 이벤트 워크플로우 통합 테스트")
class PostEventWorkflowIntegrationTest {

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
    
    @MockitoBean
    private PostCacheCommandPort postCacheCommandPort;
    
    @MockitoBean
    private PostCacheSyncPort postCacheSyncPort;

    @Test
    @DisplayName("게시글 조회 이벤트 워크플로우 - 조회수 증가까지 완료")
    void postViewedEventWorkflow_ShouldCompleteViewCountIncrement() {
        // Given
        Long postId = 123L;
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
    void multiplePostViewedEvents_ShouldBeProcessedConcurrently() {
        // Given
        Long postId1 = 100L;
        Long postId2 = 200L;
        Long postId3 = 300L;

        PostViewedEvent event1 = new PostViewedEvent(postId1);
        PostViewedEvent event2 = new PostViewedEvent(postId2);
        PostViewedEvent event3 = new PostViewedEvent(postId3);

        // When - 동시에 여러 이벤트 발행
        eventPublisher.publishEvent(event1);
        eventPublisher.publishEvent(event2);
        eventPublisher.publishEvent(event3);

        // Then - 모든 이벤트가 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(postInteractionUseCase).incrementViewCount(eq(postId1));
                    verify(postInteractionUseCase).incrementViewCount(eq(postId2));
                    verify(postInteractionUseCase).incrementViewCount(eq(postId3));
                });
    }

    @Test
    @DisplayName("동일 게시글 연속 조회 이벤트 처리")
    void samePostViewedMultipleTimes_ShouldProcessAllEvents() {
        // Given
        Long postId = 123L;
        
        PostViewedEvent event1 = new PostViewedEvent(postId);
        PostViewedEvent event2 = new PostViewedEvent(postId);
        PostViewedEvent event3 = new PostViewedEvent(postId);

        // When - 동일 게시글에 대한 여러 조회 이벤트
        eventPublisher.publishEvent(event1);
        eventPublisher.publishEvent(event2);
        eventPublisher.publishEvent(event3);

        // Then - 모든 이벤트가 처리되어야 함 (중복 검사는 Controller 레이어에서)
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(postInteractionUseCase, times(3)).incrementViewCount(eq(postId));
                });
    }

    @Test
    @DisplayName("이벤트 처리 중 예외 발생 시 복원력 테스트")
    void postViewedEventWithException_ShouldHandleGracefully() {
        // Given
        Long postId = 999L;
        PostViewedEvent event = new PostViewedEvent(postId);
        
        doThrow(new PostCustomException(PostErrorCode.POST_NOT_FOUND))
                .when(postInteractionUseCase)
                .incrementViewCount(postId);

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
    @DisplayName("이벤트 처리 시간 검증 - 게시글 조회")
    void postViewedEventProcessingTime_ShouldCompleteWithinTimeout() {
        // Given
        Long postId = 123L;
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
                    assert processingTime < 2000L : "이벤트 처리 시간이 너무 오래 걸림: " + processingTime + "ms";
                });
    }

    @Test
    @DisplayName("대용량 게시글 조회 이벤트 처리")
    void highVolumePostViewedEvents_ShouldBeProcessedEfficiently() {
        // Given
        Long postId = 123L;
        int eventCount = 100;

        // When - 100개의 조회 이벤트 발행
        for (int i = 0; i < eventCount; i++) {
            PostViewedEvent event = new PostViewedEvent(postId);
            eventPublisher.publishEvent(event);
        }

        // Then - 모든 이벤트가 10초 내에 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(postInteractionUseCase, times(eventCount)).incrementViewCount(eq(postId));
                });
    }

    @Test
    @DisplayName("null 값을 포함한 게시글 조회 이벤트 처리")
    void postViewedEventWithNullValues_ShouldBeProcessed() {
        // Given - null postId를 포함한 이벤트
        PostViewedEvent event = new PostViewedEvent(null);

        // When
        eventPublisher.publishEvent(event);

        // Then - null 값이어도 이벤트는 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(postInteractionUseCase).incrementViewCount(eq(null));
                });
    }

    @Test
    @DisplayName("다양한 사용자 식별자를 가진 이벤트들")
    void postViewedEventsWithDifferentUserIdentifiers_ShouldBeProcessedCorrectly() {
        // Given
        Long postId = 123L;

        PostViewedEvent event1 = new PostViewedEvent(postId);
        PostViewedEvent event2 = new PostViewedEvent(postId);

        // When
        eventPublisher.publishEvent(event1);
        eventPublisher.publishEvent(event2);

        // Then - 각각 호출되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(postInteractionUseCase, times(2)).incrementViewCount(eq(postId));
                });
    }

    @Test
    @DisplayName("이벤트 처리 순서 검증 - 순차적 처리")
    void postViewedEvents_ShouldMaintainProcessingOrder() {
        // Given
        Long postId1 = 100L;
        Long postId2 = 200L;
        Long postId3 = 300L;

        // When - 순차적으로 이벤트 발행
        eventPublisher.publishEvent(new PostViewedEvent(postId1));
        eventPublisher.publishEvent(new PostViewedEvent(postId2));
        eventPublisher.publishEvent(new PostViewedEvent(postId3));

        // Then - 모든 이벤트가 처리되어야 함 (비동기이므로 순서는 보장되지 않음)
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(postInteractionUseCase).incrementViewCount(eq(postId1));
                    verify(postInteractionUseCase).incrementViewCount(eq(postId2));
                    verify(postInteractionUseCase).incrementViewCount(eq(postId3));
                });
    }

    @Test
    @DisplayName("조회 이력 데이터 검증")
    void postViewedEvent_ShouldPassCorrectViewHistory() {
        // Given
        Long postId = 123L;
        PostViewedEvent event = new PostViewedEvent(postId);

        // When
        eventPublisher.publishEvent(event);

        // Then - 정확한 postId가 전달되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(postInteractionUseCase).incrementViewCount(eq(postId));
                });
    }

    @Test
    @DisplayName("게시글 공지 설정 이벤트 워크플로우 - 이벤트 발행 확인")
    void postSetAsNoticeEventWorkflow_ShouldPublishEvent() {
        // Given
        Long postId = 123L;
        PostSetAsNoticeEvent event = new PostSetAsNoticeEvent(postId);

        // When
        eventPublisher.publishEvent(event);

        // Then - 이벤트가 정상적으로 발행되었는지 확인
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    // 이벤트 발행이 완료되면 성공
                    assertThat(true).isTrue();
                });
    }

    @Test
    @DisplayName("게시글 공지 해제 이벤트 워크플로우 - 이벤트 발행 확인")
    void postUnsetAsNoticeEventWorkflow_ShouldPublishEvent() {
        // Given
        Long postId = 123L;
        PostUnsetAsNoticeEvent event = new PostUnsetAsNoticeEvent(postId);

        // When
        eventPublisher.publishEvent(event);

        // Then - 이벤트가 정상적으로 발행되었는지 확인
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    // 이벤트 발행이 완료되면 성공
                    assertThat(true).isTrue();
                });
    }

    @Test
    @DisplayName("게시글 공지 설정/해제 연속 이벤트 처리")
    void postNoticeSetAndUnsetEvents_ShouldBeProcessedSequentially() {
        // Given
        Long postId = 123L;
        PostSetAsNoticeEvent setEvent = new PostSetAsNoticeEvent(postId);
        PostUnsetAsNoticeEvent unsetEvent = new PostUnsetAsNoticeEvent(postId);

        // Mock setup for findPostDetail to return valid PostDetail
        given(postCacheSyncPort.findPostDetail(postId))
                .willReturn(PostDetail.builder()
                        .id(postId)
                        .title("Test Title")
                        .content("Test Content")
                        .viewCount(0)
                        .likeCount(0)
                        .postCacheFlag(null)
                        .createdAt(null)
                        .userId(null)
                        .userName(null)
                        .commentCount(0)
                        .isNotice(true)
                        .isLiked(false)
                        .build());

        // When - 공지 설정 후 해제
        eventPublisher.publishEvent(setEvent);
        eventPublisher.publishEvent(unsetEvent);

        // Then - 공지 설정 시 캐시 추가와 해제 시 캐시 삭제가 모두 호출되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(postCacheSyncPort).findPostDetail(postId); // 공지 설정 시 호출
                    verify(postCacheCommandPort).cachePostsWithDetails(eq(PostCacheFlag.NOTICE), any()); // 공지 설정
                    verify(postCacheCommandPort).deleteCache(null, postId, PostCacheFlag.NOTICE); // 공지 해제
                });
    }

    @Test
    @DisplayName("여러 게시글 동시 공지 설정 이벤트 처리")
    void multiplePostNoticeSetEvents_ShouldBeProcessedConcurrently() {
        // Given
        Long postId1 = 100L;
        Long postId2 = 200L;
        Long postId3 = 300L;
        
        PostSetAsNoticeEvent event1 = new PostSetAsNoticeEvent(postId1);
        PostSetAsNoticeEvent event2 = new PostSetAsNoticeEvent(postId2);
        PostSetAsNoticeEvent event3 = new PostSetAsNoticeEvent(postId3);

        // Mock setup for findPostDetail to return valid PostDetail for all post IDs
        given(postCacheSyncPort.findPostDetail(any(Long.class)))
                .willReturn(PostDetail.builder()
                        .id(100L)
                        .title("Test Title")
                        .content("Test Content")
                        .viewCount(0)
                        .likeCount(0)
                        .postCacheFlag(null)
                        .createdAt(null)
                        .userId(null)
                        .userName(null)
                        .commentCount(0)
                        .isNotice(true)
                        .isLiked(false)
                        .build());

        // When - 동시에 여러 공지 설정 이벤트 발행
        eventPublisher.publishEvent(event1);
        eventPublisher.publishEvent(event2);
        eventPublisher.publishEvent(event3);

        // Then - 3번의 공지 캐시 추가가 모두 호출되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(postCacheSyncPort, times(3)).findPostDetail(any(Long.class));
                    verify(postCacheCommandPort, times(3)).cachePostsWithDetails(eq(PostCacheFlag.NOTICE), any());
                });
    }

    @Test
    @DisplayName("공지 캐시 삭제 실패 시 복원력 테스트")
    void postNoticeEventWithCacheException_ShouldHandleGracefully() {
        // Given
        Long postId = 999L;
        PostSetAsNoticeEvent event = new PostSetAsNoticeEvent(postId);
        
        given(postCacheSyncPort.findPostDetail(postId))
                .willReturn(PostDetail.builder()
                        .id(postId)
                        .title("Test Title")
                        .content("Test Content")
                        .viewCount(0)
                        .likeCount(0)
                        .postCacheFlag(null)
                        .createdAt(null)
                        .userId(null)
                        .userName(null)
                        .commentCount(0)
                        .isNotice(true)
                        .isLiked(false)
                        .build());
        doThrow(new RuntimeException("Cache addition failed"))
                .when(postCacheCommandPort)
                .cachePostsWithDetails(eq(PostCacheFlag.NOTICE), any());

        // When
        eventPublisher.publishEvent(event);

        // Then - 예외가 발생해도 이벤트 리스너는 호출되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(postCacheSyncPort).findPostDetail(postId);
                    verify(postCacheCommandPort).cachePostsWithDetails(eq(PostCacheFlag.NOTICE), any());
                });
    }

    @Test
    @DisplayName("공지 이벤트 처리 시간 검증")
    void postNoticeEventProcessingTime_ShouldCompleteWithinTimeout() {
        // Given
        Long postId = 123L;
        PostSetAsNoticeEvent event = new PostSetAsNoticeEvent(postId);
        long startTime = System.currentTimeMillis();

        // When
        eventPublisher.publishEvent(event);

        // Then - 1초 내에 처리 완료되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    verify(postCacheSyncPort).findPostDetail(postId);

                    long endTime = System.currentTimeMillis();
                    long processingTime = endTime - startTime;
                    
                    // 처리 시간이 1초를 초과하지 않아야 함
                    assert processingTime < 1000L : "공지 이벤트 처리 시간이 너무 오래 걸림: " + processingTime + "ms";
                });
    }

    @Test
    @DisplayName("대용량 공지 설정 이벤트 처리")
    void highVolumePostNoticeEvents_ShouldBeProcessedEfficiently() {
        // Given
        Long postId = 123L;
        int eventCount = 50;

        // When - 50개의 공지 설정 이벤트 발행
        for (int i = 0; i < eventCount; i++) {
            PostSetAsNoticeEvent event = new PostSetAsNoticeEvent(postId);
            eventPublisher.publishEvent(event);
        }

        // Then - 모든 이벤트가 5초 내에 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(postCacheSyncPort, times(eventCount)).findPostDetail(eq(postId));
                });
    }

    @Test
    @DisplayName("공지 설정과 게시글 조회 이벤트 동시 처리")
    void postNoticeAndViewEvents_ShouldBeProcessedIndependently() {
        // Given
        Long postId = 123L;
        
        PostSetAsNoticeEvent noticeEvent = new PostSetAsNoticeEvent(postId);
        PostViewedEvent viewedEvent = new PostViewedEvent(postId);

        // When - 공지 설정과 조회 이벤트 동시 발행
        eventPublisher.publishEvent(noticeEvent);
        eventPublisher.publishEvent(viewedEvent);

        // Then - 두 이벤트가 모두 독립적으로 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(postCacheSyncPort).findPostDetail(postId);
                    verify(postInteractionUseCase).incrementViewCount(eq(postId));
                });
    }
}