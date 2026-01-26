package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.jpa.Post;
import jaeik.bimillog.domain.post.entity.jpa.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.FeaturedPostRepository;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.domain.post.repository.PostRepository;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.infrastructure.redis.post.RedisPostKeys;
import jaeik.bimillog.infrastructure.redis.post.RedisSimplePostAdapter;
import jaeik.bimillog.testutil.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * <h2>PostAdminService 테스트</h2>
 * <p>게시글 공지사항 서비스의 핵심 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>공지 설정/해제 시 Hash 캐시에 직접 추가/제거합니다.</p>
 *
 * @author Jaeik
 * @version 2.6.0
 */
@DisplayName("PostAdminService 테스트")
@Tag("unit")
class PostAdminServiceTest extends BaseUnitTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostQueryRepository postQueryRepository;

    @Mock
    private RedisSimplePostAdapter redisSimplePostAdapter;

    @Mock
    private FeaturedPostRepository featuredPostRepository;

    @Mock
    private Post post;

    @InjectMocks
    private PostAdminService postAdminService;

    @Test
    @DisplayName("게시글 공지 토글 - 일반 게시글을 공지로 설정")
    void shouldTogglePostNotice_WhenNormalPostToNotice() {
        // Given
        Long postId = 123L;
        PostSimpleDetail mockDetail = PostSimpleDetail.builder()
                .id(postId)
                .title("테스트 공지")
                .viewCount(100)
                .likeCount(10)
                .createdAt(Instant.now())
                .memberId(1L)
                .memberName("테스트")
                .commentCount(5)
                .build();

        given(postRepository.findById(postId)).willReturn(Optional.of(post));
        given(post.isNotice()).willReturn(false); // 현재 공지 아님
        given(postQueryRepository.findPostSimpleDetailsByIds(List.of(postId))).willReturn(List.of(mockDetail));

        // When
        postAdminService.togglePostNotice(postId);

        // Then
        verify(postRepository).findById(postId);
        verify(post).isNotice(); // 상태 확인 (if문)
        verify(post).setAsNotice();
        verify(postQueryRepository).findPostSimpleDetailsByIds(List.of(postId));
        verify(redisSimplePostAdapter).addPostToCache(PostCacheFlag.NOTICE, postId, mockDetail, RedisPostKeys.POST_CACHE_TTL_NOTICE);
    }

    @Test
    @DisplayName("게시글 공지 토글 - 존재하지 않는 게시글")
    void shouldThrowException_WhenToggleNonExistentPost() {
        // Given
        Long postId = 999L;

        given(postRepository.findById(postId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> postAdminService.togglePostNotice(postId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_NOT_FOUND);

        verify(postRepository).findById(postId);
        verify(post, never()).isNotice();
        verify(post, never()).setAsNotice();
        verify(post, never()).unsetAsNotice();
    }

    @Test
    @DisplayName("게시글 공지 토글 - null postId")
    void shouldThrowException_WhenTogglePostWithNullId() {
        // Given
        Long postId = null;

        // When & Then
        assertThatThrownBy(() -> postAdminService.togglePostNotice(postId))
                .isInstanceOf(Exception.class);

        verify(postRepository).findById(postId);
        verify(post, never()).isNotice();
        verify(post, never()).setAsNotice();
        verify(post, never()).unsetAsNotice();
    }

    @Test
    @DisplayName("게시글 공지 토글 - 공지 게시글을 일반 게시글로 해제")
    void shouldTogglePostNotice_WhenNoticePostToNormal() {
        // Given
        Long postId = 123L;

        given(postRepository.findById(postId)).willReturn(Optional.of(post));
        given(post.isNotice()).willReturn(true); // 현재 공지임

        // When
        postAdminService.togglePostNotice(postId);

        // Then
        verify(postRepository).findById(postId);
        verify(post).isNotice(); // 상태 확인 (if문)
        verify(post).unsetAsNotice();
        verify(redisSimplePostAdapter).removePostFromCache(PostCacheFlag.NOTICE, postId);
    }


}
