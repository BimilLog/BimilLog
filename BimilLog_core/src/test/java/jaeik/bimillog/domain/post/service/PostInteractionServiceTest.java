package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.global.out.GlobalMemberQueryAdapter;
import jaeik.bimillog.domain.global.out.GlobalPostQueryAdapter;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostLike;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.domain.post.out.PostLikeQueryRepository;
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
    private GlobalPostQueryAdapter globalPostQueryAdapter;

    @Mock
    private PostLikeQueryRepository postLikeQueryRepository;

    @Mock
    private GlobalMemberQueryAdapter globalMemberQueryAdapter;

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

        given(postLikeQueryRepository.existsByPostIdAndUserId(postId, memberId)).willReturn(false);
        given(globalMemberQueryAdapter.getReferenceById(memberId)).willReturn(getTestMember());
        given(globalPostQueryAdapter.findById(postId)).willReturn(post);

        // When
        postInteractionService.likePost(memberId, postId);

        // Then
        verify(postLikeQueryRepository).existsByPostIdAndUserId(postId, memberId);
        verify(globalMemberQueryAdapter).getReferenceById(memberId);
        verify(globalPostQueryAdapter).findById(postId);

        // ArgumentCaptor로 PostLike 객체 검증
        ArgumentCaptor<PostLike> postLikeCaptor = ArgumentCaptor.forClass(PostLike.class);
        verify(postLikeCommandAdapter).savePostLike(postLikeCaptor.capture());
        PostLike savedPostLike = postLikeCaptor.getValue();
        assertThat(savedPostLike.getMember()).isEqualTo(getTestMember());
        assertThat(savedPostLike.getPost()).isEqualTo(post);

        verify(postLikeCommandAdapter, never()).deletePostLike(any(), any());
    }

    @Test
    @DisplayName("게시글 추천 - 이미 추천한 경우 (추천 취소)")
    void shouldRemoveLike_WhenAlreadyLiked() {
        // Given
        Long memberId = 1L;
        Long postId = 123L;
        Post post = PostTestDataBuilder.withId(postId, PostTestDataBuilder.createPost(getTestMember(), "테스트 게시글", "내용"));

        given(postLikeQueryRepository.existsByPostIdAndUserId(postId, memberId)).willReturn(true);
        given(globalMemberQueryAdapter.getReferenceById(memberId)).willReturn(getTestMember());
        given(globalPostQueryAdapter.findById(postId)).willReturn(post);

        // When
        postInteractionService.likePost(memberId, postId);

        // Then
        verify(postLikeQueryRepository).existsByPostIdAndUserId(postId, memberId);
        verify(globalMemberQueryAdapter).getReferenceById(memberId);
        verify(globalPostQueryAdapter).findById(postId);
        verify(postLikeCommandAdapter).deletePostLike(getTestMember(), post);
        verify(postLikeCommandAdapter, never()).savePostLike(any());
    }

    @Test
    @DisplayName("게시글 추천 - 존재하지 않는 게시글인 경우")
    void shouldThrowException_WhenLikingNonExistentPost() {
        // Given
        Long memberId = 1L;
        Long postId = 999L;

        given(postLikeQueryRepository.existsByPostIdAndUserId(postId, memberId)).willReturn(false);
        given(globalMemberQueryAdapter.getReferenceById(memberId)).willReturn(getTestMember());
        given(globalPostQueryAdapter.findById(postId)).willThrow(new CustomException(ErrorCode.POST_NOT_FOUND));

        // When & Then
        assertThatThrownBy(() -> postInteractionService.likePost(memberId, postId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_NOT_FOUND);

        verify(postLikeQueryRepository).existsByPostIdAndUserId(postId, memberId);
        verify(globalMemberQueryAdapter).getReferenceById(memberId);
        verify(globalPostQueryAdapter).findById(postId);
        verify(postLikeCommandAdapter, never()).savePostLike(any());
        verify(postLikeCommandAdapter, never()).deletePostLike(any(), any());
    }

    @Test
    @DisplayName("조회수 증가 - 정상적인 게시글")
    void shouldIncrementViewCount_WhenValidPost() {
        // Given
        Long postId = 123L;

        // When
        postInteractionService.incrementViewCount(postId);

        // Then
        verify(postCommandAdapter).incrementViewByPostId(postId);
    }

}