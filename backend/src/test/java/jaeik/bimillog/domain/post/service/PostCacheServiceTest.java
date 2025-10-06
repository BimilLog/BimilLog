package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.application.port.out.PostQueryPort;
import jaeik.bimillog.domain.post.application.port.out.RedisPostCommandPort;
import jaeik.bimillog.domain.post.application.service.PostCacheService;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * <h2>PostCacheService 테스트</h2>
 * <p>공지사항 캐시 동기화의 분기 로직을 검증합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostCacheService 테스트")
@Tag("unit")
class PostCacheServiceTest {

    @Mock
    private RedisPostCommandPort redisPostCommandPort;

    @Mock
    private PostQueryPort postQueryPort;

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
        given(postQueryPort.findPostDetail(postId)).willReturn(detail);

        // When
        postCacheService.syncNoticeCache(postId, true);

        // Then (주간/레전드와 동일하게 postId만 저장)
        verify(redisPostCommandPort).cachePostIds(eq(PostCacheFlag.NOTICE), argThat(searchResults ->
                searchResults.size() == 1 && searchResults.get(0).getId().equals(postId)
        ));
    }

    @Test
    @DisplayName("공지사항 캐시 추가 - 게시글 상세 없음")
    void shouldSkipCacheWhenDetailMissing() {
        // Given
        Long postId = 1L;
        given(postQueryPort.findPostDetail(postId)).willReturn(null);

        // When
        postCacheService.syncNoticeCache(postId, true);

        // Then
        verify(postQueryPort).findPostDetail(postId);
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
        verify(postQueryPort, never()).findPostDetail(any());
    }
}
