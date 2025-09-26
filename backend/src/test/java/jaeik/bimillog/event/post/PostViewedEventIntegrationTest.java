package jaeik.bimillog.event.post;

import jaeik.bimillog.domain.post.application.port.in.PostInteractionUseCase;
import jaeik.bimillog.domain.post.event.PostViewedEvent;
import jaeik.bimillog.testutil.BaseEventIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * <h2>게시글 조회 이벤트 워크플로우 통합 테스트</h2>
 * <p>게시글 조회 시 발생하는 모든 후속 처리를 검증하는 통합 테스트</p>
 * <p>비동기 이벤트 처리와 실제 스프링 컨텍스트를 사용하여 전체 워크플로우를 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("게시글 조회 이벤트 워크플로우 통합 테스트")
@Tag("integration")
public class PostViewedEventIntegrationTest extends BaseEventIntegrationTest {

    @MockitoBean
    private PostInteractionUseCase postInteractionUseCase;

    @Test
    @DisplayName("게시글 조회 이벤트 워크플로우 - 조회수 증가까지 완료")
    void postViewedEventWorkflow_ShouldCompleteViewCountIncrement() {
        // Given
        var event = new PostViewedEvent(1L);

        // When & Then
        publishAndVerify(event, () -> {
            verify(postInteractionUseCase).incrementViewCount(eq(1L));
            verifyNoMoreInteractions(postInteractionUseCase);
        });
    }

    @Test
    @DisplayName("여러 다른 게시글 조회 이벤트 동시 처리")
    void multipleDifferentPostViewedEvents_ShouldProcessIndependently() {
        // Given
        var events = java.util.List.of(
                new PostViewedEvent(1L),
                new PostViewedEvent(2L),
                new PostViewedEvent(3L)
        );

        // When & Then
        publishEvents(events);
        verifyAsync(() -> {
            verify(postInteractionUseCase).incrementViewCount(eq(1L));
            verify(postInteractionUseCase).incrementViewCount(eq(2L));
            verify(postInteractionUseCase).incrementViewCount(eq(3L));
            verifyNoMoreInteractions(postInteractionUseCase);
        });
    }

    @Test
    @DisplayName("동일 게시글의 여러 조회 이벤트 처리")
    void multipleViewEventsForSamePost_ShouldProcessAll() {
        // Given
        var events = new java.util.ArrayList<PostViewedEvent>();
        for (int i = 0; i < 3; i++) {
            events.add(new PostViewedEvent(1L));
        }

        // When & Then
        publishEvents(events);
        verifyAsync(() -> {
            verify(postInteractionUseCase, times(3)).incrementViewCount(eq(1L));
            verifyNoMoreInteractions(postInteractionUseCase);
        });
    }

    @Test
    @DisplayName("조회수 증가 실패 시에도 시스템 정상 작동")
    void postViewedEventWithException_ShouldContinueWorking() {
        // Given
        var event = new PostViewedEvent(1L);

        // 조회수 증가 실패 시뮬레이션 - 리스너가 예외를 catch하여 로그 처리
        doThrow(new RuntimeException("조회수 증가 실패"))
                .when(postInteractionUseCase).incrementViewCount(1L);

        // When & Then - 예외가 발생해도 시스템은 정상 작동
        publishAndVerify(event, () -> {
            verify(postInteractionUseCase).incrementViewCount(eq(1L));
            verifyNoMoreInteractions(postInteractionUseCase);
        });
    }

}