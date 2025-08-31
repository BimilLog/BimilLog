package jaeik.bimillog.infrastructure.adapter.post.in.listener;

import jaeik.bimillog.domain.post.application.port.in.PostInteractionUseCase;
import jaeik.bimillog.domain.post.event.PostViewedEvent;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>게시글 조회 이벤트 리스너 테스트</h2>
 * <p>PostViewEventListener의 단위 테스트</p>
 * <p>게시글 조회 이벤트 처리 로직을 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("게시글 조회 이벤트 리스너 테스트")
class PostViewEventListenerTest {

    @Mock
    private PostInteractionUseCase postInteractionUseCase;

    @InjectMocks
    private PostViewEventListener postViewEventListener;

    @Test
    @DisplayName("게시글 조회 이벤트 처리 - 정상적인 조회수 증가")
    void handlePostViewedEvent_ShouldIncrementViewCount() {
        // Given
        Long postId = 123L;
        String userIdentifier = "192.168.1.1";
        Map<String, String> viewHistory = new HashMap<>();
        viewHistory.put("viewed_posts", "");
        PostViewedEvent event = new PostViewedEvent(this, postId, userIdentifier, viewHistory);

        // When
        postViewEventListener.handlePostViewedEvent(event);

        // Then
        verify(postInteractionUseCase).incrementViewCountWithHistory(
                eq(postId), eq(userIdentifier), eq(viewHistory));
    }

    @Test
    @DisplayName("게시글 조회 이벤트 처리 - 존재하지 않는 게시글")
    void handlePostViewedEvent_WhenPostNotFound_ShouldHandleException() {
        // Given
        Long postId = 999L;
        String userIdentifier = "192.168.1.1";
        Map<String, String> viewHistory = new HashMap<>();
        viewHistory.put("viewed_posts", "");
        PostViewedEvent event = new PostViewedEvent(this, postId, userIdentifier, viewHistory);
        
        doThrow(new CustomException(ErrorCode.POST_NOT_FOUND))
                .when(postInteractionUseCase)
                .incrementViewCountWithHistory(postId, userIdentifier, viewHistory);

        // When
        postViewEventListener.handlePostViewedEvent(event);

        // Then - 예외가 발생해도 메서드는 정상 완료되어야 함 (로그만 남기고 예외를 삼킴)
        verify(postInteractionUseCase).incrementViewCountWithHistory(
                eq(postId), eq(userIdentifier), eq(viewHistory));
    }

    @Test
    @DisplayName("게시글 조회 이벤트 처리 - null postId")
    void handlePostViewedEvent_WithNullPostId() {
        // Given
        String userIdentifier = "192.168.1.1";
        Map<String, String> viewHistory = new HashMap<>();
        viewHistory.put("viewed_posts", "");
        PostViewedEvent event = new PostViewedEvent(this, null, userIdentifier, viewHistory);

        // When
        postViewEventListener.handlePostViewedEvent(event);

        // Then - null postId도 전달되어야 함
        verify(postInteractionUseCase).incrementViewCountWithHistory(
                eq(null), eq(userIdentifier), eq(viewHistory));
    }

    @Test
    @DisplayName("게시글 조회 이벤트 처리 - null 데이터")
    void handlePostViewedEvent_WithNullData() {
        // Given
        Long postId = 123L;
        PostViewedEvent event = new PostViewedEvent(this, postId, null, null);

        // When
        postViewEventListener.handlePostViewedEvent(event);

        // Then - null 데이터도 전달되어야 함
        verify(postInteractionUseCase).incrementViewCountWithHistory(
                eq(postId), eq(null), eq(null));
    }

    @Test
    @DisplayName("게시글 조회 이벤트 처리 - 다양한 postId 값들")
    void handlePostViewedEvent_WithVariousPostIds() {
        // Given
        Long postId1 = 1L;
        Long postId2 = 999999L;
        Long postId3 = 0L;
        String userIdentifier = "192.168.1.1";
        Map<String, String> viewHistory = new HashMap<>();
        viewHistory.put("viewed_posts", "");
        
        PostViewedEvent event1 = new PostViewedEvent(this, postId1, userIdentifier, viewHistory);
        PostViewedEvent event2 = new PostViewedEvent(this, postId2, userIdentifier, viewHistory);
        PostViewedEvent event3 = new PostViewedEvent(this, postId3, userIdentifier, viewHistory);

        // When
        postViewEventListener.handlePostViewedEvent(event1);
        postViewEventListener.handlePostViewedEvent(event2);
        postViewEventListener.handlePostViewedEvent(event3);

        // Then
        verify(postInteractionUseCase).incrementViewCountWithHistory(
                eq(postId1), eq(userIdentifier), eq(viewHistory));
        verify(postInteractionUseCase).incrementViewCountWithHistory(
                eq(postId2), eq(userIdentifier), eq(viewHistory));
        verify(postInteractionUseCase).incrementViewCountWithHistory(
                eq(postId3), eq(userIdentifier), eq(viewHistory));
    }

