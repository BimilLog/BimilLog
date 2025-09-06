package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.application.port.out.PostCommandPort;
import jaeik.bimillog.domain.post.application.port.out.PostQueryPort;
import jaeik.bimillog.domain.post.application.service.PostAdminService;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.exception.PostErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>PostAdminService 테스트</h2>
 * <p>게시글 공지사항 서비스의 핵심 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>공지 설정/해제 DB 로직에만 집중하며, 캐시는 Controller에서 분리됨</p>
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
    private Post post;

    @InjectMocks
    private PostAdminService postAdminService;

    @Test
    @DisplayName("게시글 공지 토글 - 일반 게시글을 공지로 설정")
    void shouldTogglePostNotice_WhenNormalPostToNotice() {
        // Given
        Long postId = 123L;
        String postTitle = "중요한 공지사항";

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(post.getTitle()).willReturn(postTitle);
        given(post.isNotice()).willReturn(false); // 현재 공지 아님

        // When
        postAdminService.togglePostNotice(postId);

        // Then
        verify(postQueryPort).findById(postId);
        verify(post, times(2)).isNotice(); // 상태 확인 (if문 + 로그)
        verify(post).setAsNotice();
        verify(postCommandPort).save(post);
    }

    @Test
    @DisplayName("게시글 공지 토글 - 존재하지 않는 게시글")
    void shouldThrowException_WhenToggleNonExistentPost() {
        // Given
        Long postId = 999L;

        given(postQueryPort.findById(postId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> postAdminService.togglePostNotice(postId))
                .isInstanceOf(PostCustomException.class)
                .hasFieldOrPropertyWithValue("postErrorCode", PostErrorCode.POST_NOT_FOUND);

        verify(postQueryPort).findById(postId);
        verify(post, never()).isNotice();
        verify(post, never()).setAsNotice();
        verify(post, never()).unsetAsNotice();
        verify(postCommandPort, never()).save(any());
    }

    @Test
    @DisplayName("게시글 공지 토글 - null postId")
    void shouldThrowException_WhenTogglePostWithNullId() {
        // Given
        Long postId = null;

        // When & Then
        assertThatThrownBy(() -> postAdminService.togglePostNotice(postId))
                .isInstanceOf(Exception.class);

        verify(postQueryPort).findById(postId);
        verify(post, never()).isNotice();
        verify(post, never()).setAsNotice();
        verify(post, never()).unsetAsNotice();
    }

    @Test
    @DisplayName("게시글 공지 토글 - 공지 게시글을 일반 게시글로 해제")
    void shouldTogglePostNotice_WhenNoticePostToNormal() {
        // Given
        Long postId = 123L;
        String postTitle = "공지 해제될 게시글";

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(post.getTitle()).willReturn(postTitle);
        given(post.isNotice()).willReturn(true); // 현재 공지임

        // When
        postAdminService.togglePostNotice(postId);

        // Then
        verify(postQueryPort).findById(postId);
        verify(post, times(2)).isNotice(); // 상태 확인 (if문 + 로그)
        verify(post).unsetAsNotice();
        verify(postCommandPort).save(post);
    }

    @Test
    @DisplayName("게시글 공지 토글 - 두 번 토글로 원래 상태로 되돌리기")
    void shouldTogglePostNotice_TwiceToReturnOriginalState() {
        // Given
        Long postId = 123L;
        String postTitle = "두 번 토글 테스트";

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(post.getTitle()).willReturn(postTitle);
        given(post.isNotice()).willReturn(false, true); // 첫 번째: 비공지, 두 번째: 공지

        // When - 두 번 토글
        postAdminService.togglePostNotice(postId);
        postAdminService.togglePostNotice(postId);

        // Then
        verify(postQueryPort, times(2)).findById(postId);
        verify(post, times(4)).isNotice(); // 상태 확인 2번 * 2회 (if문 + 로그)
        verify(post, times(1)).setAsNotice();   // 첫 번째: 공지 설정
        verify(post, times(1)).unsetAsNotice(); // 두 번째: 공지 해제
        verify(postCommandPort, times(2)).save(post);
    }

    @Test
    @DisplayName("게시글 공지 토글 - 연속 3번 토글 시나리오")
    void shouldTogglePostNotice_ThreeTimes() {
        // Given
        Long postId = 123L;
        String postTitle = "연속 토글 테스트";

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(post.getTitle()).willReturn(postTitle);
        given(post.isNotice()).willReturn(false, true, true, false, false, true); // 각 토글마다 2회 호출 고려

        // When - 3번 토글
        postAdminService.togglePostNotice(postId);
        postAdminService.togglePostNotice(postId);
        postAdminService.togglePostNotice(postId);

        // Then
        verify(postQueryPort, times(3)).findById(postId);
        verify(post, times(6)).isNotice(); // 상태 확인 3번 * 2회 (if문 + 로그)
        verify(post, times(2)).setAsNotice();   // 1번째, 3번째
        verify(post, times(1)).unsetAsNotice(); // 2번째
        verify(postCommandPort, times(3)).save(post);
    }

    @Test
    @DisplayName("게시글 공지 토글 - 이미 공지인 게시글 토글 (멱등성 확인)")
    void shouldTogglePostNotice_WhenAlreadyNotice() {
        // Given
        Long postId = 123L;
        String postTitle = "이미 공지인 게시글";

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(post.getTitle()).willReturn(postTitle);
        given(post.isNotice()).willReturn(true); // 이미 공지임

        // When - 공지 상태에서 토글
        postAdminService.togglePostNotice(postId);

        // Then - 공지 해제됨
        verify(postQueryPort).findById(postId);
        verify(post, times(2)).isNotice(); // 상태 확인 (if문 + 로그)
        verify(post).unsetAsNotice();
        verify(postCommandPort).save(post);
        verify(post, never()).setAsNotice(); // 설정은 호출되지 않음
    }

    @Test
    @DisplayName("게시글 공지 토글 - 이미 비공지인 게시글 토글 (멱등성 확인)")
    void shouldTogglePostNotice_WhenAlreadyNonNotice() {
        // Given
        Long postId = 123L;
        String postTitle = "이미 비공지인 게시글";

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(post.getTitle()).willReturn(postTitle);
        given(post.isNotice()).willReturn(false); // 이미 비공지임

        // When - 비공지 상태에서 토글
        postAdminService.togglePostNotice(postId);

        // Then - 공지로 설정됨
        verify(postQueryPort).findById(postId);
        verify(post, times(2)).isNotice(); // 상태 확인 (if문 + 로그)
        verify(post).setAsNotice();
        verify(postCommandPort).save(post);
        verify(post, never()).unsetAsNotice(); // 해제는 호출되지 않음
    }

    @Test
    @DisplayName("게시글 공지 토글 - Mock 에러 상황 테스트")
    void shouldHandleException_WhenMockError() {
        // Given
        Long postId = 123L;

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(post.isNotice()).willReturn(false);
        doThrow(new RuntimeException("Mock error")).when(post).setAsNotice();

        // When & Then
        assertThatThrownBy(() -> postAdminService.togglePostNotice(postId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Mock error");

        verify(postQueryPort).findById(postId);
        verify(post).isNotice();
        verify(post).setAsNotice();
        // 예외로 인해 나머지는 호출되지 않음
        verify(postCommandPort, never()).save(any());
    }

    @Test
    @DisplayName("게시글 공지 토글 - 서비스 트랜잭션 동작 검증")
    void shouldVerifyTransactionalBehavior() {
        // Given
        Long postId1 = 123L;
        Long postId2 = 124L;
        String postTitle = "트랜잭션 테스트 게시글";

        given(postQueryPort.findById(any())).willReturn(Optional.of(post));
        given(post.getTitle()).willReturn(postTitle);
        given(post.isNotice()).willReturn(false, true); // 첫 번째: 비공지, 두 번째: 공지

        // When - @Transactional 메서드 호출
        postAdminService.togglePostNotice(postId1);
        postAdminService.togglePostNotice(postId2);

        // Then - 모든 작업이 트랜잭션 내에서 수행됨
        verify(postQueryPort, times(2)).findById(any());
        verify(post, times(4)).isNotice(); // 상태 확인 2번 * 2회 (if문 + 로그)
        verify(post, times(1)).setAsNotice();   // 첫 번째 토글: 공지 설정
        verify(post, times(1)).unsetAsNotice(); // 두 번째 토글: 공지 해제
        verify(postCommandPort, times(2)).save(post);
    }

    @Test
    @DisplayName("게시글 공지 토글 - 메서드 호출 순서 검증")
    void shouldVerifyCorrectExecutionOrder() {
        // Given
        Long postId = 123L;
        String postTitle = "순서 검증 테스트";

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(post.getTitle()).willReturn(postTitle);
        given(post.isNotice()).willReturn(false); // 비공지 상태

        // When
        postAdminService.togglePostNotice(postId);

        // Then - 실행 순서 검증 (InOrder를 사용)
        var inOrder = inOrder(postQueryPort, post, postCommandPort);
        inOrder.verify(postQueryPort).findById(postId);
        inOrder.verify(post).isNotice(); // if문에서 호출
        inOrder.verify(post).setAsNotice();
        inOrder.verify(postCommandPort).save(post);
        inOrder.verify(post).isNotice(); // 로그에서 호출
    }

    @Test
    @DisplayName("게시글 공지 토글 - 대량 토글 처리")
    void shouldHandleBulkToggleOperations() {
        // Given
        int operationCount = 10;
        String postTitle = "대량 토글 테스트";

        given(postQueryPort.findById(any())).willReturn(Optional.of(post));
        given(post.getTitle()).willReturn(postTitle);
        // 10번 토글: false->true->false->true->...
        given(post.isNotice()).willReturn(false, true, false, true, false, true, false, true, false, true);

        // When - 10번의 토글 수행
        for (int i = 0; i < operationCount; i++) {
            postAdminService.togglePostNotice((long) i);
        }

        // Then
        verify(postQueryPort, times(operationCount)).findById(any());
        verify(post, times(operationCount * 2)).isNotice(); // 10번 상태 확인 * 2회 (if문 + 로그)
        verify(post, times(5)).setAsNotice();   // 짝수 번째 (0,2,4,6,8): 비공지에서 공지로
        verify(post, times(5)).unsetAsNotice(); // 홀수 번째 (1,3,5,7,9): 공지에서 비공지로
        verify(postCommandPort, times(operationCount)).save(post);
    }

    @Test
    @DisplayName("게시글 공지 상태 확인 - 공지인 게시글")
    void shouldReturnTrue_WhenPostIsNotice() {
        // Given
        Long postId = 123L;
        
        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(post.isNotice()).willReturn(true);

        // When
        boolean result = postAdminService.isPostNotice(postId);

        // Then
        assertThat(result).isTrue();
        verify(postQueryPort).findById(postId);
        verify(post).isNotice();
    }

    @Test
    @DisplayName("게시글 공지 상태 확인 - 일반 게시글")
    void shouldReturnFalse_WhenPostIsNotNotice() {
        // Given
        Long postId = 123L;
        
        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(post.isNotice()).willReturn(false);

        // When
        boolean result = postAdminService.isPostNotice(postId);

        // Then
        assertThat(result).isFalse();
        verify(postQueryPort).findById(postId);
        verify(post).isNotice();
    }

    @Test
    @DisplayName("게시글 공지 상태 확인 - 존재하지 않는 게시글")
    void shouldThrowException_WhenCheckNonExistentPostNotice() {
        // Given
        Long postId = 999L;

        given(postQueryPort.findById(postId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> postAdminService.isPostNotice(postId))
                .isInstanceOf(PostCustomException.class)
                .hasFieldOrPropertyWithValue("postErrorCode", PostErrorCode.POST_NOT_FOUND);

        verify(postQueryPort).findById(postId);
        verify(post, never()).isNotice();
    }

    @Test
    @DisplayName("게시글 공지 상태 확인 - null postId")
    void shouldThrowException_WhenCheckPostNoticeWithNullId() {
        // Given
        Long postId = null;

        // When & Then
        assertThatThrownBy(() -> postAdminService.isPostNotice(postId))
                .isInstanceOf(Exception.class);

        verify(postQueryPort).findById(postId);
        verify(post, never()).isNotice();
    }
}