package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.application.port.out.PostQueryPort;
import jaeik.bimillog.domain.post.application.port.out.RedisPostCommandPort;
import jaeik.bimillog.domain.post.application.service.PostScheduledService;
import jaeik.bimillog.domain.post.entity.PopularPostInfo;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.event.PostFeaturedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>PostScheduledService 테스트</h2>
 * <p>게시글 캐시 동기화 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>스케줄링, 이벤트 처리, 캐시 무효화 등의 복잡한 시나리오를 다양하게 테스트합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostScheduledService 테스트")
@Tag("unit")
class PostScheduledServiceTest {

    @Mock
    private RedisPostCommandPort redisPostCommandPort;

    @Mock
    private PostQueryPort postQueryPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PostScheduledService postScheduledService;

    @Test
    @DisplayName("실시간 인기글 점수 지수감쇠 적용 - applyRealtimeScoreDecay")
    void shouldApplyRealtimeScoreDecay() {
        // Given
        doNothing().when(redisPostCommandPort).applyRealtimePopularScoreDecay();

        // When
        postScheduledService.applyRealtimeScoreDecay();

        // Then
        verify(redisPostCommandPort, times(1)).applyRealtimePopularScoreDecay();
        verifyNoMoreInteractions(redisPostCommandPort);
        verifyNoInteractions(eventPublisher); // 실시간 감쇠는 이벤트 발행 안함
    }

    @Test
    @DisplayName("주간 인기 게시글 업데이트 - 성공 (이벤트 발행 포함)")
    void shouldUpdateWeeklyPopularPosts_WhenPostsExist() {
        // Given
        PopularPostInfo post1 = createPopularPostInfo(1L, "주간인기글1", 1L);
        PopularPostInfo post2 = createPopularPostInfo(2L, "주간인기글2", 2L);
        List<PopularPostInfo> posts = List.of(post1, post2);

        given(postQueryPort.findWeeklyPopularPosts()).willReturn(posts);

        // When
        postScheduledService.updateWeeklyPopularPosts();

        // Then
        verify(redisPostCommandPort).cachePostIds(eq(PostCacheFlag.WEEKLY), eq(List.of(1L, 2L)));

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
        PopularPostInfo anonymousPost = createPopularPostInfo(1L, "익명글", null); // userId가 null
        PopularPostInfo userPost = createPopularPostInfo(2L, "회원글", 2L);
        List<PopularPostInfo> posts = List.of(anonymousPost, userPost);

        given(postQueryPort.findWeeklyPopularPosts()).willReturn(posts);

        // When
        postScheduledService.updateWeeklyPopularPosts();

        // Then
        verify(redisPostCommandPort).cachePostIds(eq(PostCacheFlag.WEEKLY), any());

        // 익명 게시글은 이벤트 발행 안함, 회원 게시글만 이벤트 발행
        ArgumentCaptor<PostFeaturedEvent> eventCaptor = ArgumentCaptor.forClass(PostFeaturedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        PostFeaturedEvent event = eventCaptor.getValue();
        assertThat(event.memberId()).isEqualTo(2L);
        assertThat(event.postId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("전설의 게시글 업데이트 - 성공 (명예의 전당 메시지)")
    void shouldUpdateLegendaryPosts_WhenPostsExist() {
        // Given
        PopularPostInfo legendPost = createPopularPostInfo(1L, "전설의글", 1L);
        List<PopularPostInfo> posts = List.of(legendPost);

        given(postQueryPort.findLegendaryPosts()).willReturn(posts);

        // When
        postScheduledService.updateLegendaryPosts();

        // Then
        verify(redisPostCommandPort).cachePostIds(eq(PostCacheFlag.LEGEND), eq(List.of(1L)));

        // 명예의 전당 이벤트 검증
        ArgumentCaptor<PostFeaturedEvent> eventCaptor = ArgumentCaptor.forClass(PostFeaturedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        PostFeaturedEvent event = eventCaptor.getValue();
        assertThat(event.memberId()).isEqualTo(1L);
        assertThat(event.sseMessage()).isEqualTo("명예의 전당에 등극했어요!");
        assertThat(event.fcmTitle()).isEqualTo("명예의 전당 등극");
        assertThat(event.fcmBody()).contains("명예의 전당에 등극했습니다");
    }

    @Test
    @DisplayName("전설의 게시글 업데이트 - 게시글 목록 비어있는 경우")
    void shouldUpdateLegendaryPosts_WhenPostListIsEmpty() {
        // Given
        given(postQueryPort.findLegendaryPosts()).willReturn(Collections.emptyList());

        // When
        postScheduledService.updateLegendaryPosts();

        // Then
        verify(postQueryPort).findLegendaryPosts();

        // 게시글이 없으면 캐시 및 이벤트 발행 안함
        verify(redisPostCommandPort, never()).cachePostIds(any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }


    @Test
    @DisplayName("스케줄링 메서드들의 트랜잭션 동작 검증")
    void shouldVerifyTransactionalBehavior() {
        // Given
        given(postQueryPort.findWeeklyPopularPosts()).willReturn(Collections.emptyList());
        given(postQueryPort.findLegendaryPosts()).willReturn(Collections.emptyList());

        // When - 모든 스케줄링 메서드 호출
        postScheduledService.applyRealtimeScoreDecay();
        postScheduledService.updateWeeklyPopularPosts();
        postScheduledService.updateLegendaryPosts();

        // Then - @Transactional 동작을 위한 port 호출 검증
        verify(redisPostCommandPort).applyRealtimePopularScoreDecay();
        verify(postQueryPort).findWeeklyPopularPosts();
        verify(postQueryPort).findLegendaryPosts();
    }

    @Test
    @DisplayName("대량 게시글 처리 - 성능 테스트 시나리오")
    void shouldHandleLargeNumberOfPosts_PerformanceScenario() {
        // Given - 대량의 게시글 생성 (100개)
        List<PopularPostInfo> largePosts = createLargePostList(100);

        given(postQueryPort.findWeeklyPopularPosts()).willReturn(largePosts);

        // When
        postScheduledService.updateWeeklyPopularPosts();

        // Then
        verify(redisPostCommandPort).cachePostIds(eq(PostCacheFlag.WEEKLY), any());

        // 100개 게시글 중 userId가 있는 것들만 이벤트 발행 (50개)
        verify(eventPublisher, times(50)).publishEvent(any(PostFeaturedEvent.class));
    }

    // 테스트 유틸리티 메서드들
    private PopularPostInfo createPopularPostInfo(Long postId, String title, Long memberId) {
        return new PopularPostInfo(postId, memberId, title);
    }

    private PostDetail createPostDetail(Long id, String title, String content) {
        return PostDetail.builder()
                .id(id)
                .title(title)
                .content(content)
                .viewCount(0)
                .likeCount(0)
                .createdAt(java.time.Instant.now())
                .memberId(1L)
                .memberName("테스트 사용자")
                .commentCount(0)
                .isLiked(false)
                .build();
    }

    private List<PopularPostInfo> createLargePostList(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> createPopularPostInfo(
                        (long) i,
                        "제목" + i,
                        i % 2 == 0 ? (long) i : null // 짝수만 memberId 설정
                ))
                .toList();
    }
}