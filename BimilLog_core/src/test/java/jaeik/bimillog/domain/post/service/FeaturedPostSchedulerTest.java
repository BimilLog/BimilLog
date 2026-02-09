package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.entity.jpa.PostCacheFlag;
import jaeik.bimillog.domain.post.event.PostFeaturedEvent;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.domain.post.repository.PostRepository;
import jaeik.bimillog.domain.post.scheduler.FeaturedPostScheduler;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostHashAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostIndexAdapter;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>FeaturedPostScheduler 테스트</h2>
 * <p>게시글 캐시 동기화 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>DB 조회 → featuredType 업데이트 → 글 단위 Hash 생성 → SET 인덱스 교체 → 이벤트 발행 흐름을 검증합니다.</p>
 *
 * @author Jaeik
 * @version 3.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FeaturedPostScheduler 테스트")
@Tag("unit")
class FeaturedPostSchedulerTest {

    @Mock
    private RedisPostHashAdapter redisPostHashAdapter;

    @Mock
    private RedisPostIndexAdapter redisPostIndexAdapter;

    @Mock
    private PostQueryRepository postQueryRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private FeaturedPostScheduler featuredPostScheduler;

    @BeforeEach
    void setUp() {
        featuredPostScheduler = new FeaturedPostScheduler(
                redisPostHashAdapter,
                redisPostIndexAdapter,
                postQueryRepository,
                postRepository,
                eventPublisher
        );
    }

    @Test
    @DisplayName("주간 인기 게시글 업데이트 - 성공 (featuredType 업데이트 + 글 단위 Hash 생성 + SET 인덱스 교체 + 이벤트 발행)")
    void shouldUpdateWeeklyPopularPosts_WhenPostsExist() {
        // Given
        PostSimpleDetail post1 = createPostSimpleDetail(1L, "주간인기글1", 1L);
        PostSimpleDetail post2 = createPostSimpleDetail(2L, "주간인기글2", 2L);
        List<PostSimpleDetail> posts = List.of(post1, post2);

        given(postQueryRepository.findWeeklyPopularPosts()).willReturn(posts);

        // When
        featuredPostScheduler.updateWeeklyPopularPosts();

        // Then
        // featuredType 업데이트
        verify(postRepository).clearFeaturedType(PostCacheFlag.WEEKLY);
        verify(postRepository).setFeaturedType(List.of(1L, 2L), PostCacheFlag.WEEKLY);
        // 글 단위 Hash 생성
        verify(redisPostHashAdapter).createPostHash(post1);
        verify(redisPostHashAdapter).createPostHash(post2);
        // SET 인덱스 교체
        verify(redisPostIndexAdapter).replaceIndex(eq(RedisKey.POST_WEEKLY_IDS_KEY), any(Set.class), eq(RedisKey.POST_CACHE_TTL_WEEKLY_LEGEND));

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
        featuredPostScheduler.updateWeeklyPopularPosts();

        // Then
        verify(postRepository).clearFeaturedType(PostCacheFlag.WEEKLY);
        verify(redisPostHashAdapter, times(2)).createPostHash(any(PostSimpleDetail.class));
        verify(redisPostIndexAdapter).replaceIndex(eq(RedisKey.POST_WEEKLY_IDS_KEY), any(Set.class), any(Duration.class));

        // 익명 게시글은 이벤트 발행 안함, 회원 게시글만 이벤트 발행
        ArgumentCaptor<PostFeaturedEvent> eventCaptor = ArgumentCaptor.forClass(PostFeaturedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        PostFeaturedEvent event = eventCaptor.getValue();
        assertThat(event.memberId()).isEqualTo(2L);
        assertThat(event.postId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("전설의 게시글 업데이트 - 성공 (featuredType 업데이트 + 글 단위 Hash 생성 + SET 인덱스 교체)")
    void shouldUpdateLegendaryPosts_WhenPostsExist() {
        // Given
        PostSimpleDetail legendPost = createPostSimpleDetail(1L, "전설의글", 1L);
        List<PostSimpleDetail> posts = List.of(legendPost);

        given(postQueryRepository.findLegendaryPosts()).willReturn(posts);

        // When
        featuredPostScheduler.updateLegendaryPosts();

        // Then
        verify(postRepository).clearFeaturedType(PostCacheFlag.LEGEND);
        verify(postRepository).setFeaturedTypeOverriding(List.of(1L), PostCacheFlag.LEGEND, PostCacheFlag.WEEKLY);
        verify(redisPostHashAdapter).createPostHash(legendPost);
        verify(redisPostIndexAdapter).replaceIndex(eq(RedisKey.POST_LEGEND_IDS_KEY), any(Set.class), eq(RedisKey.POST_CACHE_TTL_WEEKLY_LEGEND));

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
        featuredPostScheduler.updateLegendaryPosts();

        // Then
        verify(postQueryRepository).findLegendaryPosts();

        // 게시글이 없으면 featuredType 업데이트, 캐시, 이벤트 발행 안함
        verify(postRepository, never()).clearFeaturedType(any());
        verify(redisPostHashAdapter, never()).createPostHash(any());
        verify(redisPostIndexAdapter, never()).replaceIndex(any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("대량 게시글 처리 - 성능 테스트 시나리오")
    void shouldHandleLargeNumberOfPosts_PerformanceScenario() {
        // Given - 대량의 게시글 생성 (100개)
        List<PostSimpleDetail> largePosts = createLargePostList(100);

        given(postQueryRepository.findWeeklyPopularPosts()).willReturn(largePosts);

        // When
        featuredPostScheduler.updateWeeklyPopularPosts();

        // Then
        verify(redisPostHashAdapter, times(100)).createPostHash(any(PostSimpleDetail.class));
        verify(redisPostIndexAdapter).replaceIndex(eq(RedisKey.POST_WEEKLY_IDS_KEY), any(Set.class), any(Duration.class));

        // 100개 게시글 중 memberId가 있는 것들만 이벤트 발행 (50개)
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
                        i % 2 == 0 ? (long) i : null
                ))
                .toList();
    }
}