    @Test
    @DisplayName("게시글 조회 이벤트 처리 - 동일 게시글 여러 번 조회")
    void handlePostViewedEvent_SamePostMultipleTimes() {
        // Given
        Long postId = 123L;
        String userIdentifier = "192.168.1.1";
        Map<String, String> viewHistory = new HashMap<>();
        viewHistory.put("viewed_posts", "");
        
        PostViewedEvent event1 = new PostViewedEvent(this, postId, userIdentifier, viewHistory);
        PostViewedEvent event2 = new PostViewedEvent(this, postId, userIdentifier, viewHistory);
        PostViewedEvent event3 = new PostViewedEvent(this, postId, userIdentifier, viewHistory);

        // When
        postViewEventListener.handlePostViewedEvent(event1);
        postViewEventListener.handlePostViewedEvent(event2);
        postViewEventListener.handlePostViewedEvent(event3);

        // Then - 매번 호출되어야 함 (중복 검사는 서비스 레이어에서)
        verify(postInteractionUseCase, times(3)).incrementViewCountWithHistory(
                eq(postId), eq(userIdentifier), eq(viewHistory));
    }

    @Test
    @DisplayName("게시글 조회 이벤트 처리 - UseCase에서 RuntimeException 발생")
    void handlePostViewedEvent_WhenUseCaseThrowsRuntimeException() {
        // Given
        Long postId = 123L;
        String userIdentifier = "192.168.1.1";
        Map<String, String> viewHistory = new HashMap<>();
        viewHistory.put("viewed_posts", "");
        PostViewedEvent event = new PostViewedEvent(this, postId, userIdentifier, viewHistory);
        
        RuntimeException runtimeException = new RuntimeException("데이터베이스 연결 오류");
        doThrow(runtimeException)
                .when(postInteractionUseCase)
                .incrementViewCountWithHistory(postId, userIdentifier, viewHistory);

        // When
        postViewEventListener.handlePostViewedEvent(event);

        // Then - 예외가 발생해도 메서드는 정상 완료되어야 함
        verify(postInteractionUseCase).incrementViewCountWithHistory(
                eq(postId), eq(userIdentifier), eq(viewHistory));
    }

    @Test
    @DisplayName("게시글 조회 이벤트 처리 - UseCase에서 IllegalArgumentException 발생")
    void handlePostViewedEvent_WhenUseCaseThrowsIllegalArgumentException() {
        // Given
        Long postId = -1L; // 잘못된 postId
        String userIdentifier = "192.168.1.1";
        Map<String, String> viewHistory = new HashMap<>();
        viewHistory.put("viewed_posts", "");
        PostViewedEvent event = new PostViewedEvent(this, postId, userIdentifier, viewHistory);
        
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("잘못된 게시글 ID");
        doThrow(illegalArgumentException)
                .when(postInteractionUseCase)
                .incrementViewCountWithHistory(postId, userIdentifier, viewHistory);

        // When
        postViewEventListener.handlePostViewedEvent(event);

        // Then - 예외가 발생해도 메서드는 정상 완료되어야 함
        verify(postInteractionUseCase).incrementViewCountWithHistory(
                eq(postId), eq(userIdentifier), eq(viewHistory));
    }

