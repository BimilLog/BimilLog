package jaeik.bimillog.domain.post.application.service;

import jaeik.bimillog.domain.post.application.port.out.PostCacheCommandPort;
import jaeik.bimillog.domain.post.application.port.out.PostCacheSyncPort;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSearchResult;
import jaeik.bimillog.domain.post.event.PostFeaturedEvent;
import org.junit.jupiter.api.DisplayName;
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
 * <h2>PostCacheSyncService 테스트</h2>
 * <p>게시글 캐시 동기화 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>스케줄링, 이벤트 처리, 캐시 무효화 등의 복잡한 시나리오를 다양하게 테스트합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostCacheSyncService 테스트")
class PostCacheSyncServiceTest {

    @Mock
    private PostCacheCommandPort postCacheCommandPort;

    @Mock
    private PostCacheSyncPort postCacheSyncPort;



    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PostCacheSyncService postCacheSyncService;

    @Test
    @DisplayName("실시간 인기 게시글 업데이트 - 성공 (게시글 있음)")
    void shouldUpdateRealtimePopularPosts_WhenPostsExist() {
        // Given
        PostSearchResult post1 = createPostSearchResult(1L, "제목1", 1L);
        PostSearchResult post2 = createPostSearchResult(2L, "제목2", 2L);
        List<PostSearchResult> posts = List.of(post1, post2);
        
        PostDetail fullPost1 = createPostDetail(1L, "제목1", "내용1");
        PostDetail fullPost2 = createPostDetail(2L, "제목2", "내용2");

        given(postCacheSyncPort.findRealtimePopularPosts()).willReturn(posts);
        given(postCacheSyncPort.findPostDetail(1L)).willReturn(fullPost1);
        given(postCacheSyncPort.findPostDetail(2L)).willReturn(fullPost2);

        // When
        postCacheSyncService.updateRealtimePopularPosts();

        // Then
        verify(postCacheCommandPort).resetPopularFlag(PostCacheFlag.REALTIME);
        verify(postCacheCommandPort).applyPopularFlag(List.of(1L, 2L), PostCacheFlag.REALTIME);
        verify(postCacheCommandPort).cachePostsWithDetails(PostCacheFlag.REALTIME, List.of(fullPost1, fullPost2));
        
        verifyNoInteractions(eventPublisher); // 실시간은 이벤트 발행 안함
    }

