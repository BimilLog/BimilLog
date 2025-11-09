package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.testutil.builder.PostTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * <h2>PostCacheService 테스트</h2>
 * <p>게시글 캐시 조회 및 동기화 로직을 검증합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostCacheService 테스트")
@Tag("unit")
class PostCacheServiceTest {

    @Mock
    private RedisPostSavePort redisPostSavePort;

    @Mock
    private PostQueryPort postQueryPort;

    @Mock
    private RedisPostQueryPort redisPostQueryPort;

    @InjectMocks
    private PostCacheService postCacheService;

    @Test
    @DisplayName("실시간 인기글 조회 - 캐시 히트")
    void shouldGetRealtimePosts() {
        // Given
        PostSimpleDetail simpleDetail1 = PostTestDataBuilder.createPostSearchResult(1L, "실시간 인기글 1");
        PostSimpleDetail simpleDetail2 = PostTestDataBuilder.createPostSearchResult(2L, "실시간 인기글 2");

        given(redisPostQueryPort.getRealtimePopularPostIds()).willReturn(List.of(1L, 2L));
        given(redisPostQueryPort.getCachedPostList(PostCacheFlag.REALTIME)).willReturn(List.of(simpleDetail1, simpleDetail2));

        // When
        List<PostSimpleDetail> result = postCacheService.getRealtimePosts();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("실시간 인기글 1");
        assertThat(result.get(1).getTitle()).isEqualTo("실시간 인기글 2");

        verify(redisPostQueryPort).getRealtimePopularPostIds();
        verify(redisPostQueryPort).getCachedPostList(PostCacheFlag.REALTIME);
    }

    @Test
    @DisplayName("실시간 인기글 조회 - 캐시 미스")
    void shouldGetRealtimePosts_WhenCacheMissing() {
        // Given
        PostSimpleDetail simpleDetail2 = PostTestDataBuilder.createPostSearchResult(2L, "캐시된 게시글");
        PostDetail realtimePost1 = createPostDetail(1L, "DB에서 조회된 게시글");

        given(redisPostQueryPort.getRealtimePopularPostIds()).willReturn(List.of(1L, 2L));
        given(redisPostQueryPort.getCachedPostList(PostCacheFlag.REALTIME)).willReturn(List.of(simpleDetail2)); // postId=1은 캐시 미스
        given(postQueryPort.findPostDetailWithCounts(1L, null)).willReturn(Optional.of(realtimePost1)); // DB fallback

        // When
        List<PostSimpleDetail> result = postCacheService.getRealtimePosts();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("DB에서 조회된 게시글");
        assertThat(result.get(1).getTitle()).isEqualTo("캐시된 게시글");

        verify(redisPostQueryPort).getRealtimePopularPostIds();
        verify(redisPostQueryPort).getCachedPostList(PostCacheFlag.REALTIME);
        verify(postQueryPort).findPostDetailWithCounts(1L, null);
        verify(redisPostSavePort).cachePostList(eq(PostCacheFlag.REALTIME), any());
    }

    @Test
    @DisplayName("주간 인기글 조회")
    void shouldGetWeeklyPosts() {
        // Given
        List<PostSimpleDetail> weeklyPosts = List.of(
            PostTestDataBuilder.createPostSearchResult(1L, "주간 인기글 1"),
            PostTestDataBuilder.createPostSearchResult(2L, "주간 인기글 2")
        );

        given(redisPostQueryPort.getCachedPostList(PostCacheFlag.WEEKLY)).willReturn(weeklyPosts);

        // When
        List<PostSimpleDetail> result = postCacheService.getWeeklyPosts();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("주간 인기글 1");
        assertThat(result.get(1).getTitle()).isEqualTo("주간 인기글 2");

        verify(redisPostQueryPort).getCachedPostList(PostCacheFlag.WEEKLY);
    }

    @Test
    @DisplayName("레전드 인기 게시글 페이징 조회")
    void shouldGetPopularPostLegend() {
        // Given
        PostCacheFlag type = PostCacheFlag.LEGEND;
        Pageable pageable = PageRequest.of(0, 10);

        PostSimpleDetail legendPost1 = PostTestDataBuilder.createPostSearchResult(1L, "레전드 게시글 1");
        PostSimpleDetail legendPost2 = PostTestDataBuilder.createPostSearchResult(2L, "레전드 게시글 2");
        Page<PostSimpleDetail> expectedPage = new PageImpl<>(List.of(legendPost1, legendPost2), pageable, 2);

        given(redisPostQueryPort.getCachedPostListPaged(pageable)).willReturn(expectedPage);

        // When
        Page<PostSimpleDetail> result = postCacheService.getPopularPostLegend(type, pageable);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("레전드 게시글 1");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("레전드 게시글 2");

        verify(redisPostQueryPort).getCachedPostListPaged(pageable);
    }

