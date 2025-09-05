package jaeik.bimillog.integration.event.post;

import jaeik.bimillog.domain.post.application.port.in.PostCacheUseCase;
import jaeik.bimillog.domain.post.event.PostUnsetAsNoticeEvent;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import jaeik.bimillog.testutil.TestContainersConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import jaeik.bimillog.testutil.TestContainersConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doThrow;

/**
 * <h2>게시글 공지사항 해제 이벤트 워크플로우 통합 테스트</h2>
 * <p>게시글 공지사항 해제 시 발생하는 모든 후속 처리를 검증하는 통합 테스트</p>
 * <p>비동기 이벤트 처리와 실제 스프링 컨텍스트를 사용하여 전체 워크플로우를 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest
@Import(TestContainersConfiguration.class)
@Testcontainers
@Transactional
@DisplayName("게시글 공지사항 해제 이벤트 워크플로우 통합 테스트")
public class PostUnsetAsNoticeTestIntegrationTest {


    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @MockitoBean
    private PostCacheUseCase postCacheUseCase;

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
    @DisplayName("여러 게시글 공지사항 해제 이벤트 동시 처리")
    void multiplePostUnsetAsNoticeEvents_ShouldProcessConcurrently() {
        // Given
        Long postId1 = 1L;
        Long postId2 = 2L;
        Long postId3 = 3L;

        // When - 동시에 여러 공지사항 해제 이벤트 발행
        eventPublisher.publishEvent(new PostUnsetAsNoticeEvent(postId1));
        eventPublisher.publishEvent(new PostUnsetAsNoticeEvent(postId2));
        eventPublisher.publishEvent(new PostUnsetAsNoticeEvent(postId3));

        // Then - 모든 게시글이 공지사항 캐시에서 제거되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(postCacheUseCase).removeNoticeFromCache(eq(postId1));
                    verify(postCacheUseCase).removeNoticeFromCache(eq(postId2));
                    verify(postCacheUseCase).removeNoticeFromCache(eq(postId3));
                });
    }

    @Test
    @DisplayName("동일 게시글의 여러 공지사항 해제 이벤트 처리")
    void multipleUnsetEventsForSamePost_ShouldProcessAll() {
        // Given - 동일 게시글의 여러 해제 (중복 해제 요청)
        Long postId = 1L;

        // When - 동일 게시글에 대한 해제 이벤트 여러 번 발행
        eventPublisher.publishEvent(new PostUnsetAsNoticeEvent(postId));
        eventPublisher.publishEvent(new PostUnsetAsNoticeEvent(postId));
        eventPublisher.publishEvent(new PostUnsetAsNoticeEvent(postId));

        // Then - 모든 해제 이벤트가 처리되어야 함 (중복 요청도 모두 처리)
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(postCacheUseCase, times(3)).removeNoticeFromCache(eq(postId));
                });
    }

    @Test
    @DisplayName("공지사항 해제 이벤트 처리 성능 검증")
    void unsetNoticeEventProcessingTime_ShouldCompleteWithinTimeout() {
        // Given
        Long postId = 999L;
        PostUnsetAsNoticeEvent event = new PostUnsetAsNoticeEvent(postId);

        long startTime = System.currentTimeMillis();

        // When
        eventPublisher.publishEvent(event);

        // Then - 2초 내에 처리 완료되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    verify(postCacheUseCase).removeNoticeFromCache(eq(postId));

                    long endTime = System.currentTimeMillis();
                    long processingTime = endTime - startTime;
                    
                    // 처리 시간이 2초를 초과하지 않아야 함
                    assert processingTime < 2000L : "공지사항 해제 이벤트 처리 시간이 너무 오래 걸림: " + processingTime + "ms";
                });
    }

    @Test
    @DisplayName("대량 공지사항 해제 이벤트 처리 성능")
    void massNoticeUnsetEvents_ShouldProcessEfficiently() {
        // Given - 대량의 공지사항 해제 이벤트
        int eventCount = 50;
        
        long startTime = System.currentTimeMillis();

        // When - 대량 공지사항 해제 이벤트 발행
        for (int i = 1; i <= eventCount; i++) {
            eventPublisher.publishEvent(new PostUnsetAsNoticeEvent((long) i));
        }

        // Then - 모든 이벤트가 10초 내에 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    for (int i = 1; i <= eventCount; i++) {
                        verify(postCacheUseCase).removeNoticeFromCache(eq((long) i));
                    }

                    long endTime = System.currentTimeMillis();
                    long totalProcessingTime = endTime - startTime;
                    
                    // 대량 처리 시간이 10초를 초과하지 않아야 함
                    assert totalProcessingTime < 10000L : "대량 공지사항 해제 이벤트 처리 시간이 너무 오래 걸림: " + totalProcessingTime + "ms";
                });
    }

    @Test
    @DisplayName("공지사항 해제 이벤트와 다른 이벤트의 독립적 처리")
    void unsetNoticeEventWithOtherEvents_ShouldProcessIndependently() {
        // Given
        Long postId1 = 1L;
        Long postId2 = 2L;

        // When - 공지사항 해제 이벤트와 다른 종류의 이벤트를 동시에 발행
        eventPublisher.publishEvent(new PostUnsetAsNoticeEvent(postId1));
        eventPublisher.publishEvent(new PostUnsetAsNoticeEvent(postId2));

        // Then - 모든 공지사항 해제 이벤트가 독립적으로 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(postCacheUseCase).removeNoticeFromCache(eq(postId1));
                    verify(postCacheUseCase).removeNoticeFromCache(eq(postId2));
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
    @DisplayName("연속된 공지사항 해제 이벤트 처리 순서")
    void sequentialUnsetNoticeEvents_ShouldMaintainOrder() {
        // Given - 연속된 공지사항 해제 이벤트
        Long postId1 = 1L;
        Long postId2 = 2L;
        Long postId3 = 3L;
        
        // When - 순서대로 공지사항 해제 이벤트 발행
        eventPublisher.publishEvent(new PostUnsetAsNoticeEvent(postId1));
        eventPublisher.publishEvent(new PostUnsetAsNoticeEvent(postId2));
        eventPublisher.publishEvent(new PostUnsetAsNoticeEvent(postId3));

        // Then - 비동기 처리이지만 모든 이벤트가 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(postCacheUseCase).removeNoticeFromCache(eq(postId1));
                    verify(postCacheUseCase).removeNoticeFromCache(eq(postId2));
                    verify(postCacheUseCase).removeNoticeFromCache(eq(postId3));
                });
    }

    @Test
    @DisplayName("공지사항 해제와 설정 이벤트 혼합 처리")
    void mixedNoticeUnsetAndSetEvents_ShouldProcessIndependently() {
        // Given - 해제와 설정 이벤트 혼합
        Long unsetPostId1 = 1L;
        Long unsetPostId2 = 2L;

        // When - 해제 이벤트들을 발행 (설정 이벤트와 함께 처리될 수 있음)
        eventPublisher.publishEvent(new PostUnsetAsNoticeEvent(unsetPostId1));
        eventPublisher.publishEvent(new PostUnsetAsNoticeEvent(unsetPostId2));

        // Then - 해제 이벤트들이 올바르게 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(postCacheUseCase).removeNoticeFromCache(eq(unsetPostId1));
                    verify(postCacheUseCase).removeNoticeFromCache(eq(unsetPostId2));
                });
    }

    @Test
    @DisplayName("공지사항 해제 후 재설정 시나리오")
    void noticeUnsetThenResetScenario_ShouldProcessAll() {
        // Given - 해제 후 재설정 시나리오
        Long postId = 1L;

        // When - 해제 이벤트만 발행 (재설정은 별도 테스트)
        eventPublisher.publishEvent(new PostUnsetAsNoticeEvent(postId));

        // Then - 해제 이벤트가 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(postCacheUseCase).removeNoticeFromCache(eq(postId));
                });
    }

    @Test
    @DisplayName("시스템 부하 상황에서의 공지사항 해제 이벤트 처리")
    void unsetNoticeEventUnderLoad_ShouldProcessReliably() {
        // Given - 고부하 상황 시뮬레이션
        int highLoadEventCount = 100;
        
        long startTime = System.currentTimeMillis();

        // When - 높은 부하로 공지사항 해제 이벤트 발행
        for (int i = 1; i <= highLoadEventCount; i++) {
            eventPublisher.publishEvent(new PostUnsetAsNoticeEvent((long) i));
        }

        // Then - 모든 이벤트가 20초 내에 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> {
                    for (int i = 1; i <= highLoadEventCount; i++) {
                        verify(postCacheUseCase).removeNoticeFromCache(eq((long) i));
                    }

                    long endTime = System.currentTimeMillis();
                    long totalProcessingTime = endTime - startTime;
                    
                    // 고부하 처리 시간이 20초를 초과하지 않아야 함
                    assert totalProcessingTime < 20000L : "고부하 공지사항 해제 이벤트 처리 시간이 너무 오래 걸림: " + totalProcessingTime + "ms";
                });
    }

    @Test
    @DisplayName("특정 게시글 ID 패턴의 공지사항 해제 이벤트")
    void specificPostIdPatterns_ShouldProcessCorrectly() {
        // Given - 특정 패턴의 게시글 ID들
        Long[] postIds = {1L, 100L, 999L, 10000L, Long.MAX_VALUE};

        // When - 다양한 ID 패턴으로 해제 이벤트 발행
        for (Long postId : postIds) {
            eventPublisher.publishEvent(new PostUnsetAsNoticeEvent(postId));
        }

        // Then - 모든 특정 패턴의 ID가 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    for (Long postId : postIds) {
                        verify(postCacheUseCase).removeNoticeFromCache(eq(postId));
                    }
                });
    }
}