    @Test
    @DisplayName("실시간 인기 게시글 업데이트 - 게시글 없음")
    void shouldUpdateRealtimePopularPosts_WhenNoPostsExist() {
        // Given
        given(postCacheSyncPort.findRealtimePopularPosts()).willReturn(Collections.emptyList());

        // When
        postCacheSyncService.updateRealtimePopularPosts();

        // Then
        verify(postCacheCommandPort).resetPopularFlag(PostCacheFlag.REALTIME);
        verify(postCacheSyncPort).findRealtimePopularPosts();
        
        // 게시글이 없으면 캐시 관련 작업 수행 안함
        verify(postCacheCommandPort, never()).applyPopularFlag(any(), any());
        verify(postCacheCommandPort, never()).cachePostsWithDetails(any(), any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("주간 인기 게시글 업데이트 - 성공 (이벤트 발행 포함)")
    void shouldUpdateWeeklyPopularPosts_WhenPostsExist() {
        // Given
        PostSearchResult post1 = createPostSearchResult(1L, "주간인기글1", 1L);
        PostSearchResult post2 = createPostSearchResult(2L, "주간인기글2", 2L);
        List<PostSearchResult> posts = List.of(post1, post2);
        
        PostDetail fullPost1 = createPostDetail(1L, "주간인기글1", "내용1");
        PostDetail fullPost2 = createPostDetail(2L, "주간인기글2", "내용2");

        given(postCacheSyncPort.findWeeklyPopularPosts()).willReturn(posts);
        given(postCacheSyncPort.findPostDetail(1L)).willReturn(fullPost1);
        given(postCacheSyncPort.findPostDetail(2L)).willReturn(fullPost2);

        // When
        postCacheSyncService.updateWeeklyPopularPosts();

        // Then
        verify(postCacheCommandPort).resetPopularFlag(PostCacheFlag.WEEKLY);
        verify(postCacheCommandPort).applyPopularFlag(List.of(1L, 2L), PostCacheFlag.WEEKLY);
        verify(postCacheCommandPort).cachePostsWithDetails(PostCacheFlag.WEEKLY, List.of(fullPost1, fullPost2));

        // 이벤트 발행 검증
        ArgumentCaptor<PostFeaturedEvent> eventCaptor = ArgumentCaptor.forClass(PostFeaturedEvent.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
        
        List<PostFeaturedEvent> events = eventCaptor.getAllValues();
        assertThat(events).hasSize(2);
        assertThat(events.getFirst().getUserId()).isEqualTo(1L);
        assertThat(events.get(0).getSseMessage()).isEqualTo("주간 인기 게시글로 선정되었어요!");
        assertThat(events.get(0).getPostId()).isEqualTo(1L);
        assertThat(events.get(1).getUserId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("주간 인기 게시글 업데이트 - 익명 게시글 포함 (이벤트 발행 안함)")
    void shouldUpdateWeeklyPopularPosts_WhenAnonymousPostsIncluded() {
        // Given
        PostSearchResult anonymousPost = createPostSearchResult(1L, "익명글", null); // userId가 null
        PostSearchResult userPost = createPostSearchResult(2L, "회원글", 2L);
        List<PostSearchResult> posts = List.of(anonymousPost, userPost);
        
        PostDetail fullPost1 = createPostDetail(1L, "익명글", "내용1");
        PostDetail fullPost2 = createPostDetail(2L, "회원글", "내용2");

        given(postCacheSyncPort.findWeeklyPopularPosts()).willReturn(posts);
        given(postCacheSyncPort.findPostDetail(1L)).willReturn(fullPost1);
        given(postCacheSyncPort.findPostDetail(2L)).willReturn(fullPost2);

        // When
        postCacheSyncService.updateWeeklyPopularPosts();

        // Then
        verify(postCacheCommandPort).resetPopularFlag(PostCacheFlag.WEEKLY);
        verify(postCacheCommandPort).applyPopularFlag(List.of(1L, 2L), PostCacheFlag.WEEKLY);
        verify(postCacheCommandPort).cachePostsWithDetails(eq(PostCacheFlag.WEEKLY), any());

        // 익명 게시글은 이벤트 발행 안함, 회원 게시글만 이벤트 발행
        ArgumentCaptor<PostFeaturedEvent> eventCaptor = ArgumentCaptor.forClass(PostFeaturedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
        
        PostFeaturedEvent event = eventCaptor.getValue();
        assertThat(event.getUserId()).isEqualTo(2L);
        assertThat(event.getPostId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("전설의 게시글 업데이트 - 성공 (명예의 전당 메시지)")
    void shouldUpdateLegendaryPosts_WhenPostsExist() {
        // Given
        PostSearchResult legendPost = createPostSearchResult(1L, "전설의글", 1L);
        List<PostSearchResult> posts = List.of(legendPost);
        
        PostDetail fullPost = createPostDetail(1L, "전설의글", "전설적인 내용");

        given(postCacheSyncPort.findLegendaryPosts()).willReturn(posts);
        given(postCacheSyncPort.findPostDetail(1L)).willReturn(fullPost);

        // When
        postCacheSyncService.updateLegendaryPosts();

        // Then
        verify(postCacheCommandPort).resetPopularFlag(PostCacheFlag.LEGEND);
        verify(postCacheCommandPort).applyPopularFlag(List.of(1L), PostCacheFlag.LEGEND);
        verify(postCacheCommandPort).cachePostsWithDetails(PostCacheFlag.LEGEND, List.of(fullPost));

        // 명예의 전당 이벤트 검증
        ArgumentCaptor<PostFeaturedEvent> eventCaptor = ArgumentCaptor.forClass(PostFeaturedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        PostFeaturedEvent event = eventCaptor.getValue();
        assertThat(event.getUserId()).isEqualTo(1L);
        assertThat(event.getSseMessage()).isEqualTo("명예의 전당에 등극했어요!");
        assertThat(event.getFcmTitle()).isEqualTo("명예의 전당 등극");
        assertThat(event.getFcmBody()).contains("명예의 전당에 등극했습니다");
    }

    @Test
    @DisplayName("전설의 게시글 업데이트 - 게시글 상세 정보 null인 경우")
    void shouldUpdateLegendaryPosts_WhenPostDetailIsNull() {
        // Given
        PostSearchResult legendPost = createPostSearchResult(1L, "전설의글", 1L);
        List<PostSearchResult> posts = List.of(legendPost);

        given(postCacheSyncPort.findLegendaryPosts()).willReturn(posts);
        given(postCacheSyncPort.findPostDetail(1L)).willReturn(null);

        // When
        postCacheSyncService.updateLegendaryPosts();

        // Then
        verify(postCacheCommandPort).resetPopularFlag(PostCacheFlag.LEGEND);
        verify(postCacheCommandPort).applyPopularFlag(List.of(1L), PostCacheFlag.LEGEND);
        
        // 상세 정보가 null이면 빈 리스트로 캐시 메서드 호출됨
        verify(postCacheCommandPort).cachePostsWithDetails(PostCacheFlag.LEGEND, Collections.emptyList());
        
        // 이벤트는 여전히 발행됨
        verify(eventPublisher).publishEvent(any(PostFeaturedEvent.class));
    }

    @Test
    @DisplayName("공지 캐시 삭제 - 성공")
    void shouldDeleteNoticeCache_Successfully() {
        // When
        postCacheSyncService.deleteNoticeCache();

        // Then
        verify(postCacheCommandPort).deletePopularPostsCache(PostCacheFlag.NOTICE);
    }


    @Test
    @DisplayName("스케줄링 메서드들의 트랜잭션 동작 검증")
    void shouldVerifyTransactionalBehavior() {
        // Given
        given(postCacheSyncPort.findRealtimePopularPosts()).willReturn(Collections.emptyList());
        given(postCacheSyncPort.findWeeklyPopularPosts()).willReturn(Collections.emptyList());
        given(postCacheSyncPort.findLegendaryPosts()).willReturn(Collections.emptyList());

        // When - 모든 스케줄링 메서드 호출
        postCacheSyncService.updateRealtimePopularPosts();
        postCacheSyncService.updateWeeklyPopularPosts();
        postCacheSyncService.updateLegendaryPosts();

        // Then - @Transactional 동작을 위한 port 호출 검증
        verify(postCacheCommandPort, times(3)).resetPopularFlag(any());
        verify(postCacheSyncPort).findRealtimePopularPosts();
        verify(postCacheSyncPort).findWeeklyPopularPosts();
        verify(postCacheSyncPort).findLegendaryPosts();
    }

    @Test
    @DisplayName("대량 게시글 처리 - 성능 테스트 시나리오")
    void shouldHandleLargeNumberOfPosts_PerformanceScenario() {
        // Given - 대량의 게시글 생성 (1000개로 원복)
        List<PostSearchResult> largePosts = createLargePostList(1000);
        List<Long> postIds = largePosts.stream().map(PostSearchResult::getId).toList();

        given(postCacheSyncPort.findWeeklyPopularPosts()).willReturn(largePosts);
        // 모든 상세 정보는 null로 설정하여 상세 캐싱 건너뛰기
        largePosts.forEach(post -> given(postCacheSyncPort.findPostDetail(post.getId())).willReturn(null));

        // When
        postCacheSyncService.updateWeeklyPopularPosts();

        // Then
        verify(postCacheCommandPort).resetPopularFlag(PostCacheFlag.WEEKLY);
        verify(postCacheCommandPort).applyPopularFlag(postIds, PostCacheFlag.WEEKLY);
        verify(postCacheCommandPort).cachePostsWithDetails(eq(PostCacheFlag.WEEKLY), any());
        
        // 1000개 게시글 중 userId가 있는 것들만 이벤트 발행 (500개)
        verify(eventPublisher, times(500)).publishEvent(any(PostFeaturedEvent.class));
    }

    // 테스트 유틸리티 메서드들
    private PostSearchResult createPostSearchResult(Long id, String title, Long userId) {
        return PostSearchResult.builder()
                .id(id)
                .title(title)
                .userId(userId)
                .build();
    }

    private PostDetail createPostDetail(Long id, String title, String content) {
        return PostDetail.builder()
                .id(id)
                .title(title)
                .content(content)
                .viewCount(0)
                .likeCount(0)
                .postCacheFlag(null)
                .createdAt(java.time.Instant.now())
                .userId(1L)
                .userName("테스트 사용자")
                .commentCount(0)
                .isNotice(false)
                .isLiked(false)
                .build();
    }

    private List<PostSearchResult> createLargePostList(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> createPostSearchResult(
                        (long) i, 
                        "제목" + i, 
                        i % 2 == 0 ? (long) i : null // 짝수만 userId 설정
                ))
                .toList();
    }
}