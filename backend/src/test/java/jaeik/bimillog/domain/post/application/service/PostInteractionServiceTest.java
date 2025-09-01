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

    // TODO: 아래 테스트들은 incrementViewCountWithHistory 메서드가 실제 구현에 존재하지 않아 임시 주석 처리
    // 중복 조회 검증 로직이 Controller로 이동했으므로 해당 로직에 맞는 새로운 테스트가 필요함
    
    /*
    @Test
    @DisplayName("이벤트 기반 조회수 증가 - 처음 조회하는 경우")
    void shouldIncrementViewCountWithHistory_WhenFirstTimeViewing() {
        // Given
        Long postId = 123L;
        String userIdentifier = "192.168.1.1";
        Map<String, String> viewHistory = new HashMap<>();
        viewHistory.put("viewed_posts", "");

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));

        // When
        Map<String, String> result = postInteractionService.incrementViewCountWithHistory(postId, userIdentifier, viewHistory);

        // Then
        verify(postQueryPort).findById(postId);
        verify(postCommandPort).incrementView(post);
        
        // 업데이트된 조회 이력 확인
        assertThat(result.get("viewed_posts")).isEqualTo("123");
    }

    @Test
    @DisplayName("이벤트 기반 조회수 증가 - 이미 조회한 경우")
    void shouldNotIncrementViewCountWithHistory_WhenAlreadyViewed() {
        // Given
        Long postId = 123L;
        String userIdentifier = "192.168.1.1";
        Map<String, String> viewHistory = new HashMap<>();
        viewHistory.put("viewed_posts", "123");

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));

        // When
        Map<String, String> result = postInteractionService.incrementViewCountWithHistory(postId, userIdentifier, viewHistory);

        // Then
        verify(postQueryPort).findById(postId);
        verify(postCommandPort, never()).incrementView(any());
        
        // 기존 이력 그대로 반환
        assertThat(result.get("viewed_posts")).isEqualTo("123");
    }

    @Test
    @DisplayName("이벤트 기반 조회수 증가 - 다른 게시글은 조회했지만 이 게시글은 처음")
    void shouldIncrementViewCountWithHistory_WhenViewingDifferentPost() {
        // Given
        Long postId = 123L;
        String userIdentifier = "192.168.1.1";
        Map<String, String> viewHistory = new HashMap<>();
        viewHistory.put("viewed_posts", "456,789");

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));

        // When
        Map<String, String> result = postInteractionService.incrementViewCountWithHistory(postId, userIdentifier, viewHistory);

        // Then
        verify(postQueryPort).findById(postId);
        verify(postCommandPort).incrementView(post);
        
        // 새로운 게시글 ID가 추가되어야 함
        assertThat(result.get("viewed_posts")).contains("123");
        assertThat(result.get("viewed_posts")).contains("456");
        assertThat(result.get("viewed_posts")).contains("789");
    }

    @Test
    @DisplayName("이벤트 기반 조회수 증가 - 부분 문자열 매칭 버그 방지")
    void shouldNotMatchPartialPostId_WhenCheckingViewHistory() {
        // Given - postId 12가 "123,124" 조회 이력에서 잘못 매칭되지 않아야 함
        Long postId = 12L;
        String userIdentifier = "192.168.1.1";
        Map<String, String> viewHistory = new HashMap<>();
        viewHistory.put("viewed_posts", "123,124,1234");

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));

        // When
        Map<String, String> result = postInteractionService.incrementViewCountWithHistory(postId, userIdentifier, viewHistory);

        // Then - 부분 매칭이 아닌 정확한 매칭이므로 조회수 증가되어야 함
        verify(postQueryPort).findById(postId);
        verify(postCommandPort).incrementView(post);
        
        // 새로운 ID가 추가되어야 함
        assertThat(result.get("viewed_posts")).contains("12");
    }

    @Test
    @DisplayName("이벤트 기반 조회수 증가 - 100개 제한 테스트")
    void shouldLimitViewHistoryTo100_WhenExceedsLimit() {
        // Given
        Long postId = 999L;
        String userIdentifier = "192.168.1.1";
        Map<String, String> viewHistory = new HashMap<>();
        
        // 이미 100개의 ID가 있는 상황
        StringBuilder existingViews = new StringBuilder();
        for (int i = 1; i <= 100; i++) {
            if (i > 1) existingViews.append(",");
            existingViews.append(i);
        }
        viewHistory.put("viewed_posts", existingViews.toString());

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));

        // When
        Map<String, String> result = postInteractionService.incrementViewCountWithHistory(postId, userIdentifier, viewHistory);

        // Then
        verify(postQueryPort).findById(postId);
        verify(postCommandPort).incrementView(post);
        
        // 결과는 여전히 100개 이하여야 함 (오래된 것 제거)
        String[] resultIds = result.get("viewed_posts").split(",");
        assertThat(resultIds.length).isLessThanOrEqualTo(100);
        assertThat(result.get("viewed_posts")).contains("999");
    }

    @Test
    @DisplayName("이벤트 기반 조회수 증가 - 존재하지 않는 게시글")
    void shouldThrowException_WhenIncrementViewCountWithHistoryForNonExistentPost() {
        // Given
        Long postId = 999L;
        String userIdentifier = "192.168.1.1";
        Map<String, String> viewHistory = new HashMap<>();
        viewHistory.put("viewed_posts", "");

        given(postQueryPort.findById(postId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> 
            postInteractionService.incrementViewCountWithHistory(postId, userIdentifier, viewHistory))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_NOT_FOUND);

        verify(postQueryPort).findById(postId);
        verify(postCommandPort, never()).incrementView(any());
    }

    @Test
    @DisplayName("이벤트 기반 조회수 증가 - null 값들 처리")
    void shouldHandleNullValues_WhenIncrementViewCountWithHistory() {
        // Given
        Long postId = 123L;
        String userIdentifier = null;
        Map<String, String> viewHistory = null;

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));

        // When
        Map<String, String> result = postInteractionService.incrementViewCountWithHistory(postId, userIdentifier, viewHistory);

        // Then
        verify(postQueryPort).findById(postId);
        verify(postCommandPort).incrementView(post);
        
        // null 값들을 적절히 처리해야 함
        assertThat(result).isNotNull();
        assertThat(result.get("viewed_posts")).isEqualTo("123");
    }

    @Test
    @DisplayName("이벤트 기반 조회수 증가 - 빈 조회 이력 처리")
    void shouldHandleEmptyViewHistory_WhenIncrementViewCountWithHistory() {
        // Given
        Long postId = 123L;
        String userIdentifier = "192.168.1.1";
        Map<String, String> viewHistory = new HashMap<>();
        // viewed_posts 키가 없는 상황

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));

        // When
        Map<String, String> result = postInteractionService.incrementViewCountWithHistory(postId, userIdentifier, viewHistory);

        // Then
        verify(postQueryPort).findById(postId);
        verify(postCommandPort).incrementView(post);
        
        // 빈 이력을 적절히 처리해야 함
        assertThat(result.get("viewed_posts")).isEqualTo("123");
    }
    */
}