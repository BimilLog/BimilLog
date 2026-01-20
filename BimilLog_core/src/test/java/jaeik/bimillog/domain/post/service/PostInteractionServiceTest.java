package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostLike;
import jaeik.bimillog.domain.post.repository.PostLikeRepository;
import jaeik.bimillog.domain.post.repository.PostRepository;
import jaeik.bimillog.domain.post.adapter.PostToMemberAdapter;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.builder.PostTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

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
    private PostLikeRepository postLikeRepository;

    @Mock
    private PostToMemberAdapter postToMemberAdapter;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostInteractionService postInteractionService;

    @ParameterizedTest(name = "이미 추천 여부: {0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("게시글 추천 토글 - 추가/취소")
    void shouldToggleLike_WhenLikePost(boolean alreadyLiked) {
        // Given
        Long memberId = 1L;
        Long postId = 123L;
        Post post = PostTestDataBuilder.withId(postId, PostTestDataBuilder.createPost(getTestMember(), "테스트 게시글", "내용"));

        given(postLikeRepository.existsByPostIdAndMemberId(postId, memberId)).willReturn(alreadyLiked);
        given(postToMemberAdapter.getReferenceById(memberId)).willReturn(getTestMember());
        given(postRepository.findById(postId)).willReturn(Optional.of(post));

        // When
        postInteractionService.likePost(memberId, postId);

        // Then
        verify(postLikeRepository).existsByPostIdAndMemberId(postId, memberId);
        verify(postToMemberAdapter).getReferenceById(memberId);
        verify(postRepository).findById(postId);

        if (alreadyLiked) {
            verify(postLikeRepository).deleteByMemberAndPost(getTestMember(), post);
            verify(postLikeRepository, never()).save(any());
        } else {
            ArgumentCaptor<PostLike> postLikeCaptor = ArgumentCaptor.forClass(PostLike.class);
            verify(postLikeRepository).save(postLikeCaptor.capture());
            PostLike savedPostLike = postLikeCaptor.getValue();
            assertThat(savedPostLike.getMember()).isEqualTo(getTestMember());
            assertThat(savedPostLike.getPost()).isEqualTo(post);

            verify(postLikeRepository, never()).deleteByMemberAndPost(any(), any());
        }
    }

    @Test
    @DisplayName("게시글 추천 - 존재하지 않는 게시글인 경우")
    void shouldThrowException_WhenLikingNonExistentPost() {
        // Given
        Long memberId = 1L;
        Long postId = 999L;

        given(postLikeRepository.existsByPostIdAndMemberId(postId, memberId)).willReturn(false);
        given(postToMemberAdapter.getReferenceById(memberId)).willReturn(getTestMember());
        given(postRepository.findById(postId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> postInteractionService.likePost(memberId, postId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_NOT_FOUND);

        verify(postLikeRepository).existsByPostIdAndMemberId(postId, memberId);
        verify(postToMemberAdapter).getReferenceById(memberId);
        verify(postRepository).findById(postId);
        verify(postLikeRepository, never()).save(any());
        verify(postLikeRepository, never()).deleteByMemberAndPost(any(), any());
    }

    @Test
    @DisplayName("조회수 증가 - 정상적인 게시글")
    void shouldIncrementViewCount_WhenValidPost() {
        // Given
        Long postId = 123L;

        // When
        postInteractionService.incrementViewCount(postId);

        // Then
        verify(postRepository).incrementViewsByPostId(postId);
    }

}