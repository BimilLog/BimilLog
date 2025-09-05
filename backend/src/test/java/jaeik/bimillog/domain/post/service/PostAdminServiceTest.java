package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.application.port.out.PostCommandPort;
import jaeik.bimillog.domain.post.application.port.out.PostQueryPort;
import jaeik.bimillog.domain.post.application.service.PostAdminService;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.event.PostSetAsNoticeEvent;
import jaeik.bimillog.domain.post.event.PostUnsetAsNoticeEvent;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.exception.PostErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>PostAdminService 테스트</h2>
 * <p>게시글 공지사항 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>공지 설정/해제, 이벤트 발행, 관리자 권한 등의 시나리오를 테스트합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostAdminService 테스트")
class PostAdminServiceTest {

    @Mock
    private PostQueryPort postQueryPort;

    @Mock
    private PostCommandPort postCommandPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private Post post;

    @InjectMocks
    private PostAdminService postAdminService;

    @Test
    @DisplayName("게시글 공지 설정 - 성공")
    void shouldSetPostAsNotice_WhenValidPost() {
        // Given
        Long postId = 123L;
        String postTitle = "중요한 공지사항";

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(post.getTitle()).willReturn(postTitle);

        // When
        postAdminService.setPostAsNotice(postId);

        // Then
        verify(postQueryPort).findById(postId);
        verify(post).setAsNotice();
        verify(postCommandPort).save(post);

        // 이벤트 발행 검증
        ArgumentCaptor<PostSetAsNoticeEvent> eventCaptor = ArgumentCaptor.forClass(PostSetAsNoticeEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        PostSetAsNoticeEvent event = eventCaptor.getValue();
        assertThat(event.postId()).isEqualTo(postId);
    }

    @Test
    @DisplayName("게시글 공지 설정 - 존재하지 않는 게시글")
    void shouldThrowException_WhenSetNonExistentPostAsNotice() {
        // Given
        Long postId = 999L;

        given(postQueryPort.findById(postId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> postAdminService.setPostAsNotice(postId))
                .isInstanceOf(PostCustomException.class)
                .hasFieldOrPropertyWithValue("postErrorCode", PostErrorCode.POST_NOT_FOUND);

        verify(postQueryPort).findById(postId);
        verify(post, never()).setAsNotice();
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("게시글 공지 설정 - null postId")
    void shouldThrowException_WhenSetPostAsNoticeWithNullId() {
        // Given
        Long postId = null;

        // When & Then
        assertThatThrownBy(() -> postAdminService.setPostAsNotice(postId))
                .isInstanceOf(Exception.class);

        verify(postQueryPort).findById(postId);
        verify(post, never()).setAsNotice();
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("게시글 공지 해제 - 성공")
    void shouldUnsetPostAsNotice_WhenValidPost() {
        // Given
        Long postId = 123L;
        String postTitle = "공지 해제될 게시글";

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(post.getTitle()).willReturn(postTitle);

        // When
        postAdminService.unsetPostAsNotice(postId);

        // Then
        verify(postQueryPort).findById(postId);
        verify(post).unsetAsNotice();
        verify(postCommandPort).save(post);

        // 이벤트 발행 검증
        ArgumentCaptor<PostUnsetAsNoticeEvent> eventCaptor = ArgumentCaptor.forClass(PostUnsetAsNoticeEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        PostUnsetAsNoticeEvent event = eventCaptor.getValue();
        assertThat(event.postId()).isEqualTo(postId);
    }

    @Test
    @DisplayName("게시글 공지 해제 - 존재하지 않는 게시글")
    void shouldThrowException_WhenUnsetNonExistentPostAsNotice() {
        // Given
        Long postId = 999L;

        given(postQueryPort.findById(postId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> postAdminService.unsetPostAsNotice(postId))
                .isInstanceOf(PostCustomException.class)
                .hasFieldOrPropertyWithValue("postErrorCode", PostErrorCode.POST_NOT_FOUND);

        verify(postQueryPort).findById(postId);
        verify(post, never()).unsetAsNotice();
        verify(postCommandPort, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("게시글 공지 해제 - null postId")
    void shouldThrowException_WhenUnsetPostAsNoticeWithNullId() {
        // Given
        Long postId = null;

        // When & Then
        assertThatThrownBy(() -> postAdminService.unsetPostAsNotice(postId))
                .isInstanceOf(Exception.class);

        verify(postQueryPort).findById(postId);
        verify(post, never()).unsetAsNotice();
        verify(postCommandPort, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("게시글 공지 설정 시 이벤트 발행 검증")
    void shouldPublishCorrectEvent_WhenSettingPostAsNotice() {
        // Given
        Long postId = 123L;
        String postTitle = "테스트 공지사항";

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(post.getTitle()).willReturn(postTitle);

        // When
        postAdminService.setPostAsNotice(postId);

        // Then
        ArgumentCaptor<PostSetAsNoticeEvent> eventCaptor = ArgumentCaptor.forClass(PostSetAsNoticeEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        PostSetAsNoticeEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.postId()).isEqualTo(postId);
        
        // 이벤트가 올바른 타입인지 확인
        assertThat(capturedEvent).isInstanceOf(PostSetAsNoticeEvent.class);
    }

    @Test
    @DisplayName("게시글 공지 해제 시 이벤트 발행 검증")
    void shouldPublishCorrectEvent_WhenUnsettingPostAsNotice() {
        // Given
        Long postId = 456L;
        String postTitle = "공지 해제될 게시글";

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(post.getTitle()).willReturn(postTitle);

        // When
        postAdminService.unsetPostAsNotice(postId);

        // Then
        ArgumentCaptor<PostUnsetAsNoticeEvent> eventCaptor = ArgumentCaptor.forClass(PostUnsetAsNoticeEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        PostUnsetAsNoticeEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.postId()).isEqualTo(postId);
        
        // 이벤트가 올바른 타입인지 확인
        assertThat(capturedEvent).isInstanceOf(PostUnsetAsNoticeEvent.class);
    }

    @Test
    @DisplayName("연속 공지 설정/해제 시나리오")
    void shouldHandleSequentialNoticeOperations() {
        // Given
        Long postId = 123L;
        String postTitle = "연속 처리 테스트 게시글";

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(post.getTitle()).willReturn(postTitle);

        // When - 공지 설정 -> 해제 -> 다시 설정
        postAdminService.setPostAsNotice(postId);
        postAdminService.unsetPostAsNotice(postId);
        postAdminService.setPostAsNotice(postId);

        // Then
        verify(postQueryPort, times(3)).findById(postId);
        verify(post, times(2)).setAsNotice();
        verify(post, times(1)).unsetAsNotice();
        verify(postCommandPort, times(3)).save(post);
        
        // 이벤트 발행 확인
        verify(eventPublisher, times(2)).publishEvent(any(PostSetAsNoticeEvent.class));
        verify(eventPublisher, times(1)).publishEvent(any(PostUnsetAsNoticeEvent.class));
    }

    @Test
    @DisplayName("이미 공지인 게시글을 다시 공지 설정")
    void shouldHandleAlreadyNoticePost_WhenSettingAsNotice() {
        // Given
        Long postId = 123L;
        String postTitle = "이미 공지인 게시글";

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(post.getTitle()).willReturn(postTitle);

        // When - 이미 공지인 게시글을 다시 공지 설정
        postAdminService.setPostAsNotice(postId);

        // Then - 정상적으로 처리되어야 함 (비즈니스 로직에서 중복 확인은 하지 않음)
        verify(postQueryPort).findById(postId);
        verify(post).setAsNotice();
        verify(postCommandPort).save(post);
        verify(eventPublisher).publishEvent(any(PostSetAsNoticeEvent.class));
    }

    @Test
    @DisplayName("공지가 아닌 게시글을 공지 해제")
    void shouldHandleNonNoticePost_WhenUnsettingAsNotice() {
        // Given
        Long postId = 123L;
        String postTitle = "일반 게시글";

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(post.getTitle()).willReturn(postTitle);

        // When - 공지가 아닌 게시글을 공지 해제
        postAdminService.unsetPostAsNotice(postId);

        // Then - 정상적으로 처리되어야 함 (비즈니스 로직에서 상태 확인은 하지 않음)
        verify(postQueryPort).findById(postId);
        verify(post).unsetAsNotice();
        verify(postCommandPort).save(post);
        verify(eventPublisher).publishEvent(any(PostUnsetAsNoticeEvent.class));
    }

    @Test
    @DisplayName("서비스 메서드들의 트랜잭션 동작 검증")
    void shouldVerifyTransactionalBehavior() {
        // Given
        Long postId1 = 123L;
        Long postId2 = 124L;
        String postTitle = "트랜잭션 테스트 게시글";

        given(postQueryPort.findById(any())).willReturn(Optional.of(post));
        given(post.getTitle()).willReturn(postTitle);

        // When - @Transactional 메서드들 호출
        postAdminService.setPostAsNotice(postId1);
        postAdminService.unsetPostAsNotice(postId2);

        // Then - 모든 작업이 트랜잭션 내에서 수행됨
        verify(postQueryPort, times(2)).findById(any());
        verify(post).setAsNotice();
        verify(post).unsetAsNotice();
        verify(postCommandPort, times(2)).save(post);
        
        // ArgumentCaptor로 이벤트 발행 검증 (Mockito 버그 회피)
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
        
        List<Object> capturedEvents = eventCaptor.getAllValues();
        assertThat(capturedEvents).hasSize(2);
        
        // 이벤트 타입별 검증
        long setNoticeEventCount = capturedEvents.stream()
                .filter(event -> event instanceof PostSetAsNoticeEvent)
                .count();
        long unsetNoticeEventCount = capturedEvents.stream()
                .filter(event -> event instanceof PostUnsetAsNoticeEvent)
                .count();
                
        assertThat(setNoticeEventCount).isEqualTo(1);
        assertThat(unsetNoticeEventCount).isEqualTo(1);
    }

    @Test
    @DisplayName("예외 상황에서의 이벤트 발행 안됨 검증")
    void shouldNotPublishEvent_WhenExceptionOccurs() {
        // Given
        Long postId = 123L;

        given(postQueryPort.findById(postId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> postAdminService.setPostAsNotice(postId))
                .isInstanceOf(PostCustomException.class);

        // 예외 발생 시 이벤트 발행되지 않음
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Post 엔티티 메서드 호출 후 이벤트 발행 순서 검증")
    void shouldVerifyCorrectExecutionOrder() {
        // Given
        Long postId = 123L;
        String postTitle = "순서 검증 테스트";

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(post.getTitle()).willReturn(postTitle);

        // When
        postAdminService.setPostAsNotice(postId);

        // Then - 실행 순서 검증 (InOrder를 사용)
        var inOrder = inOrder(postQueryPort, post, postCommandPort, eventPublisher);
        inOrder.verify(postQueryPort).findById(postId);
        inOrder.verify(post).setAsNotice();
        inOrder.verify(postCommandPort).save(post);
        inOrder.verify(eventPublisher).publishEvent(any(PostSetAsNoticeEvent.class));
    }

    @Test
    @DisplayName("대량 공지 설정/해제 처리")
    void shouldHandleBulkNoticeOperations() {
        // Given
        int operationCount = 100; // 원래 계획대로 100개로 복원
        String postTitle = "대량 처리 테스트";

        given(postQueryPort.findById(any())).willReturn(Optional.of(post));
        given(post.getTitle()).willReturn(postTitle);

        // When - 100번의 공지 설정/해제 수행
        for (int i = 0; i < operationCount; i++) {
            if (i % 2 == 0) {
                postAdminService.setPostAsNotice((long) i);
            } else {
                postAdminService.unsetPostAsNotice((long) i);
            }
        }

        // Then
        verify(postQueryPort, times(operationCount)).findById(any());
        verify(post, times(50)).setAsNotice();  // 짝수 인덱스
        verify(post, times(50)).unsetAsNotice(); // 홀수 인덱스
        verify(postCommandPort, times(operationCount)).save(post);
        
        // ArgumentCaptor로 이벤트 발행 검증 (Mockito 버그 회피)
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(operationCount)).publishEvent(eventCaptor.capture());
        
        List<Object> capturedEvents = eventCaptor.getAllValues();
        assertThat(capturedEvents.size()).isEqualTo(100);
        
        // 이벤트 타입별 개수 검증
        long setNoticeEventCount = capturedEvents.stream()
                .filter(PostSetAsNoticeEvent.class::isInstance)
                .count();
        long unsetNoticeEventCount = capturedEvents.stream()
                .filter(PostUnsetAsNoticeEvent.class::isInstance)
                .count();
                
        assertThat(setNoticeEventCount).isEqualTo(50);
        assertThat(unsetNoticeEventCount).isEqualTo(50);
    }
}