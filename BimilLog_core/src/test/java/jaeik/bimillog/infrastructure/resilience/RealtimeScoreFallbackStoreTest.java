package jaeik.bimillog.infrastructure.resilience;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>RealtimeScoreFallbackStore 단위 테스트</h2>
 * <p>Redis 폴백 저장소의 동시성, 정렬, 기본 동작을 검증합니다.</p>
 */
@Tag("unit")
@DisplayName("RealtimeScoreFallbackStore 단위 테스트")
class RealtimeScoreFallbackStoreTest {

    private RealtimeScoreFallbackStore fallbackStore;

    @BeforeEach
    void setUp() {
        fallbackStore = new RealtimeScoreFallbackStore();
    }

    @Test
    @DisplayName("점수 저장 - 단일 게시글 점수 증가")
    void shouldIncrementScore_WhenPostIdAndScoreProvided() {
        // Given
        Long postId = 1L;
        double score = 4.0;

        // When
        fallbackStore.incrementScore(postId, score);

        // Then
        assertThat(fallbackStore.size()).isEqualTo(1);
        assertThat(fallbackStore.hasData()).isTrue();
    }

    @Test
    @DisplayName("점수 누적 - 동일 게시글에 여러 번 점수 증가")
    void shouldAccumulateScore_WhenMultipleIncrementsOccur() {
        // Given
        Long postId = 1L;

        // When: 조회 2점 + 댓글 3점 + 추천 4점
        fallbackStore.incrementScore(postId, 2.0);
        fallbackStore.incrementScore(postId, 3.0);
        fallbackStore.incrementScore(postId, 4.0);

        // Then: 상위 1개 조회하면 해당 postId가 있어야 함
        List<Long> topIds = fallbackStore.getTopPostIds(0, 1);
        assertThat(topIds).containsExactly(postId);
    }

    @Test
    @DisplayName("상위 N개 조회 - 점수 내림차순 정렬")
    void shouldReturnTopPostIds_InDescendingOrder() {
        // Given: 여러 게시글에 다른 점수 설정
        fallbackStore.incrementScore(1L, 10.0);
        fallbackStore.incrementScore(2L, 30.0);
        fallbackStore.incrementScore(3L, 20.0);
        fallbackStore.incrementScore(4L, 5.0);
        fallbackStore.incrementScore(5L, 25.0);

        // When
        List<Long> topIds = fallbackStore.getTopPostIds(0, 5);

        // Then: 점수 내림차순으로 정렬 (30, 25, 20, 10, 5)
        assertThat(topIds).containsExactly(2L, 5L, 3L, 1L, 4L);
    }

    @Test
    @DisplayName("상위 N개 조회 - limit보다 적은 데이터")
    void shouldReturnAllPostIds_WhenLessThanLimit() {
        // Given: 3개 게시글
        fallbackStore.incrementScore(1L, 10.0);
        fallbackStore.incrementScore(2L, 20.0);
        fallbackStore.incrementScore(3L, 15.0);

        // When: 5개 요청
        List<Long> topIds = fallbackStore.getTopPostIds(0, 5);

        // Then: 3개만 반환
        assertThat(topIds).hasSize(3);
        assertThat(topIds).containsExactly(2L, 3L, 1L);
    }

    @Test
    @DisplayName("빈 저장소 조회 - 빈 리스트 반환")
    void shouldReturnEmptyList_WhenStoreIsEmpty() {
        // When
        List<Long> topIds = fallbackStore.getTopPostIds(0, 10);

        // Then
        assertThat(topIds).isEmpty();
        assertThat(fallbackStore.hasData()).isFalse();
        assertThat(fallbackStore.size()).isZero();
    }

    @Test
    @DisplayName("0 이하 점수 필터링 - 양수 점수만 반환")
    void shouldFilterOutNonPositiveScores() {
        // Given
        fallbackStore.incrementScore(1L, 10.0);
        fallbackStore.incrementScore(2L, -5.0);  // 음수
        fallbackStore.incrementScore(3L, 5.0);
        fallbackStore.incrementScore(3L, -5.0);  // 3L의 합계는 0

        // When
        List<Long> topIds = fallbackStore.getTopPostIds(0, 10);

        // Then: 양수 점수만 반환
        assertThat(topIds).containsExactly(1L);
    }

    @Test
    @DisplayName("저장소 초기화 - clear 호출 후 비어있음")
    void shouldClearAllData_WhenClearInvoked() {
        // Given
        fallbackStore.incrementScore(1L, 10.0);
        fallbackStore.incrementScore(2L, 20.0);
        assertThat(fallbackStore.size()).isEqualTo(2);

        // When
        fallbackStore.clear();

        // Then
        assertThat(fallbackStore.size()).isZero();
        assertThat(fallbackStore.hasData()).isFalse();
        assertThat(fallbackStore.getTopPostIds(0, 10)).isEmpty();
    }

    @Test
    @DisplayName("동시성 테스트 - 다중 스레드에서 점수 증가")
    void shouldHandleConcurrentIncrements() throws InterruptedException {
        // Given
        int threadCount = 100;
        Long postId = 1L;
        double scorePerThread = 1.0;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // When: 100개 스레드에서 동시에 점수 증가
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    fallbackStore.incrementScore(postId, scorePerThread);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Then: 모든 점수가 누적되어야 함
        List<Long> topIds = fallbackStore.getTopPostIds(0, 1);
        assertThat(topIds).containsExactly(postId);
        assertThat(fallbackStore.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("동시성 테스트 - 다중 스레드에서 여러 게시글 점수 증가")
    void shouldHandleConcurrentIncrementsForMultiplePosts() throws InterruptedException {
        // Given
        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // When: 100개 스레드에서 10개 게시글에 분산하여 점수 증가
        for (int i = 0; i < threadCount; i++) {
            final long postId = i % 10 + 1; // 1-10
            executor.submit(() -> {
                try {
                    fallbackStore.incrementScore(postId, 1.0);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Then: 10개 게시글이 저장되어야 함
        assertThat(fallbackStore.size()).isEqualTo(10);

        // 각 게시글당 10회씩 증가 (100 / 10 = 10)
        List<Long> topIds = fallbackStore.getTopPostIds(0, 10);
        assertThat(topIds).hasSize(10);
    }
}
