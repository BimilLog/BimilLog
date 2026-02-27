package jaeik.bimillog.unit.domain.post;

import jaeik.bimillog.domain.global.event.CheckBlacklistEvent;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.post.async.CacheRealtimeSync;
import jaeik.bimillog.domain.post.async.CacheUpdateCountSync;
import jaeik.bimillog.domain.post.entity.jpa.Post;
import jaeik.bimillog.domain.post.entity.jpa.PostLike;
import jaeik.bimillog.domain.post.event.PostLikeEvent;
import jaeik.bimillog.domain.post.repository.PostLikeRepository;
import jaeik.bimillog.domain.post.repository.PostRepository;
import jaeik.bimillog.domain.post.adapter.PostToMemberAdapter;
import jaeik.bimillog.domain.post.service.PostInteractionService;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.builder.PostTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * <h2>PostInteractionService 테스트</h2>
 * <p>게시글 상호작용 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>추천/추천취소 등의 다양한 시나리오를 테스트합니다.</p>
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
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostToMemberAdapter postToMemberAdapter;

    @Mock
    private CacheRealtimeSync cacheRealtimeSync;

    @Mock
    private CacheUpdateCountSync cacheUpdateCountSync;

    @InjectMocks
    private PostInteractionService postInteractionService;

    @ParameterizedTest(name = "이미 추천 여부: {0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("게시글 추천 토글 - 추가/취소")
    void shouldToggleLike_WhenLikePost(boolean alreadyLiked) {
        // Given
        Long memberId = 1L;
        Long postId = 123L;
        Member member = getTestMember();
        Post post = PostTestDataBuilder.withId(postId, PostTestDataBuilder.createPost(member, "테스트 게시글", "내용"));

        given(postLikeRepository.existsByPostIdAndMemberId(postId, memberId)).willReturn(alreadyLiked);
        given(postToMemberAdapter.getMember(memberId)).willReturn(member);
        given(postRepository.findById(postId)).willReturn(Optional.of(post));

        // When
        postInteractionService.likePost(memberId, postId);

        // Then
        verify(postLikeRepository).existsByPostIdAndMemberId(postId, memberId);
        verify(postToMemberAdapter).getMember(memberId);
        verify(postRepository).findById(postId);
        // 게시글 작성자가 있으면 블랙리스트 확인 이벤트 발행
        verify(eventPublisher).publishEvent(any(CheckBlacklistEvent.class));

        if (alreadyLiked) {
            verify(postLikeRepository).deleteByMemberAndPost(member, post);
            verify(postRepository).decrementLikeCount(postId);
            verify(cacheUpdateCountSync).incrementLikeCounter(postId, -1);
            verify(postLikeRepository, never()).save(any(PostLike.class));
        } else {
            verify(postLikeRepository).save(any(PostLike.class));
            verify(postRepository).incrementLikeCount(postId);
            verify(cacheUpdateCountSync).incrementLikeCounter(postId, 1);
            verify(postLikeRepository, never()).deleteByMemberAndPost(any(), any());
            // 자기 자신 게시글 추천이므로 PostLikeEvent 발행 안 함 (memberId == postAuthorId)
            verify(eventPublisher, never()).publishEvent(any(PostLikeEvent.class));
        }
    }

    @Test
    @DisplayName("게시글 추천 - 다른 사람 게시글 추천 시 PostLikeEvent 발행")
    void shouldPublishPostLikeEvent_WhenLikingOtherMembersPost() {
        // Given
        Long memberId = 1L;
        Long postId = 123L;
        Member liker = getTestMember();   // id=1L
        Member author = getOtherMember(); // id=2L (다른 사람)
        Post post = PostTestDataBuilder.withId(postId, PostTestDataBuilder.createPost(author, "다른 사람 게시글", "내용"));

        given(postLikeRepository.existsByPostIdAndMemberId(postId, memberId)).willReturn(false);
        given(postToMemberAdapter.getMember(memberId)).willReturn(liker);
        given(postRepository.findById(postId)).willReturn(Optional.of(post));

        // When
        postInteractionService.likePost(memberId, postId);

        // Then: 블랙리스트 확인 + 추천 알림 이벤트 모두 발행
        verify(eventPublisher).publishEvent(any(CheckBlacklistEvent.class));
        verify(eventPublisher).publishEvent(any(PostLikeEvent.class));
    }

    @Test
    @DisplayName("게시글 추천 - 존재하지 않는 게시글인 경우")
    void shouldThrowException_WhenLikingNonExistentPost() {
        // Given
        Long memberId = 1L;
        Long postId = 999L;
        Member member = getTestMember();

        given(postLikeRepository.existsByPostIdAndMemberId(postId, memberId)).willReturn(false);
        given(postToMemberAdapter.getMember(memberId)).willReturn(member);
        given(postRepository.findById(postId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> postInteractionService.likePost(memberId, postId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_NOT_FOUND);

        verify(postLikeRepository).existsByPostIdAndMemberId(postId, memberId);
        verify(postRepository).findById(postId);
        verify(postLikeRepository, never()).save(any(PostLike.class));
        verify(postLikeRepository, never()).deleteByMemberAndPost(any(), any());
    }

}
