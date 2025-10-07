package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.application.port.out.PostQueryPort;
import jaeik.bimillog.domain.post.application.port.out.RedisPostCommandPort;
import jaeik.bimillog.domain.post.application.port.out.RedisPostQueryPort;
import jaeik.bimillog.domain.post.application.service.PostCacheService;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.exception.PostErrorCode;
import jaeik.bimillog.testutil.PostTestDataBuilder;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
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
    private RedisPostCommandPort redisPostCommandPort;

    @Mock
    private PostQueryPort postQueryPort;

    @Mock
    private RedisPostQueryPort redisPostQueryPort;

    @InjectMocks
    private PostCacheService postCacheService;

    @Test
    @DisplayName("공지사항 캐시 추가 - 게시글 상세 존재")
    void shouldAddNoticeToCacheWhenDetailExists() {
        // Given
        Long postId = 1L;
        PostDetail detail = PostDetail.builder()
                .id(postId)
                .title("공지사항")
                .content("본문")
                .viewCount(10)
                .likeCount(2)
                .createdAt(Instant.now())
                .memberId(5L)
                .memberName("작성자")
                .commentCount(0)
                .isLiked(false)
                .build();
        given(postQueryPort.findPostDetailWithCounts(postId, null)).willReturn(Optional.of(detail));

        // When
        postCacheService.syncNoticeCache(postId, true);

        // Then (주간/레전드와 동일하게 postId만 저장)
        verify(redisPostCommandPort).cachePostIds(eq(PostCacheFlag.NOTICE), argThat(searchResults ->
                searchResults.size() == 1 && searchResults.get(0).postId().equals(postId)
        ));
    }

    @Test
    @DisplayName("공지사항 캐시 추가 - 게시글 상세 없음")
    void shouldSkipCacheWhenDetailMissing() {
        // Given
        Long postId = 1L;
        given(postQueryPort.findPostDetailWithCounts(postId, null)).willReturn(Optional.empty());

        // When
        postCacheService.syncNoticeCache(postId, true);

        // Then
        verify(postQueryPort).findPostDetailWithCounts(postId, null);
        verify(redisPostCommandPort, never()).cachePostIds(any(), any());
    }

    @Test
    @DisplayName("공지사항 캐시 제거")
    void shouldRemoveNoticeFromCache() {
        // Given
        Long postId = 10L;

        // When
        postCacheService.syncNoticeCache(postId, false);

        // Then
        verify(redisPostCommandPort).deleteCache(null, postId, PostCacheFlag.NOTICE);
        verify(postQueryPort, never()).findPostDetailWithCounts(any(), any());
    }

    @Test
    @DisplayName("실시간 인기글 조회")
    void shouldGetRealtimePosts() {
        // Given
        PostDetail realtimePost1 = createPostDetail(1L, "실시간 인기글 1");
        PostDetail realtimePost2 = createPostDetail(2L, "실시간 인기글 2");

        given(redisPostQueryPort.getRealtimePopularPostIds()).willReturn(List.of(1L, 2L));
        given(redisPostQueryPort.getCachedPostIfExists(1L)).willReturn(realtimePost1);
        given(redisPostQueryPort.getCachedPostIfExists(2L)).willReturn(realtimePost2);

        // When
        List<PostSimpleDetail> result = postCacheService.getRealtimePosts();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("실시간 인기글 1");
        assertThat(result.get(1).getTitle()).isEqualTo("실시간 인기글 2");

        verify(redisPostQueryPort).getRealtimePopularPostIds();
        verify(redisPostQueryPort).getCachedPostIfExists(1L);
        verify(redisPostQueryPort).getCachedPostIfExists(2L);
    }

    @Test
    @DisplayName("실시간 인기글 조회 - 캐시 미스")
    void shouldGetRealtimePosts_WhenCacheMissing() {
        // Given
        PostDetail realtimePost1 = createPostDetail(1L, "업데이트된 실시간");

        given(redisPostQueryPort.getRealtimePopularPostIds()).willReturn(List.of(1L));
        given(redisPostQueryPort.getCachedPostIfExists(1L)).willReturn(null); // Cache miss
        given(postQueryPort.findPostDetailWithCounts(1L, null)).willReturn(Optional.of(realtimePost1)); // DB fallback

        // When
        List<PostSimpleDetail> result = postCacheService.getRealtimePosts();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("업데이트된 실시간");

        verify(redisPostQueryPort).getRealtimePopularPostIds();
        verify(redisPostQueryPort).getCachedPostIfExists(1L);
        verify(postQueryPort).findPostDetailWithCounts(1L, null);
        verify(redisPostCommandPort).cachePostDetail(realtimePost1);
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
    @DisplayName("레전드 인기 게시글 페이징 조회 - 잘못된 타입으로 호출시 예외 발생")
    void shouldThrowException_WhenGetPopularPostLegendWithNonLegendType() {
        // Given
        PostCacheFlag type = PostCacheFlag.REALTIME; // LEGEND가 아닌 타입
        Pageable pageable = PageRequest.of(0, 10);

        // When & Then
        assertThatThrownBy(() -> postCacheService.getPopularPostLegend(type, pageable))
                .isInstanceOf(PostCustomException.class)
                .hasFieldOrPropertyWithValue("postErrorCode", PostErrorCode.INVALID_INPUT_VALUE);

        // 타입 검증에서 바로 예외가 발생하므로 다른 메서드들은 호출되지 않음
        verify(redisPostQueryPort, never()).getCachedPostListPaged(any());
    }

    @Test
    @DisplayName("레전드 인기 게시글 페이징 조회 - null 타입으로 호출시 예외 발생")
    void shouldThrowException_WhenGetPopularPostLegendWithNullType() {
        // Given
        PostCacheFlag type = null;
        Pageable pageable = PageRequest.of(0, 10);

        // When & Then
        assertThatThrownBy(() -> postCacheService.getPopularPostLegend(type, pageable))
                .isInstanceOf(PostCustomException.class)
                .hasFieldOrPropertyWithValue("postErrorCode", PostErrorCode.INVALID_INPUT_VALUE);

        verify(redisPostQueryPort, never()).getCachedPostListPaged(any());
    }

    @Test
    @DisplayName("공지사항 조회 - 성공")
    void shouldGetNoticePosts_Successfully() {
        // Given
        PostSimpleDetail noticePost = PostTestDataBuilder.createPostSearchResult(1L, "공지사항");
        List<PostSimpleDetail> noticePosts = List.of(noticePost);

        given(redisPostQueryPort.getCachedPostList(PostCacheFlag.NOTICE)).willReturn(noticePosts);

        // When
        List<PostSimpleDetail> result = postCacheService.getNoticePosts();

        // Then
        assertThat(result).isEqualTo(noticePosts);
        assertThat(result).hasSize(1);

        verify(redisPostQueryPort).getCachedPostList(PostCacheFlag.NOTICE);
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
