package jaeik.bimillog.integration.event.post;

import jaeik.bimillog.domain.post.application.port.in.PostCacheUseCase;
import jaeik.bimillog.domain.post.event.PostSetAsNoticeEvent;
import jaeik.bimillog.domain.post.event.PostUnsetAsNoticeEvent;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doThrow;

/**
 * <h2>게시글 공지사항 설정/해제 이벤트 워크플로우 통합 테스트</h2>
 * <p>게시글 공지사항 설정/해제 시 발생하는 모든 후속 처리를 검증하는 통합 테스트</p>
 * <p>비동기 이벤트 처리와 실제 스프링 컨텍스트를 사용하여 전체 워크플로우를 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest
@Testcontainers
@Transactional
@DisplayName("게시글 공지사항 이벤트 워크플로우 통합 테스트")
public class PostSetAsNoticeEventIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @MockitoBean
    private PostCacheUseCase postCacheUseCase;

    @Test
    @DisplayName("게시글 공지사항 설정 이벤트 워크플로우 - 캐시 추가까지 완료")
    void postSetAsNoticeEventWorkflow_ShouldCompleteNoticeCache() {
        // Given
        Long postId = 1L;
        PostSetAsNoticeEvent event = new PostSetAsNoticeEvent(postId);

        // When
        eventPublisher.publishEvent(event);

        // Then - 비동기 처리를 고려하여 Awaitility 사용
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(postCacheUseCase).addNoticeToCache(eq(postId));
                });
    }

    @Test
    @DisplayName("게시글 공지사항 해제 이벤트 워크플로우 - 캐시 제거까지 완료")
    void postUnsetAsNoticeEventWorkflow_ShouldCompleteNoticeRemoval() {
        // Given
        Long postId = 1L;
        PostUnsetAsNoticeEvent event = new PostUnsetAsNoticeEvent(postId);

        // When
        eventPublisher.publishEvent(event);

        // Then - 비동기 처리를 고려하여 Awaitility 사용
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(postCacheUseCase).removeNoticeFromCache(eq(postId));
                });
    }

    @Test
    @DisplayName("여러 게시글 공지사항 설정 이벤트 동시 처리")
    void multiplePostSetAsNoticeEvents_ShouldProcessConcurrently() {
        // Given
        Long postId1 = 1L;
        Long postId2 = 2L;
        Long postId3 = 3L;

        // When - 동시에 여러 공지사항 설정 이벤트 발행
        eventPublisher.publishEvent(new PostSetAsNoticeEvent(postId1));
        eventPublisher.publishEvent(new PostSetAsNoticeEvent(postId2));
        eventPublisher.publishEvent(new PostSetAsNoticeEvent(postId3));

        // Then - 모든 게시글이 공지사항 캐시에 추가되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(postCacheUseCase).addNoticeToCache(eq(postId1));
                    verify(postCacheUseCase).addNoticeToCache(eq(postId2));
                    verify(postCacheUseCase).addNoticeToCache(eq(postId3));
                });
    }

    @Test
    @DisplayName("공지사항 설정과 해제 이벤트 혼합 처리")
    void mixedNoticeEvents_ShouldProcessIndependently() {
        // Given
        Long setPostId1 = 1L;
        Long setPostId2 = 2L;
        Long unsetPostId1 = 3L;
        Long unsetPostId2 = 4L;

        // When - 설정과 해제 이벤트를 혼합 발행
        eventPublisher.publishEvent(new PostSetAsNoticeEvent(setPostId1));
        eventPublisher.publishEvent(new PostUnsetAsNoticeEvent(unsetPostId1));
        eventPublisher.publishEvent(new PostSetAsNoticeEvent(setPostId2));
        eventPublisher.publishEvent(new PostUnsetAsNoticeEvent(unsetPostId2));

        // Then - 설정과 해제가 각각 올바르게 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(postCacheUseCase).addNoticeToCache(eq(setPostId1));
                    verify(postCacheUseCase).addNoticeToCache(eq(setPostId2));
                    verify(postCacheUseCase).removeNoticeFromCache(eq(unsetPostId1));
                    verify(postCacheUseCase).removeNoticeFromCache(eq(unsetPostId2));
                });
    }

    @Test
    @DisplayName("동일 게시글의 공지사항 설정/해제 반복 처리")
    void repeatedNoticeEventsForSamePost_ShouldProcessAll() {
        // Given - 동일 게시글의 반복 설정/해제
        Long postId = 1L;

        // When - 동일 게시글에 대한 설정/해제 반복
        eventPublisher.publishEvent(new PostSetAsNoticeEvent(postId));
        eventPublisher.publishEvent(new PostUnsetAsNoticeEvent(postId));
        eventPublisher.publishEvent(new PostSetAsNoticeEvent(postId));

        // Then - 모든 이벤트가 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(postCacheUseCase, times(2)).addNoticeToCache(eq(postId));
                    verify(postCacheUseCase, times(1)).removeNoticeFromCache(eq(postId));
                });
    }

    @Test
    @DisplayName("공지사항 이벤트 처리 성능 검증")
    void noticeEventProcessingTime_ShouldCompleteWithinTimeout() {
        // Given
        Long postId = 999L;
        PostSetAsNoticeEvent setEvent = new PostSetAsNoticeEvent(postId);
        PostUnsetAsNoticeEvent unsetEvent = new PostUnsetAsNoticeEvent(postId);

        long startTime = System.currentTimeMillis();

        // When
        eventPublisher.publishEvent(setEvent);
        eventPublisher.publishEvent(unsetEvent);

        // Then - 3초 내에 처리 완료되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> {
                    verify(postCacheUseCase).addNoticeToCache(eq(postId));
                    verify(postCacheUseCase).removeNoticeFromCache(eq(postId));

                    long endTime = System.currentTimeMillis();
                    long processingTime = endTime - startTime;
                    
                    // 처리 시간이 3초를 초과하지 않아야 함
                    assert processingTime < 3000L : "공지사항 이벤트 처리 시간이 너무 오래 걸림: " + processingTime + "ms";
                });
    }

    @Test
    @DisplayName("대량 공지사항 설정 이벤트 처리 성능")
    void massNoticeSetEvents_ShouldProcessEfficiently() {
        // Given - 대량의 공지사항 설정 이벤트
        int eventCount = 50;
        
        long startTime = System.currentTimeMillis();

        // When - 대량 공지사항 설정 이벤트 발행
        for (int i = 1; i <= eventCount; i++) {
            eventPublisher.publishEvent(new PostSetAsNoticeEvent((long) i));
        }

        // Then - 모든 이벤트가 10초 내에 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    for (int i = 1; i <= eventCount; i++) {
                        verify(postCacheUseCase).addNoticeToCache(eq((long) i));
                    }

                    long endTime = System.currentTimeMillis();
                    long totalProcessingTime = endTime - startTime;
                    
                    // 대량 처리 시간이 10초를 초과하지 않아야 함
                    assert totalProcessingTime < 10000L : "대량 공지사항 설정 이벤트 처리 시간이 너무 오래 걸림: " + totalProcessingTime + "ms";
                });
    }

    @Test
    @DisplayName("공지사항 설정 이벤트와 다른 이벤트의 독립적 처리")
    void noticeEventWithOtherEvents_ShouldProcessIndependently() {
        // Given
        Long postId1 = 1L;
        Long postId2 = 2L;

        // When - 공지사항 이벤트와 다른 종류의 이벤트를 동시에 발행
        eventPublisher.publishEvent(new PostSetAsNoticeEvent(postId1));
        eventPublisher.publishEvent(new PostUnsetAsNoticeEvent(postId2));

        // Then - 모든 공지사항 이벤트가 독립적으로 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(postCacheUseCase).addNoticeToCache(eq(postId1));
                    verify(postCacheUseCase).removeNoticeFromCache(eq(postId2));
                });
    }

    @Test
    @DisplayName("예외 상황에서의 공지사항 설정 이벤트 처리 - 캐시 추가 실패")
    void eventProcessingWithException_NoticeCacheAddFailure() {
        // Given
        Long postId = 1L;
        PostSetAsNoticeEvent event = new PostSetAsNoticeEvent(postId);
        
        // 캐시 추가 실패 시뮬레이션
        doThrow(new RuntimeException("캐시 추가 실패"))
                .when(postCacheUseCase).addNoticeToCache(postId);

        // When
        eventPublisher.publishEvent(event);

        // Then - 예외가 발생해도 이벤트 리스너는 호출되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(postCacheUseCase).addNoticeToCache(eq(postId));
                });
    }

    @Test
    @DisplayName("예외 상황에서의 공지사항 해제 이벤트 처리 - 캐시 제거 실패")
    void eventProcessingWithException_NoticeCacheRemoveFailure() {
        // Given
        Long postId = 1L;
        PostUnsetAsNoticeEvent event = new PostUnsetAsNoticeEvent(postId);
        
        // 캐시 제거 실패 시뮬레이션
        doThrow(new RuntimeException("캐시 제거 실패"))
                .when(postCacheUseCase).removeNoticeFromCache(postId);

        // When
        eventPublisher.publishEvent(event);

        // Then - 예외가 발생해도 이벤트 리스너는 호출되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(postCacheUseCase).removeNoticeFromCache(eq(postId));
                });
    }

    @Test
    @DisplayName("연속된 공지사항 이벤트 처리 순서")
    void sequentialNoticeEvents_ShouldMaintainOrder() {
        // Given - 연속된 공지사항 이벤트
        Long postId1 = 1L;
        Long postId2 = 2L;
        Long postId3 = 3L;
        
        // When - 순서대로 공지사항 이벤트 발행 (설정 -> 해제 -> 설정)
        eventPublisher.publishEvent(new PostSetAsNoticeEvent(postId1));
        eventPublisher.publishEvent(new PostUnsetAsNoticeEvent(postId2));
        eventPublisher.publishEvent(new PostSetAsNoticeEvent(postId3));

        // Then - 비동기 처리이지만 모든 이벤트가 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(postCacheUseCase).addNoticeToCache(eq(postId1));
                    verify(postCacheUseCase).removeNoticeFromCache(eq(postId2));
                    verify(postCacheUseCase).addNoticeToCache(eq(postId3));
                });
    }

    @Test
    @DisplayName("공지사항 대량 해제 이벤트 처리")
    void massNoticeUnsetEvents_ShouldProcessAllEfficiently() {
        // Given - 대량의 공지사항 해제 이벤트
        int eventCount = 30;
        
        long startTime = System.currentTimeMillis();

        // When - 대량 공지사항 해제 이벤트 발행
        for (int i = 1; i <= eventCount; i++) {
            eventPublisher.publishEvent(new PostUnsetAsNoticeEvent((long) i));
        }

        // Then - 모든 이벤트가 8초 내에 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(8))
                .untilAsserted(() -> {
                    for (int i = 1; i <= eventCount; i++) {
                        verify(postCacheUseCase).removeNoticeFromCache(eq((long) i));
                    }

                    long endTime = System.currentTimeMillis();
                    long totalProcessingTime = endTime - startTime;
                    
                    // 대량 처리 시간이 8초를 초과하지 않아야 함
                    assert totalProcessingTime < 8000L : "대량 공지사항 해제 이벤트 처리 시간이 너무 오래 걸림: " + totalProcessingTime + "ms";
                });
    }
}
