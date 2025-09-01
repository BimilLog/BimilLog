package jaeik.bimillog.domain.post.application.service;

import jaeik.bimillog.domain.post.application.port.out.*;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostLike;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>PostInteractionService 테스트</h2>
 * <p>게시글 상호작용 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>추천/추천취소, 조회수 증가 등의 다양한 시나리오를 테스트합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostInteractionService 테스트")
class PostInteractionServiceTest {

    @Mock
    private PostCommandPort postCommandPort;

    @Mock
    private PostQueryPort postQueryPort;

    @Mock
    private PostLikeCommandPort postLikeCommandPort;

    @Mock
    private PostLikeQueryPort postLikeQueryPort;

    @Mock
    private LoadUserInfoPort loadUserInfoPort;

    @Mock
    private User user;

    @Mock
    private Post post;

    @InjectMocks
    private PostInteractionService postInteractionService;

    @Test
    @DisplayName("게시글 추천 - 처음 추천하는 경우")
    void shouldAddLike_WhenFirstTimeLiking() {
        // Given
        Long userId = 1L;
        Long postId = 123L;

        given(loadUserInfoPort.getReferenceById(userId)).willReturn(user);
        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(postLikeQueryPort.existsByUserAndPost(user, post)).willReturn(false);

        // When
        postInteractionService.likePost(userId, postId);

        // Then
        verify(loadUserInfoPort).getReferenceById(userId);
        verify(postQueryPort).findById(postId);
        verify(postLikeQueryPort).existsByUserAndPost(user, post);
        
        // ArgumentCaptor로 PostLike 객체 검증
        ArgumentCaptor<PostLike> postLikeCaptor = ArgumentCaptor.forClass(PostLike.class);
        verify(postLikeCommandPort).save(postLikeCaptor.capture());
        PostLike savedPostLike = postLikeCaptor.getValue();
        assertThat(savedPostLike.getUser()).isEqualTo(user);
        assertThat(savedPostLike.getPost()).isEqualTo(post);
        
        verify(postLikeCommandPort, never()).deleteByUserAndPost(any(), any());
    }

    @Test
    @DisplayName("게시글 추천 - 이미 추천한 경우 (추천 취소)")
    void shouldRemoveLike_WhenAlreadyLiked() {
        // Given
        Long userId = 1L;
        Long postId = 123L;

        given(loadUserInfoPort.getReferenceById(userId)).willReturn(user);
        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(postLikeQueryPort.existsByUserAndPost(user, post)).willReturn(true);

        // When
        postInteractionService.likePost(userId, postId);

        // Then
        verify(loadUserInfoPort).getReferenceById(userId);
        verify(postQueryPort).findById(postId);
        verify(postLikeQueryPort).existsByUserAndPost(user, post);
        verify(postLikeCommandPort).deleteByUserAndPost(user, post);
        verify(postLikeCommandPort, never()).save(any());
    }

    @Test
    @DisplayName("게시글 추천 - 존재하지 않는 게시글인 경우")
    void shouldThrowException_WhenLikingNonExistentPost() {
        // Given
        Long userId = 1L;
        Long postId = 999L;

        given(loadUserInfoPort.getReferenceById(userId)).willReturn(user);
        given(postQueryPort.findById(postId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> postInteractionService.likePost(userId, postId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_NOT_FOUND);

        verify(loadUserInfoPort).getReferenceById(userId);
        verify(postQueryPort).findById(postId);
        verify(postLikeQueryPort, never()).existsByUserAndPost(any(), any());
        verify(postLikeCommandPort, never()).save(any());
        verify(postLikeCommandPort, never()).deleteByUserAndPost(any(), any());
    }

    @Test
    @DisplayName("조회수 증가 - 정상적인 게시글")
    void shouldIncrementViewCount_WhenValidPost() {
        // Given
        Long postId = 123L;

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));

        // When
        postInteractionService.incrementViewCount(postId);

        // Then
        verify(postQueryPort).findById(postId);
        verify(postCommandPort).incrementView(post);
    }

    @Test
    @DisplayName("조회수 증가 - 존재하지 않는 게시글인 경우 예외 발생")
    void shouldThrowException_WhenIncrementViewCountForNonExistentPost() {
        // Given
        Long postId = 999L;

        given(postQueryPort.findById(postId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> postInteractionService.incrementViewCount(postId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_NOT_FOUND);

        verify(postQueryPort).findById(postId);
        verify(postCommandPort, never()).incrementView(any());
    }
}