    @Test
    @DisplayName("레전드 인기 게시글 페이징 조회 - 캐시 미스 복구")
    void shouldGetPopularPostLegend_WhenCacheMiss() {
        // Given
        PostCacheFlag type = PostCacheFlag.LEGEND;
        Pageable pageable = PageRequest.of(0, 10);

        PostDetail postDetail1 = createPostDetail(1L, "레전드 게시글 1");
        PostDetail postDetail2 = createPostDetail(2L, "레전드 게시글 2");

        // 첫 번째 조회: 빈 페이지
        Page<PostSimpleDetail> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        PostSimpleDetail simpleDetail1 = PostTestDataBuilder.createPostSearchResult(1L, "레전드 게시글 1");
        PostSimpleDetail simpleDetail2 = PostTestDataBuilder.createPostSearchResult(2L, "레전드 게시글 2");
        Page<PostSimpleDetail> recoveredPage = new PageImpl<>(List.of(simpleDetail1, simpleDetail2), pageable, 2);

        given(redisPostQueryPort.getCachedPostListPaged(pageable))
                .willReturn(emptyPage)  // 첫 번째 호출: 캐시 미스
                .willReturn(recoveredPage); // 두 번째 호출: 복구 후
        given(redisPostQueryPort.getStoredPostIds(PostCacheFlag.LEGEND)).willReturn(List.of(1L, 2L));
        given(postQueryPort.findPostDetailWithCounts(1L, null)).willReturn(Optional.of(postDetail1));
        given(postQueryPort.findPostDetailWithCounts(2L, null)).willReturn(Optional.of(postDetail2));

        // When
        Page<PostSimpleDetail> result = postCacheService.getPopularPostLegend(type, pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("레전드 게시글 1");

        verify(redisPostQueryPort, times(2)).getCachedPostListPaged(pageable);
        verify(redisPostQueryPort).getStoredPostIds(PostCacheFlag.LEGEND);
        verify(postQueryPort).findPostDetailWithCounts(1L, null);
        verify(postQueryPort).findPostDetailWithCounts(2L, null);
        verify(redisPostSavePort).cachePostList(eq(PostCacheFlag.LEGEND), any());
    }

    @Test
    @DisplayName("공지사항 조회 - 성공")
    void shouldGetNoticePosts_Successfully() {
        // Given
        PostSimpleDetail noticePost = PostTestDataBuilder.createPostSearchResult(1L, "공지사항");
        List<PostSimpleDetail> noticePosts = List.of(noticePost);

        given(redisPostQueryPort.getStoredPostIds(PostCacheFlag.NOTICE)).willReturn(List.of(1L));
        given(redisPostQueryPort.getCachedPostList(PostCacheFlag.NOTICE)).willReturn(noticePosts);

        // When
        List<PostSimpleDetail> result = postCacheService.getNoticePosts();

        // Then
        assertThat(result).isEqualTo(noticePosts);
        assertThat(result).hasSize(1);

        verify(redisPostQueryPort).getStoredPostIds(PostCacheFlag.NOTICE);
        verify(redisPostQueryPort).getCachedPostList(PostCacheFlag.NOTICE);
    }

    @Test
    @DisplayName("주간 인기글 조회 - 캐시 미스 복구")
    void shouldGetWeeklyPosts_WhenCacheMiss() {
        // Given
        PostDetail postDetail1 = createPostDetail(1L, "주간 인기글 1");
        PostDetail postDetail2 = createPostDetail(2L, "주간 인기글 2");

        given(redisPostQueryPort.getCachedPostList(PostCacheFlag.WEEKLY)).willReturn(List.of());  // 캐시 미스
        given(redisPostQueryPort.getStoredPostIds(PostCacheFlag.WEEKLY)).willReturn(List.of(1L, 2L));
        given(postQueryPort.findPostDetailWithCounts(1L, null)).willReturn(Optional.of(postDetail1));
        given(postQueryPort.findPostDetailWithCounts(2L, null)).willReturn(Optional.of(postDetail2));

        // When
        List<PostSimpleDetail> result = postCacheService.getWeeklyPosts();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("주간 인기글 1");
        assertThat(result.get(1).getTitle()).isEqualTo("주간 인기글 2");

        verify(redisPostQueryPort).getCachedPostList(PostCacheFlag.WEEKLY);
        verify(redisPostQueryPort).getStoredPostIds(PostCacheFlag.WEEKLY);
        verify(postQueryPort).findPostDetailWithCounts(1L, null);
        verify(postQueryPort).findPostDetailWithCounts(2L, null);
        verify(redisPostSavePort).cachePostList(eq(PostCacheFlag.WEEKLY), any());  // 재캐싱 검증
    }

    @Test
    @DisplayName("공지사항 조회 - 캐시 미스 복구")
    void shouldGetNoticePosts_WhenCacheMiss() {
        // Given
        PostDetail postDetail1 = createPostDetail(1L, "공지사항 1");

        given(redisPostQueryPort.getStoredPostIds(PostCacheFlag.NOTICE)).willReturn(List.of(1L));
        given(redisPostQueryPort.getCachedPostList(PostCacheFlag.NOTICE)).willReturn(List.of());  // 캐시 미스 (size 불일치)
        given(postQueryPort.findPostDetailWithCounts(1L, null)).willReturn(Optional.of(postDetail1));

        // When
        List<PostSimpleDetail> result = postCacheService.getNoticePosts();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("공지사항 1");

        verify(redisPostQueryPort, times(2)).getStoredPostIds(PostCacheFlag.NOTICE);  // 1번: size 비교, 2번: recoverFromStoredPostIds
        verify(redisPostQueryPort).getCachedPostList(PostCacheFlag.NOTICE);
        verify(postQueryPort).findPostDetailWithCounts(1L, null);
    }

    // Helper method for creating PostDetail test data
    private PostDetail createPostDetail(Long id, String title) {
        return PostDetail.builder()
                .id(id)
                .title(title)
                .content("테스트 내용")
                .viewCount(0)
                .likeCount(0)
                .createdAt(Instant.now())
                .memberId(1L)
                .memberName("테스트 사용자")
                .commentCount(0)
                .isLiked(false)
                .build();
    }
}
