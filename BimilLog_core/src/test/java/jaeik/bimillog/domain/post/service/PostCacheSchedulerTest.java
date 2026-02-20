package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.event.PostFeaturedEvent;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.domain.post.repository.PostRepository;
import jaeik.bimillog.domain.post.scheduler.PostCacheScheduler;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostJsonListAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>PostCacheScheduler 테스트</h2>
 * <p>게시글 캐시 동기화 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>DB 조회 → 플래그 업데이트 → JSON LIST 전체 교체 → 이벤트 발행 흐름을 검증합니다.</p>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostCacheScheduler 테스트")
@Tag("unit")
class PostCacheSchedulerTest {

    @Mock
    private RedisPostJsonListAdapter redisPostJsonListAdapter;

    @Mock
    private RedisRealTimePostAdapter redisRealTimePostAdapter;

    @Mock
    private PostQueryRepository postQueryRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PostCacheScheduler postCacheScheduler;

    @BeforeEach
    void setUp() {
        postCacheScheduler = new PostCacheScheduler(
                redisPostJsonListAdapter,
                redisRealTimePostAdapter,
                postQueryRepository,
                postRepository,
                eventPublisher
        );
    }

    @Test
    @DisplayName("주간 인기 게시글 업데이트 - 성공 (플래그 업데이트 + JSON LIST 교체 + 이벤트 발행)")
    void shouldUpdateWeeklyPopularPosts_WhenPostsExist() {
        // Given
        PostSimpleDetail post1 = createPostSimpleDetail(1L, "주간인기글1", 1L);
        PostSimpleDetail post2 = createPostSimpleDetail(2L, "주간인기글2", 2L);
        List<PostSimpleDetail> posts = List.of(post1, post2);

        given(postQueryRepository.findWeeklyPopularPosts()).willReturn(posts);

        // When
        postCacheScheduler.updateWeeklyPopularPosts();

        // Then
        // 플래그 업데이트
        verify(postRepository).clearWeeklyFlag();
        verify(postRepository).setWeeklyFlag(List.of(1L, 2L));
        // JSON LIST 전체 교체
        verify(redisPostJsonListAdapter).replaceAll(eq(RedisKey.POST_WEEKLY_JSON_KEY), anyList(), eq(RedisKey.DEFAULT_CACHE_TTL));

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
        PostSimpleDetail anonymousPost = createPostSimpleDetail(1L, "익명글", null);
        PostSimpleDetail userPost = createPostSimpleDetail(2L, "회원글", 2L);
        List<PostSimpleDetail> posts = List.of(anonymousPost, userPost);

        given(postQueryRepository.findWeeklyPopularPosts()).willReturn(posts);

        // When
        postCacheScheduler.updateWeeklyPopularPosts();

        // Then
        verify(postRepository).clearWeeklyFlag();
        verify(redisPostJsonListAdapter).replaceAll(eq(RedisKey.POST_WEEKLY_JSON_KEY), anyList(), any());

        // 익명 게시글은 이벤트 발행 안함, 회원 게시글만 이벤트 발행
        ArgumentCaptor<PostFeaturedEvent> eventCaptor = ArgumentCaptor.forClass(PostFeaturedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        PostFeaturedEvent event = eventCaptor.getValue();
        assertThat(event.memberId()).isEqualTo(2L);
        assertThat(event.postId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("전설의 게시글 업데이트 - 성공 (플래그 업데이트 + JSON LIST 교체)")
    void shouldUpdateLegendaryPosts_WhenPostsExist() {
        // Given
        PostSimpleDetail legendPost = createPostSimpleDetail(1L, "전설의글", 1L);
        List<PostSimpleDetail> posts = List.of(legendPost);

        given(postQueryRepository.findLegendaryPosts()).willReturn(posts);

        // When
        postCacheScheduler.updateLegendaryPosts();

        // Then
        verify(postRepository).clearLegendFlag();
        verify(postRepository).setLegendFlag(List.of(1L));
        verify(redisPostJsonListAdapter).replaceAll(eq(RedisKey.POST_LEGEND_JSON_KEY), anyList(), eq(RedisKey.DEFAULT_CACHE_TTL));

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
        given(postQueryRepository.findLegendaryPosts()).willReturn(Collections.emptyList());

        // When
        postCacheScheduler.updateLegendaryPosts();

        // Then
        verify(postQueryRepository).findLegendaryPosts();

        // 게시글이 없으면 플래그 업데이트, 캐시, 이벤트 발행 안함
        verify(postRepository, never()).clearLegendFlag();
        verify(redisPostJsonListAdapter, never()).replaceAll(any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("대량 게시글 처리 - 성능 테스트 시나리오")
    void shouldHandleLargeNumberOfPosts_PerformanceScenario() {
        // Given - 대량의 게시글 생성 (100개)
        List<PostSimpleDetail> largePosts = createLargePostList(100);

        given(postQueryRepository.findWeeklyPopularPosts()).willReturn(largePosts);

        // When
        postCacheScheduler.updateWeeklyPopularPosts();

        // Then
        verify(redisPostJsonListAdapter).replaceAll(eq(RedisKey.POST_WEEKLY_JSON_KEY), anyList(), any());

        // 100개 게시글 중 memberId가 있는 것들만 이벤트 발행 (50개)
        verify(eventPublisher, times(50)).publishEvent(any(PostFeaturedEvent.class));
    }

    // ==================== 공지사항 ====================

    @Test
    @DisplayName("공지사항 캐시 갱신 - 성공 (DB 조회 → JSON LIST 전체 교체 with TTL)")
    void shouldRefreshNoticePosts_WhenPostsExist() {
        // Given
        PostSimpleDetail notice1 = createPostSimpleDetail(1L, "공지1", 1L);
        PostSimpleDetail notice2 = createPostSimpleDetail(2L, "공지2", 2L);
        List<PostSimpleDetail> posts = List.of(notice1, notice2);

        given(postQueryRepository.findNoticePostsForScheduler()).willReturn(posts);

        // When
        postCacheScheduler.refreshNoticePosts();

        // Then
        verify(redisPostJsonListAdapter).replaceAll(eq(RedisKey.POST_NOTICE_JSON_KEY), any(), eq(RedisKey.DEFAULT_CACHE_TTL));

        // 공지사항은 플래그 업데이트나 이벤트 발행 없음
        verify(postRepository, never()).clearWeeklyFlag();
        verify(postRepository, never()).clearLegendFlag();
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("공지사항 캐시 갱신 - 공지 없으면 스킵")
    void shouldSkipNoticeRefresh_WhenNoNoticePosts() {
        // Given
        given(postQueryRepository.findNoticePostsForScheduler()).willReturn(Collections.emptyList());

        // When
        postCacheScheduler.refreshNoticePosts();

        // Then
        verify(redisPostJsonListAdapter, never()).replaceAll(any(), any(), any());
    }

    // ==================== 첫 페이지 ====================

    @Test
    @DisplayName("첫 페이지 캐시 갱신 - 성공 (DB 조회 → JSON LIST 전체 교체)")
    void shouldRefreshFirstPageCache_WhenPostsExist() {
        // Given
        PostSimpleDetail post1 = createPostSimpleDetail(1L, "첫페이지글1", 1L);
        PostSimpleDetail post2 = createPostSimpleDetail(2L, "첫페이지글2", 2L);
        List<PostSimpleDetail> posts = List.of(post1, post2);

        given(postQueryRepository.findBoardPostsByCursor(null, RedisKey.FIRST_PAGE_SIZE)).willReturn(posts);

        // When
        postCacheScheduler.refreshFirstPageCache();

        // Then
        verify(redisPostJsonListAdapter).replaceAll(eq(RedisKey.FIRST_PAGE_JSON_KEY), anyList(), eq(RedisKey.DEFAULT_CACHE_TTL));

        // 첫 페이지는 플래그 업데이트나 이벤트 발행 없음
        verify(postRepository, never()).clearWeeklyFlag();
        verify(postRepository, never()).clearLegendFlag();
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("첫 페이지 캐시 갱신 - 게시글 없으면 스킵")
    void shouldSkipFirstPageRefresh_WhenNoPostsExist() {
        // Given
        given(postQueryRepository.findBoardPostsByCursor(null, RedisKey.FIRST_PAGE_SIZE))
                .willReturn(Collections.emptyList());

        // When
        postCacheScheduler.refreshFirstPageCache();

        // Then
        verify(redisPostJsonListAdapter, never()).replaceAll(any(), any(), any());
    }

    // ==================== 실시간 인기글 ====================

    @Test
    @DisplayName("실시간 인기글 캐시 갱신 - 성공 (ZSet top ID → DB → JSON LIST 교체)")
    void shouldRefreshRealtimePopularPosts_WhenZSetHasData() {
        // Given
        List<Long> topIds = List.of(3L, 1L, 5L);
        PostSimpleDetail post1 = createPostSimpleDetail(3L, "실시간인기글1", 1L);
        PostSimpleDetail post2 = createPostSimpleDetail(1L, "실시간인기글2", 2L);
        PostSimpleDetail post3 = createPostSimpleDetail(5L, "실시간인기글3", 3L);
        List<PostSimpleDetail> posts = List.of(post1, post2, post3);

        given(redisRealTimePostAdapter.getRangePostId()).willReturn(topIds);
        given(postQueryRepository.findPostSimpleDetailsByIds(eq(topIds), any(Pageable.class)))
                .willReturn(new PageImpl<>(posts));

        // When
        postCacheScheduler.refreshRealtimePopularPosts();

        // Then
        verify(redisPostJsonListAdapter).replaceAll(eq(RedisKey.POST_REALTIME_JSON_KEY), anyList(), eq(RedisKey.DEFAULT_CACHE_TTL));
    }

    @Test
    @DisplayName("실시간 인기글 캐시 갱신 - ZSet 비어있으면 스킵")
    void shouldSkipRealtimeRefresh_WhenZSetIsEmpty() {
        // Given
        given(redisRealTimePostAdapter.getRangePostId()).willReturn(List.of());

        // When
        postCacheScheduler.refreshRealtimePopularPosts();

        // Then
        verify(redisPostJsonListAdapter, never()).replaceAll(
                eq(RedisKey.POST_REALTIME_JSON_KEY), any(), any());
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
                        i % 2 == 0 ? (long) i : null
                ))
                .toList();
    }
}
