package jaeik.bimillog.domain.post.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.post.entity.jpa.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.event.PostFeaturedEvent;
import jaeik.bimillog.domain.post.scheduler.FeaturedPostScheduleExecutor;
import jaeik.bimillog.domain.post.scheduler.PostScheduledService;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisSimplePostAdapter;
import jaeik.bimillog.infrastructure.resilience.RealtimeScoreFallbackStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static jaeik.bimillog.domain.post.scheduler.PostScheduledService.POST_CACHE_TTL_WEEKLY_LEGEND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>PostScheduledService 테스트</h2>
 * <p>게시글 캐시 동기화 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>주간/레전드는 Hash 캐시에 TTL 1일로 직접 저장합니다.</p>
 *
 * @author Jaeik
 * @version 2.6.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostScheduledService 테스트")
@Tag("unit")
class PostScheduledServiceTest {

    @Mock
    private RedisSimplePostAdapter redisSimplePostAdapter;

    @Mock
    private RedisRealTimePostAdapter redisRealTimePostAdapter;

    @Mock
    private RealtimeScoreFallbackStore realtimeScoreFallbackStore;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private CircuitBreaker realtimeRedisCircuitBreaker;

    @Mock
    private FeaturedPostScheduleExecutor featuredPostScheduleExecutor;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PostScheduledService postScheduledService;

    @BeforeEach
    void setUp() {
        given(circuitBreakerRegistry.circuitBreaker("realtimeRedis")).willReturn(realtimeRedisCircuitBreaker);
        given(realtimeRedisCircuitBreaker.getState()).willReturn(CircuitBreaker.State.CLOSED);

        postScheduledService = new PostScheduledService(
                redisSimplePostAdapter,
                redisRealTimePostAdapter,
                realtimeScoreFallbackStore,
                circuitBreakerRegistry,
                eventPublisher,
                featuredPostScheduleExecutor
        );
    }

    @Test
    @DisplayName("실시간 인기글 점수 지수감쇠 - 서킷 닫힘 → Redis에 감쇠 적용")
    void shouldApplyDecayToRedis_WhenCircuitClosed() {
        // Given: 기본 setUp에서 서킷 CLOSED

        // When
        postScheduledService.applyRealtimeScoreDecay();

        // Then: Redis에만 감쇠 적용
        verify(redisRealTimePostAdapter).applyRealtimePopularScoreDecay();
        verify(realtimeScoreFallbackStore, never()).applyDecay();
    }

    @Test
    @DisplayName("실시간 인기글 점수 지수감쇠 - 서킷 열림 → Caffeine에 감쇠 적용")
    void shouldApplyDecayToCaffeine_WhenCircuitOpen() {
        // Given: 서킷 OPEN
        given(realtimeRedisCircuitBreaker.getState()).willReturn(CircuitBreaker.State.OPEN);

        // When
        postScheduledService.applyRealtimeScoreDecay();

        // Then: Caffeine에만 감쇠 적용
        verify(realtimeScoreFallbackStore).applyDecay();
        verify(redisRealTimePostAdapter, never()).applyRealtimePopularScoreDecay();
    }

