package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.application.port.out.PostCommandPort;
import jaeik.bimillog.domain.post.application.port.out.PostLikeCommandPort;
import jaeik.bimillog.domain.post.application.port.out.PostLikeQueryPort;
import jaeik.bimillog.domain.post.application.port.out.PostQueryPort;
import jaeik.bimillog.domain.post.application.service.PostInteractionService;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostLike;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.exception.PostErrorCode;
import jaeik.bimillog.global.application.port.out.GlobalUserQueryPort;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * <h2>PostInteractionService 테스트</h2>
 * <p>게시글 상호작용 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>추천/추천취소, 조회수 증가 등의 다양한 시나리오를 테스트합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("PostInteractionService 테스트")
class PostInteractionServiceTest extends BaseUnitTest {

    @Mock
    private PostCommandPort postCommandPort;

    @Mock
    private PostQueryPort postQueryPort;

    @Mock
    private PostLikeCommandPort postLikeCommandPort;

    @Mock
    private PostLikeQueryPort postLikeQueryPort;

    @Mock
    private GlobalUserQueryPort globalUserQueryPort;

    @InjectMocks
    private PostInteractionService postInteractionService;

    @Test
    @DisplayName("게시글 추천 - 처음 추천하는 경우")
    void shouldAddLike_WhenFirstTimeLiking() {
        // Given
        Long userId = 1L;
        Long postId = 123L;
        Post post = TestFixtures.withId(postId, TestFixtures.createPost(testUser, "테스트 게시글", "내용"));

        given(postLikeQueryPort.existsByPostIdAndUserId(postId, userId)).willReturn(false);
        given(globalUserQueryPort.getReferenceById(userId)).willReturn(testUser);
        given(postQueryPort.findById(postId)).willReturn(post);

        // When
        postInteractionService.likePost(userId, postId);

        // Then
        verify(postLikeQueryPort).existsByPostIdAndUserId(postId, userId);
        verify(globalUserQueryPort).getReferenceById(userId);
        verify(postQueryPort).findById(postId);

        // ArgumentCaptor로 PostLike 객체 검증
        ArgumentCaptor<PostLike> postLikeCaptor = ArgumentCaptor.forClass(PostLike.class);
        verify(postLikeCommandPort).save(postLikeCaptor.capture());
        PostLike savedPostLike = postLikeCaptor.getValue();
        assertThat(savedPostLike.getUser()).isEqualTo(testUser);
        assertThat(savedPostLike.getPost()).isEqualTo(post);

        verify(postLikeCommandPort, never()).deleteByUserAndPost(any(), any());
    }

    @Test
    @DisplayName("게시글 추천 - 이미 추천한 경우 (추천 취소)")
    void shouldRemoveLike_WhenAlreadyLiked() {
        // Given
        Long userId = 1L;
        Long postId = 123L;
        Post post = TestFixtures.withId(postId, TestFixtures.createPost(testUser, "테스트 게시글", "내용"));

        given(postLikeQueryPort.existsByPostIdAndUserId(postId, userId)).willReturn(true);
        given(globalUserQueryPort.getReferenceById(userId)).willReturn(testUser);
        given(postQueryPort.findById(postId)).willReturn(post);

        // When
        postInteractionService.likePost(userId, postId);

        // Then
        verify(postLikeQueryPort).existsByPostIdAndUserId(postId, userId);
        verify(globalUserQueryPort).getReferenceById(userId);
        verify(postQueryPort).findById(postId);
        verify(postLikeCommandPort).deleteByUserAndPost(testUser, post);
        verify(postLikeCommandPort, never()).save(any());
    }

    @Test
    @DisplayName("게시글 추천 - 존재하지 않는 게시글인 경우")
    void shouldThrowException_WhenLikingNonExistentPost() {
        // Given
        Long userId = 1L;
        Long postId = 999L;

        given(postLikeQueryPort.existsByPostIdAndUserId(postId, userId)).willReturn(false);
        given(globalUserQueryPort.getReferenceById(userId)).willReturn(testUser);
        given(postQueryPort.findById(postId)).willThrow(new PostCustomException(PostErrorCode.POST_NOT_FOUND));

        // When & Then
        assertThatThrownBy(() -> postInteractionService.likePost(userId, postId))
                .isInstanceOf(PostCustomException.class)
                .hasFieldOrPropertyWithValue("postErrorCode", PostErrorCode.POST_NOT_FOUND);

        verify(postLikeQueryPort).existsByPostIdAndUserId(postId, userId);
        verify(globalUserQueryPort).getReferenceById(userId);
        verify(postQueryPort).findById(postId);
        verify(postLikeCommandPort, never()).save(any());
        verify(postLikeCommandPort, never()).deleteByUserAndPost(any(), any());
    }

    @Test
    @DisplayName("조회수 증가 - 정상적인 게시글")
    void shouldIncrementViewCount_WhenValidPost() {
        // Given
        Long postId = 123L;

        // When
        postInteractionService.incrementViewCount(postId);

        // Then
        verify(postCommandPort).incrementViewByPostId(postId);
    }

}