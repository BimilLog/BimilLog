package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.global.application.port.out.GlobalMemberQueryPort;
import jaeik.bimillog.domain.global.application.port.out.GlobalPostQueryPort;
import jaeik.bimillog.domain.post.application.port.out.PostCommandPort;
import jaeik.bimillog.domain.post.application.port.out.PostLikeCommandPort;
import jaeik.bimillog.domain.post.application.port.out.PostLikeQueryPort;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostLike;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.exception.PostErrorCode;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.builder.PostTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;

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
@Tag("unit")
class PostInteractionServiceTest extends BaseUnitTest {

    @Mock
    private PostCommandPort postCommandPort;

    @Mock
    private GlobalPostQueryPort globalPostQueryPort;

    @Mock
    private PostLikeCommandPort postLikeCommandPort;

    @Mock
    private PostLikeQueryPort postLikeQueryPort;

    @Mock
    private GlobalMemberQueryPort globalMemberQueryPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PostInteractionService postInteractionService;

    @Test
    @DisplayName("게시글 추천 - 처음 추천하는 경우")
    void shouldAddLike_WhenFirstTimeLiking() {
        // Given
        Long memberId = 1L;
        Long postId = 123L;
        Post post = PostTestDataBuilder.withId(postId, PostTestDataBuilder.createPost(getTestMember(), "테스트 게시글", "내용"));

        given(postLikeQueryPort.existsByPostIdAndUserId(postId, memberId)).willReturn(false);
        given(globalMemberQueryPort.getReferenceById(memberId)).willReturn(getTestMember());
        given(globalPostQueryPort.findById(postId)).willReturn(post);

        // When
        postInteractionService.likePost(memberId, postId);

        // Then
        verify(postLikeQueryPort).existsByPostIdAndUserId(postId, memberId);
        verify(globalMemberQueryPort).getReferenceById(memberId);
        verify(globalPostQueryPort).findById(postId);

        // ArgumentCaptor로 PostLike 객체 검증
        ArgumentCaptor<PostLike> postLikeCaptor = ArgumentCaptor.forClass(PostLike.class);
        verify(postLikeCommandPort).savePostLike(postLikeCaptor.capture());
        PostLike savedPostLike = postLikeCaptor.getValue();
        assertThat(savedPostLike.getMember()).isEqualTo(getTestMember());
        assertThat(savedPostLike.getPost()).isEqualTo(post);

        verify(postLikeCommandPort, never()).deletePostLike(any(), any());
    }

    @Test
    @DisplayName("게시글 추천 - 이미 추천한 경우 (추천 취소)")
    void shouldRemoveLike_WhenAlreadyLiked() {
        // Given
        Long memberId = 1L;
        Long postId = 123L;
        Post post = PostTestDataBuilder.withId(postId, PostTestDataBuilder.createPost(getTestMember(), "테스트 게시글", "내용"));

        given(postLikeQueryPort.existsByPostIdAndUserId(postId, memberId)).willReturn(true);
        given(globalMemberQueryPort.getReferenceById(memberId)).willReturn(getTestMember());
        given(globalPostQueryPort.findById(postId)).willReturn(post);

        // When
        postInteractionService.likePost(memberId, postId);

        // Then
        verify(postLikeQueryPort).existsByPostIdAndUserId(postId, memberId);
        verify(globalMemberQueryPort).getReferenceById(memberId);
        verify(globalPostQueryPort).findById(postId);
        verify(postLikeCommandPort).deletePostLike(getTestMember(), post);
        verify(postLikeCommandPort, never()).savePostLike(any());
    }

    @Test
    @DisplayName("게시글 추천 - 존재하지 않는 게시글인 경우")
    void shouldThrowException_WhenLikingNonExistentPost() {
        // Given
        Long memberId = 1L;
        Long postId = 999L;

        given(postLikeQueryPort.existsByPostIdAndUserId(postId, memberId)).willReturn(false);
        given(globalMemberQueryPort.getReferenceById(memberId)).willReturn(getTestMember());
        given(globalPostQueryPort.findById(postId)).willThrow(new PostCustomException(PostErrorCode.POST_NOT_FOUND));

        // When & Then
        assertThatThrownBy(() -> postInteractionService.likePost(memberId, postId))
                .isInstanceOf(PostCustomException.class)
                .hasFieldOrPropertyWithValue("postErrorCode", PostErrorCode.POST_NOT_FOUND);

        verify(postLikeQueryPort).existsByPostIdAndUserId(postId, memberId);
        verify(globalMemberQueryPort).getReferenceById(memberId);
        verify(globalPostQueryPort).findById(postId);
        verify(postLikeCommandPort, never()).savePostLike(any());
        verify(postLikeCommandPort, never()).deletePostLike(any(), any());
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