    @Test
    @DisplayName("주간 인기 게시글 업데이트 - 성공 (TTL 1일로 Hash 캐시에 직접 저장)")
    void shouldUpdateWeeklyPopularPosts_WhenPostsExist() {
        // Given
        PostSimpleDetail post1 = createPostSimpleDetail(1L, "주간인기글1", 1L);
        PostSimpleDetail post2 = createPostSimpleDetail(2L, "주간인기글2", 2L);
        List<PostSimpleDetail> posts = List.of(post1, post2);

        given(featuredPostScheduleExecutor.fetchWeeklyPosts()).willReturn(posts);

        // When
        postScheduledService.updateWeeklyPopularPosts();

        // Then
        // DB에 특집 게시글 저장
        verify(featuredPostScheduleExecutor).saveFeaturedPosts(any(), eq(PostCacheFlag.WEEKLY));
        // Hash 캐시에 TTL 1일로 직접 저장
        verify(redisSimplePostAdapter).cachePostsWithTtl(eq(PostCacheFlag.WEEKLY), any(), eq(POST_CACHE_TTL_WEEKLY_LEGEND));

        // 이벤트 발행 검증
        ArgumentCaptor<PostFeaturedEvent> eventCaptor = ArgumentCaptor.forClass(PostFeaturedEvent.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());

        List<PostFeaturedEvent> events = eventCaptor.getAllValues();
        assertThat(events).hasSize(2);
        assertThat(events.getFirst().memberId()).isEqualTo(1L);
        assertThat(events.get(0).sseMessage()).isEqualTo("주간 인기 게시글로 선정되었어요!");
        assertThat(events.get(0).postId()).isEqualTo(1L);
        assertThat(events.get(1).memberId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("주간 인기 게시글 업데이트 - 익명 게시글 포함 (이벤트 발행 안함)")
    void shouldUpdateWeeklyPopularPosts_WhenAnonymousPostsIncluded() {
        // Given
        PostSimpleDetail anonymousPost = createPostSimpleDetail(1L, "익명글", null); // userId가 null
        PostSimpleDetail userPost = createPostSimpleDetail(2L, "회원글", 2L);
        List<PostSimpleDetail> posts = List.of(anonymousPost, userPost);

        given(featuredPostScheduleExecutor.fetchWeeklyPosts()).willReturn(posts);

        // When
        postScheduledService.updateWeeklyPopularPosts();

        // Then
        verify(featuredPostScheduleExecutor).saveFeaturedPosts(any(), eq(PostCacheFlag.WEEKLY));
        verify(redisSimplePostAdapter).cachePostsWithTtl(eq(PostCacheFlag.WEEKLY), any(), any(Duration.class));

        // 익명 게시글은 이벤트 발행 안함, 회원 게시글만 이벤트 발행
        ArgumentCaptor<PostFeaturedEvent> eventCaptor = ArgumentCaptor.forClass(PostFeaturedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        PostFeaturedEvent event = eventCaptor.getValue();
        assertThat(event.memberId()).isEqualTo(2L);
        assertThat(event.postId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("전설의 게시글 업데이트 - 성공 (TTL 1일로 Hash 캐시에 직접 저장)")
    void shouldUpdateLegendaryPosts_WhenPostsExist() {
        // Given
        PostSimpleDetail legendPost = createPostSimpleDetail(1L, "전설의글", 1L);
        List<PostSimpleDetail> posts = List.of(legendPost);

        given(featuredPostScheduleExecutor.fetchLegendaryPosts()).willReturn(posts);

        // When
        postScheduledService.updateLegendaryPosts();

        // Then
        // DB에 특집 게시글 저장
        verify(featuredPostScheduleExecutor).saveFeaturedPosts(any(), eq(PostCacheFlag.LEGEND));
        // Hash 캐시에 TTL 1일로 직접 저장
        verify(redisSimplePostAdapter).cachePostsWithTtl(eq(PostCacheFlag.LEGEND), any(), eq(POST_CACHE_TTL_WEEKLY_LEGEND));

        // 명예의 전당 이벤트 검증
        ArgumentCaptor<PostFeaturedEvent> eventCaptor = ArgumentCaptor.forClass(PostFeaturedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        PostFeaturedEvent event = eventCaptor.getValue();
        assertThat(event.memberId()).isEqualTo(1L);
        assertThat(event.sseMessage()).isEqualTo("명예의 전당에 등극했어요!");
        assertThat(event.notificationType()).isEqualTo(NotificationType.POST_FEATURED_LEGEND);
        assertThat(event.postTitle()).isNotNull();
    }

    @Test
    @DisplayName("전설의 게시글 업데이트 - 게시글 목록 비어있는 경우")
    void shouldUpdateLegendaryPosts_WhenPostListIsEmpty() {
        // Given
        given(featuredPostScheduleExecutor.fetchLegendaryPosts()).willReturn(Collections.emptyList());

        // When
        postScheduledService.updateLegendaryPosts();

        // Then
        verify(featuredPostScheduleExecutor).fetchLegendaryPosts();

        // 게시글이 없으면 저장, 캐시, 이벤트 발행 안함
        verify(featuredPostScheduleExecutor, never()).saveFeaturedPosts(any(), any());
        verify(redisSimplePostAdapter, never()).cachePostsWithTtl(any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }


    @Test
    @DisplayName("스케줄링 메서드들의 트랜잭션 동작 검증")
    void shouldVerifyTransactionalBehavior() {
        // Given: 기본 setUp에서 서킷 CLOSED
        given(featuredPostScheduleExecutor.fetchWeeklyPosts()).willReturn(Collections.emptyList());
        given(featuredPostScheduleExecutor.fetchLegendaryPosts()).willReturn(Collections.emptyList());

        // When - 모든 스케줄링 메서드 호출
        postScheduledService.applyRealtimeScoreDecay();
        postScheduledService.updateWeeklyPopularPosts();
        postScheduledService.updateLegendaryPosts();

        // Then - 서킷 CLOSED이므로 Redis에만 감쇠 적용
        verify(redisRealTimePostAdapter).applyRealtimePopularScoreDecay();
        verify(realtimeScoreFallbackStore, never()).applyDecay();
        verify(featuredPostScheduleExecutor).fetchWeeklyPosts();
        verify(featuredPostScheduleExecutor).fetchLegendaryPosts();
    }

    @Test
    @DisplayName("대량 게시글 처리 - 성능 테스트 시나리오")
    void shouldHandleLargeNumberOfPosts_PerformanceScenario() {
        // Given - 대량의 게시글 생성 (100개)
        List<PostSimpleDetail> largePosts = createLargePostList(100);

        given(featuredPostScheduleExecutor.fetchWeeklyPosts()).willReturn(largePosts);

        // When
        postScheduledService.updateWeeklyPopularPosts();

        // Then
        verify(redisSimplePostAdapter).cachePostsWithTtl(eq(PostCacheFlag.WEEKLY), any(), any(Duration.class));

        // 100개 게시글 중 userId가 있는 것들만 이벤트 발행 (50개)
        verify(eventPublisher, times(50)).publishEvent(any(PostFeaturedEvent.class));
    }

    // 테스트 유틸리티 메서드들
    private PostSimpleDetail createPostSimpleDetail(Long postId, String title, Long memberId) {
        return PostSimpleDetail.builder()
                .id(postId)
                .title(title)
                .viewCount(0)
                .likeCount(0)
                .createdAt(Instant.now())
                .memberId(memberId)
                .memberName(memberId != null ? "회원" + memberId : "비회원")
                .commentCount(0)
                .build();
    }

    private List<PostSimpleDetail> createLargePostList(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> createPostSimpleDetail(
                        (long) i,
                        "제목" + i,
                        i % 2 == 0 ? (long) i : null // 짝수만 memberId 설정
                ))
                .toList();
    }
}
