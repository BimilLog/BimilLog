package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.adapter.PostToMemberAdapter;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostReadModelQueryRepository;
import jaeik.bimillog.infrastructure.redis.post.RedisFirstPagePostAdapter;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.builder.PostTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>FirstPageCacheService 테스트</h2>
 * <p>첫 페이지 캐시 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@DisplayName("FirstPageCacheService 테스트")
@Tag("unit")
class FirstPageCacheServiceTest extends BaseUnitTest {

    @Mock
    private RedisFirstPagePostAdapter redisFirstPagePostAdapter;

    @Mock
    private PostReadModelQueryRepository postReadModelQueryRepository;

    @Mock
    private PostToMemberAdapter postToMemberAdapter;

    @InjectMocks
    private FirstPageCacheService firstPageCacheService;

    @Test
    @DisplayName("첫 페이지 조회 - 캐시 히트 (비회원)")
    void shouldGetFirstPage_CacheHit_Guest() {
        // Given
        PostSimpleDetail post1 = PostTestDataBuilder.createPostSearchResult(1L, "게시글1");
        PostSimpleDetail post2 = PostTestDataBuilder.createPostSearchResult(2L, "게시글2");
        List<PostSimpleDetail> cachedPosts = List.of(post1, post2);

        given(redisFirstPagePostAdapter.getFirstPage()).willReturn(cachedPosts);

        // When
        List<PostSimpleDetail> result = firstPageCacheService.getFirstPage(null);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("게시글1");

        verify(redisFirstPagePostAdapter).getFirstPage();
        verify(postReadModelQueryRepository, never()).findBoardPostsByCursor(any(), anyInt());
    }

    @Test
    @DisplayName("첫 페이지 조회 - 캐시 미스 시 DB 폴백 (비회원)")
    void shouldGetFirstPage_CacheMiss_FallbackToDB_Guest() {
        // Given
        PostSimpleDetail post = PostTestDataBuilder.createPostSearchResult(1L, "DB게시글");
        List<PostSimpleDetail> dbPosts = List.of(post);

        given(redisFirstPagePostAdapter.getFirstPage()).willReturn(Collections.emptyList());
        given(postReadModelQueryRepository.findBoardPostsByCursor(null, 20)).willReturn(dbPosts);

        // When
        List<PostSimpleDetail> result = firstPageCacheService.getFirstPage(null);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("DB게시글");

        verify(redisFirstPagePostAdapter).getFirstPage();
        verify(postReadModelQueryRepository).findBoardPostsByCursor(null, 20);
    }

    @Test
    @DisplayName("첫 페이지 조회 - 캐시 히트 + 블랙리스트 필터링 (회원)")
    void shouldGetFirstPage_CacheHit_WithBlacklistFiltering_Member() {
        // Given
        Long memberId = 100L;
        Long blacklistedMemberId = 2L;

        PostSimpleDetail post1 = PostTestDataBuilder.createPostSearchResultWithMemberId(1L, "게시글1", 1L);
        PostSimpleDetail post2 = PostTestDataBuilder.createPostSearchResultWithMemberId(2L, "블랙게시글", blacklistedMemberId);
        PostSimpleDetail post3 = PostTestDataBuilder.createPostSearchResultWithMemberId(3L, "게시글3", 3L);
        List<PostSimpleDetail> cachedPosts = List.of(post1, post2, post3);

        given(redisFirstPagePostAdapter.getFirstPage()).willReturn(cachedPosts);
        given(postToMemberAdapter.getInterActionBlacklist(memberId)).willReturn(List.of(blacklistedMemberId));

        // When
        List<PostSimpleDetail> result = firstPageCacheService.getFirstPage(memberId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(PostSimpleDetail::getId).containsExactly(1L, 3L);

        verify(redisFirstPagePostAdapter).getFirstPage();
        verify(postToMemberAdapter).getInterActionBlacklist(memberId);
    }

    @Test
    @DisplayName("isFirstPage - cursor가 null이고 size가 20 이하면 true")
    void shouldReturnTrue_WhenCursorNullAndSizeLessThanOrEqual20() {
        // When & Then
        assertThat(firstPageCacheService.isFirstPage(null, 10)).isTrue();
        assertThat(firstPageCacheService.isFirstPage(null, 20)).isTrue();
    }

    @Test
    @DisplayName("isFirstPage - cursor가 있으면 false")
    void shouldReturnFalse_WhenCursorExists() {
        // When & Then
        assertThat(firstPageCacheService.isFirstPage(100L, 10)).isFalse();
        assertThat(firstPageCacheService.isFirstPage(1L, 20)).isFalse();
    }

    @Test
    @DisplayName("isFirstPage - size가 20 초과면 false")
    void shouldReturnFalse_WhenSizeGreaterThan20() {
        // When & Then
        assertThat(firstPageCacheService.isFirstPage(null, 21)).isFalse();
        assertThat(firstPageCacheService.isFirstPage(null, 50)).isFalse();
    }
}
