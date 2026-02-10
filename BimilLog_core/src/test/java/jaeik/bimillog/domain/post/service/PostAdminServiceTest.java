package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.jpa.Post;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.domain.post.repository.PostRepository;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostHashAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostIndexAdapter;
import jaeik.bimillog.testutil.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * <h2>PostAdminService 테스트</h2>
 * <p>게시글 공지사항 서비스의 핵심 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>공지 설정: Post.isNotice true + 글 단위 Hash 생성 + List 인덱스 LPUSH</p>
 * <p>공지 해제: Post.isNotice false + List 인덱스 LREM (Hash는 유지)</p>
 *
 * @author Jaeik
 * @version 3.0.0
 */
@DisplayName("PostAdminService 테스트")
@Tag("unit")
class PostAdminServiceTest extends BaseUnitTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostQueryRepository postQueryRepository;

    @Mock
    private RedisPostHashAdapter redisPostHashAdapter;

    @Mock
    private RedisPostIndexAdapter redisPostIndexAdapter;

    @InjectMocks
    private PostAdminService postAdminService;

    @Test
    @DisplayName("게시글 공지 토글 - 일반 게시글을 공지로 설정 → Post.isNotice true + Hash 생성 + List LPUSH")
    void shouldTogglePostNotice_WhenNormalPostToNotice() {
        // Given
        Long postId = 123L;
        Post post = Post.builder()
                .title("테스트 공지")
                .content("테스트 내용")
                .views(100)
                .likeCount(10)
                .commentCount(5)
                .build();

        PostSimpleDetail mockDetail = PostSimpleDetail.builder()
                .id(postId)
                .title("테스트 공지")
                .viewCount(100)
                .likeCount(10)
                .createdAt(Instant.now())
                .memberId(1L)
                .memberName("테스트")
                .commentCount(5)
                .isNotice(true)
                .build();

        given(postRepository.findById(postId)).willReturn(Optional.of(post));
        given(postQueryRepository.findPostSimpleDetailById(postId)).willReturn(Optional.of(mockDetail));

        // When
        postAdminService.togglePostNotice(postId);

        // Then
        verify(postRepository).findById(postId);
        // 글 단위 Hash 생성 + List 인덱스 LPUSH
        verify(redisPostHashAdapter).createPostHash(mockDetail);
        verify(redisPostIndexAdapter).addToIndex(RedisKey.POST_NOTICE_IDS_KEY, postId);
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
        verify(redisPostIndexAdapter, never()).removeFromIndex(RedisKey.POST_NOTICE_IDS_KEY, postId);
    }

    @Test
    @DisplayName("게시글 공지 토글 - 공지 게시글을 일반 게시글로 해제 → Post.isNotice false + List LREM")
    void shouldTogglePostNotice_WhenNoticePostToNormal() {
        // Given
        Long postId = 123L;
        Post post = Post.builder()
                .title("테스트 공지")
                .content("테스트 내용")
                .views(100)
                .likeCount(10)
                .commentCount(5)
                .build();
        post.updateNotice(true);

        given(postRepository.findById(postId)).willReturn(Optional.of(post));

        // When
        postAdminService.togglePostNotice(postId);

        // Then
        verify(postRepository).findById(postId);
        // List 인덱스에서 제거 (Hash는 삭제하지 않음 - 다른 목록에서 참조 가능)
        verify(redisPostIndexAdapter).removeFromIndex(RedisKey.POST_NOTICE_IDS_KEY, postId);
    }
}