    @Test
    @DisplayName("게시글 조회 이벤트 처리 - 서로 다른 사용자 식별자들")
    void handlePostViewedEvent_WithDifferentUserIdentifiers() {
        // Given
        Long postId = 123L;
        String userIdentifier1 = "192.168.1.1";
        String userIdentifier2 = "192.168.1.2";
        Map<String, String> viewHistory1 = new HashMap<>();
        viewHistory1.put("viewed_posts", "100,101");
        Map<String, String> viewHistory2 = new HashMap<>();
        viewHistory2.put("viewed_posts", "200,201");
        
        PostViewedEvent event1 = new PostViewedEvent(this, postId, userIdentifier1, viewHistory1);
        PostViewedEvent event2 = new PostViewedEvent(this, postId, userIdentifier2, viewHistory2);

        // When
        postViewEventListener.handlePostViewedEvent(event1);
        postViewEventListener.handlePostViewedEvent(event2);

        // Then - 각각 다른 데이터가 전달되어야 함
        verify(postInteractionUseCase).incrementViewCountWithHistory(
                eq(postId), eq(userIdentifier1), eq(viewHistory1));
        verify(postInteractionUseCase).incrementViewCountWithHistory(
                eq(postId), eq(userIdentifier2), eq(viewHistory2));
    }

    @Test
    @DisplayName("이벤트 데이터 검증 - PostViewedEvent 속성들")
    void handlePostViewedEvent_EventDataValidation() {
        // Given
        Long postId = 456L;
        String userIdentifier = "10.0.0.1";
        Map<String, String> viewHistory = new HashMap<>();
        viewHistory.put("viewed_posts", "123,456");
        PostViewedEvent event = new PostViewedEvent(this, postId, userIdentifier, viewHistory);
        
        // 이벤트 데이터 검증
        assert event.getPostId().equals(postId);
        assert event.getUserIdentifier().equals(userIdentifier);
        assert event.getViewHistory().equals(viewHistory);

        // When
        postViewEventListener.handlePostViewedEvent(event);

        // Then
        verify(postInteractionUseCase).incrementViewCountWithHistory(
                eq(postId), eq(userIdentifier), eq(viewHistory));
    }

    @Test
    @DisplayName("대량 이벤트 처리 - 성능 테스트")
    void handlePostViewedEvent_HighVolumeProcessing() {
        // Given
        Long postId = 123L;
        String userIdentifier = "192.168.1.1";
        Map<String, String> viewHistory = new HashMap<>();
        viewHistory.put("viewed_posts", "");
        int eventCount = 1000;

        // When - 1000개의 이벤트 처리
        for (int i = 0; i < eventCount; i++) {
            PostViewedEvent event = new PostViewedEvent(this, postId, userIdentifier, viewHistory);
            postViewEventListener.handlePostViewedEvent(event);
        }

        // Then
        verify(postInteractionUseCase, times(eventCount)).incrementViewCountWithHistory(
                eq(postId), eq(userIdentifier), eq(viewHistory));
    }

    @Test
    @DisplayName("UseCase 호출 파라미터 정확성 검증")
    void handlePostViewedEvent_ShouldPassCorrectParameters() {
        // Given
        Long expectedPostId = 789L;
        String expectedUserIdentifier = "172.16.0.1";
        Map<String, String> expectedViewHistory = new HashMap<>();
        expectedViewHistory.put("viewed_posts", "100,200,300");
        PostViewedEvent event = new PostViewedEvent(this, expectedPostId, expectedUserIdentifier, expectedViewHistory);

        // When
        postViewEventListener.handlePostViewedEvent(event);

        // Then - 정확한 파라미터가 전달되어야 함
        verify(postInteractionUseCase, times(1)).incrementViewCountWithHistory(
                eq(expectedPostId), 
                same(expectedUserIdentifier),  // 동일한 객체 참조
                same(expectedViewHistory)  // 동일한 객체 참조
        );
        
        // 다른 메서드는 호출되지 않아야 함
        verify(postInteractionUseCase, never()).incrementViewCount(any());
        verify(postInteractionUseCase, never()).likePost(any(), any());
    }

    @Test
    @DisplayName("이벤트 소스 객체 검증")
    void handlePostViewedEvent_EventSourceValidation() {
        // Given
        Long postId = 123L;
        String userIdentifier = "192.168.1.1";
        Map<String, String> viewHistory = new HashMap<>();
        viewHistory.put("viewed_posts", "");
        Object eventSource = new Object();
        PostViewedEvent event = new PostViewedEvent(eventSource, postId, userIdentifier, viewHistory);
        
        // 이벤트 소스 검증
        assert event.getSource().equals(eventSource);

        // When
        postViewEventListener.handlePostViewedEvent(event);

        // Then - 이벤트 소스와 관계없이 정상 처리되어야 함
        verify(postInteractionUseCase).incrementViewCountWithHistory(
                eq(postId), eq(userIdentifier), eq(viewHistory));
    }
}