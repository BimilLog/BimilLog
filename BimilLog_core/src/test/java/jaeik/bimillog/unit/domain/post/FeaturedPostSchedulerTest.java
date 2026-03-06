package jaeik.bimillog.unit.domain.post;

import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.event.PostEvent.PostFeaturedEvent;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.domain.post.repository.PostQueryType;
import jaeik.bimillog.domain.post.repository.PostRepository;
import jaeik.bimillog.domain.post.scheduler.FeaturedPostScheduler;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostListUpdateAdapter;
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
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>FeaturedPostScheduler 테스트</h2>
 * <p>주간/전설 게시글 플래그 갱신 + 캐시 교체 + 이벤트 발행 흐름을 검증합니다.</p>
 *
 * @author Jaeik
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FeaturedPostScheduler 테스트")
@Tag("unit")
class FeaturedPostSchedulerTest {

    @Mock
    private RedisPostListUpdateAdapter redisPostListUpdateAdapter;

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
                redisPostListUpdateAdapter,
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

        given(postQueryRepository.selectPostSimpleDetails(any(), eq(PageRequest.of(0, PostQueryType.WEEKLY_SCHEDULER.getLimit())), any()))
                .willReturn(new PageImpl<>(posts));

        // When
        featuredPostScheduler.updateWeeklyPopularPosts();

        // Then
        verify(postRepository).clearWeeklyFlag();
        verify(postRepository).setWeeklyFlag(List.of(1L, 2L));
        verify(redisPostListUpdateAdapter).replaceList(eq(RedisKey.POST_WEEKLY_JSON_KEY), anyList(), eq(RedisKey.DEFAULT_CACHE_TTL));

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

        given(postQueryRepository.selectPostSimpleDetails(any(), eq(PageRequest.of(0, PostQueryType.WEEKLY_SCHEDULER.getLimit())), any()))
                .willReturn(new PageImpl<>(posts));

        // When
        featuredPostScheduler.updateWeeklyPopularPosts();

        // Then
        verify(postRepository).clearWeeklyFlag();
        verify(redisPostListUpdateAdapter).replaceList(eq(RedisKey.POST_WEEKLY_JSON_KEY), anyList(), any());

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

        given(postQueryRepository.selectPostSimpleDetails(any(), eq(PageRequest.of(0, PostQueryType.LEGEND_SCHEDULER.getLimit())), any()))
                .willReturn(new PageImpl<>(posts));

        // When
        featuredPostScheduler.updateLegendaryPosts();

        // Then
        verify(postRepository).clearLegendFlag();
        verify(postRepository).setLegendFlag(List.of(1L));
        verify(redisPostListUpdateAdapter).replaceList(eq(RedisKey.POST_LEGEND_JSON_KEY), anyList(), eq(RedisKey.DEFAULT_CACHE_TTL));

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
        given(postQueryRepository.selectPostSimpleDetails(any(), eq(PageRequest.of(0, PostQueryType.LEGEND_SCHEDULER.getLimit())), any()))
                .willReturn(new PageImpl<>(Collections.emptyList()));

        // When
        featuredPostScheduler.updateLegendaryPosts();

        // Then
        verify(postQueryRepository).selectPostSimpleDetails(any(), eq(PageRequest.of(0, PostQueryType.LEGEND_SCHEDULER.getLimit())), any());
        verify(postRepository, never()).clearLegendFlag();
        verify(redisPostListUpdateAdapter, never()).replaceList(any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("대량 게시글 처리 - 성능 테스트 시나리오")
    void shouldHandleLargeNumberOfPosts_PerformanceScenario() {
        // Given
        List<PostSimpleDetail> largePosts = java.util.stream.IntStream.range(0, 100)
                .mapToObj(i -> createPostSimpleDetail((long) i, "제목" + i, i % 2 == 0 ? (long) i : null))
                .toList();

        given(postQueryRepository.selectPostSimpleDetails(any(), eq(PageRequest.of(0, PostQueryType.WEEKLY_SCHEDULER.getLimit())), any()))
                .willReturn(new PageImpl<>(largePosts));

        // When
        featuredPostScheduler.updateWeeklyPopularPosts();

        // Then
        verify(redisPostListUpdateAdapter).replaceList(eq(RedisKey.POST_WEEKLY_JSON_KEY), anyList(), any());
        verify(eventPublisher, times(50)).publishEvent(any(PostFeaturedEvent.class));
    }

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
}
