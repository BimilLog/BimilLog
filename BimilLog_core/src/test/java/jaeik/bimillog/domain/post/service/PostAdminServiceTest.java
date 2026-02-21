package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.entity.jpa.Post;
import jaeik.bimillog.domain.post.repository.PostRepository;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostListDeleteAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostListUpdateAdapter;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.fixtures.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * <h2>PostAdminService 테스트</h2>
 * <p>게시글 공지사항 서비스의 핵심 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>공지 설정: Post.isNotice true + JSON LIST에 LPUSH</p>
 * <p>공지 해제: Post.isNotice false + JSON LIST에서 LREM</p>
 *
 * @author Jaeik
 * @version 4.0.0
 */
@DisplayName("PostAdminService 테스트")
@Tag("unit")
class PostAdminServiceTest extends BaseUnitTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private RedisPostListUpdateAdapter redisPostListUpdateAdapter;

    @Mock
    private RedisPostListDeleteAdapter redisPostListDeleteAdapter;

    @InjectMocks
    private PostAdminService postAdminService;

    @Test
    @DisplayName("게시글 공지 토글 - 일반 게시글을 공지로 설정 → Post.isNotice true + JSON LIST LPUSH")
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
        TestFixtures.setFieldValue(post, "id", postId);

        given(postRepository.findById(postId)).willReturn(Optional.of(post));

        // When - 현재 공지가 아닌 상태(false)를 전달
        postAdminService.togglePostNotice(postId, false);

        // Then
        verify(postRepository).findById(postId);
        verify(redisPostListUpdateAdapter).addPostToList(eq(RedisKey.POST_NOTICE_JSON_KEY), any(PostSimpleDetail.class), anyInt());
    }

    @Test
    @DisplayName("게시글 공지 토글 - 존재하지 않는 게시글")
    void shouldThrowException_WhenToggleNonExistentPost() {
        // Given
        Long postId = 999L;

        given(postRepository.findById(postId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> postAdminService.togglePostNotice(postId, false))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_NOT_FOUND);

        verify(postRepository).findById(postId);
        verify(redisPostListDeleteAdapter, never()).removePost(eq(postId));
    }

    @Test
    @DisplayName("게시글 공지 토글 - 공지 게시글을 일반 게시글로 해제 → Post.isNotice false + JSON LIST LREM")
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

        // When - 현재 공지 상태(true)를 전달
        postAdminService.togglePostNotice(postId, true);

        // Then
        verify(postRepository).findById(postId);
        // JSON LIST에서 제거
        verify(redisPostListDeleteAdapter).removePost(postId);
    }
}
