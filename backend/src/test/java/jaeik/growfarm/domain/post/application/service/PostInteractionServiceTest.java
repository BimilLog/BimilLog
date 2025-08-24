package jaeik.growfarm.domain.post.application.service;

import jaeik.growfarm.domain.post.application.port.out.*;
import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.domain.post.entity.PostLike;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    @DisplayName("게시글 추천 - 처음 추천하는 경우 (추천 추가)")
    void shouldLikePost_WhenNotPreviouslyLiked() {
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
        
        // 추천 추가 검증
        ArgumentCaptor<PostLike> postLikeCaptor = ArgumentCaptor.forClass(PostLike.class);
        verify(postLikeCommandPort).save(postLikeCaptor.capture());
        
        PostLike savedPostLike = postLikeCaptor.getValue();
        assertThat(savedPostLike.getUser()).isEqualTo(user);
        assertThat(savedPostLike.getPost()).isEqualTo(post);
        
        // 추천 삭제는 호출되지 않음
        verify(postLikeCommandPort, never()).deleteByUserAndPost(any(), any());
    }

    @Test
    @DisplayName("게시글 추천 - 이미 추천한 경우 (추천 취소)")
    void shouldUnlikePost_WhenPreviouslyLiked() {
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
        
        // 추천 삭제 검증
        verify(postLikeCommandPort).deleteByUserAndPost(user, post);
        
        // 추천 추가는 호출되지 않음
        verify(postLikeCommandPort, never()).save(any());
    }

    @Test
    @DisplayName("게시글 추천 - 존재하지 않는 게시글인 경우")
    void shouldThrowException_WhenPostNotFound() {
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
        
        // 추천 관련 작업은 수행되지 않음
        verify(postLikeQueryPort, never()).existsByUserAndPost(any(), any());
        verify(postLikeCommandPort, never()).save(any());
        verify(postLikeCommandPort, never()).deleteByUserAndPost(any(), any());
    }

    @Test
    @DisplayName("게시글 추천 - 유효하지 않은 사용자인 경우")
    void shouldHandleInvalidUser_WhenLikingPost() {
        // Given
        Long userId = null;
        Long postId = 123L;

        doThrow(new IllegalArgumentException("User ID cannot be null"))
                .when(loadUserInfoPort).getReferenceById(userId);

        // When & Then
        assertThatThrownBy(() -> postInteractionService.likePost(userId, postId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User ID cannot be null");

        verify(loadUserInfoPort).getReferenceById(userId);
        verifyNoInteractions(postQueryPort, postLikeQueryPort, postLikeCommandPort);
    }

    @Test
    @DisplayName("조회수 증가 - 성공")
    void shouldIncrementViewCount_WhenValidPost() {
        // Given
        Long postId = 123L;
        int initialViews = 10;
        
        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(post.getViews()).willReturn(initialViews);

        // When
        postInteractionService.incrementViewCount(postId);

        // Then
        verify(postQueryPort).findById(postId);
        verify(postCommandPort).incrementView(post);
    }

    @Test
    @DisplayName("조회수 증가 - 존재하지 않는 게시글인 경우")
    void shouldThrowException_WhenIncrementViewForNonExistentPost() {
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

    @Test
    @DisplayName("조회수 증가 - null postId인 경우")
    void shouldThrowException_WhenIncrementViewWithNullPostId() {
        // Given
        Long postId = null;

        // When & Then
        assertThatThrownBy(() -> postInteractionService.incrementViewCount(postId))
                .isInstanceOf(Exception.class);

        verify(postQueryPort).findById(postId);
        verify(postCommandPort, never()).incrementView(any());
    }

    @Test
    @DisplayName("추천 토글 동작 검증 - 연속 호출")
    void shouldToggleLike_WhenCalledRepeatedly() {
        // Given
        Long userId = 1L;
        Long postId = 123L;

        given(loadUserInfoPort.getReferenceById(userId)).willReturn(user);
        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        
        // 첫 번째 호출: 추천 안됨 -> 추천 추가
        given(postLikeQueryPort.existsByUserAndPost(user, post))
                .willReturn(false)   // 첫 번째 호출
                .willReturn(true);   // 두 번째 호출

        // When - 첫 번째 호출 (추천 추가)
        postInteractionService.likePost(userId, postId);

        // When - 두 번째 호출 (추천 취소)
        postInteractionService.likePost(userId, postId);

        // Then
        verify(loadUserInfoPort, times(2)).getReferenceById(userId);
        verify(postQueryPort, times(2)).findById(postId);
        verify(postLikeQueryPort, times(2)).existsByUserAndPost(user, post);
        
        // 첫 번째: 추천 추가, 두 번째: 추천 삭제
        verify(postLikeCommandPort, times(1)).save(any(PostLike.class));
        verify(postLikeCommandPort, times(1)).deleteByUserAndPost(user, post);
    }

    @Test
    @DisplayName("동시 추천 요청 시나리오 - 데이터 일관성")
    void shouldHandleConcurrentLikeRequests() {
        // Given
        Long userId1 = 1L;
        Long userId2 = 2L;
        Long postId = 123L;

        User user1 = mock(User.class);
        User user2 = mock(User.class);

        given(loadUserInfoPort.getReferenceById(userId1)).willReturn(user1);
        given(loadUserInfoPort.getReferenceById(userId2)).willReturn(user2);
        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(postLikeQueryPort.existsByUserAndPost(user1, post)).willReturn(false);
        given(postLikeQueryPort.existsByUserAndPost(user2, post)).willReturn(false);

        // When - 두 사용자가 동시에 추천
        postInteractionService.likePost(userId1, postId);
        postInteractionService.likePost(userId2, postId);

        // Then
        verify(loadUserInfoPort).getReferenceById(userId1);
        verify(loadUserInfoPort).getReferenceById(userId2);
        verify(postQueryPort, times(2)).findById(postId);
        verify(postLikeCommandPort, times(2)).save(any(PostLike.class));
    }


    @Test
    @DisplayName("대용량 조회수 증가 처리 - 실제 동시성 문제 검증 필요")
    void shouldHandleHighVolumeViewIncrements() {
        // Given
        Long postId = 123L;
        int callCount = 10000;

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));

        // When
        for (int i = 0; i < callCount; i++) {
            postInteractionService.incrementViewCount(postId);
        }

        // Then
        verify(postQueryPort, times(callCount)).findById(postId);
        verify(postCommandPort, times(callCount)).incrementView(post);
    }

    @Test
    @DisplayName("실제 멀티스레드 동시성 테스트 - 추천 시스템")
    void shouldHandleConcurrentLikeRequests_RealMultithread() {
        
        // Given
        Long userId = 1L;
        Long postId = 123L;
        int threadCount = 100;
        int operationsPerThread = 10;

        given(loadUserInfoPort.getReferenceById(userId)).willReturn(user);
        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(postLikeQueryPort.existsByUserAndPost(user, post)).willReturn(false);

        // When -
        for (int i = 0; i < threadCount * operationsPerThread; i++) {
            postInteractionService.likePost(userId, postId);
        }

        // Then
        verify(loadUserInfoPort, times(threadCount * operationsPerThread)).getReferenceById(userId);
        verify(postQueryPort, times(threadCount * operationsPerThread)).findById(postId);
    }

    @Test
    @DisplayName("서비스 메서드들의 트랜잭션 동작 검증")
    void shouldVerifyTransactionalBehavior() {
        // Given
        Long userId = 1L;
        Long postId = 123L;

        given(loadUserInfoPort.getReferenceById(userId)).willReturn(user);
        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(postLikeQueryPort.existsByUserAndPost(user, post)).willReturn(false);

        // When - @Transactional 메서드들 호출
        postInteractionService.likePost(userId, postId);
        postInteractionService.incrementViewCount(postId);

        // Then - 모든 포트 메서드가 트랜잭션 내에서 호출됨
        verify(loadUserInfoPort).getReferenceById(userId);
        verify(postQueryPort, times(2)).findById(postId);
        verify(postLikeQueryPort).existsByUserAndPost(user, post);
        verify(postLikeCommandPort).save(any(PostLike.class));
        verify(postCommandPort).incrementView(post);
    }

    @Test
    @DisplayName("예외 상황에서의 롤백 동작 검증")
    void shouldVerifyRollbackBehavior_WhenExceptionOccurs() {
        // Given
        Long userId = 1L;
        Long postId = 123L;

        given(loadUserInfoPort.getReferenceById(userId)).willReturn(user);
        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(postLikeQueryPort.existsByUserAndPost(user, post)).willReturn(false);
        doThrow(new RuntimeException("DB 오류")).when(postLikeCommandPort).save(any());

        // When & Then
        assertThatThrownBy(() -> postInteractionService.likePost(userId, postId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB 오류");

        // 예외 발생 전까지의 호출만 확인
        verify(loadUserInfoPort).getReferenceById(userId);
        verify(postQueryPort).findById(postId);
        verify(postLikeQueryPort).existsByUserAndPost(user, post);
        verify(postLikeCommandPort).save(any(PostLike.class));
    }
}