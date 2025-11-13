package jaeik.bimillog.domain.paper.service;

import jaeik.bimillog.domain.global.out.GlobalMemberQueryAdapter;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.paper.entity.DecoType;
import jaeik.bimillog.domain.paper.entity.Message;
import jaeik.bimillog.domain.paper.event.RollingPaperEvent;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.domain.paper.out.PaperCommandRepository;
import jaeik.bimillog.domain.paper.out.PaperQueryRepository;
import jaeik.bimillog.testutil.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>PaperCommandService 테스트</h2>
 * <p>롤링페이퍼 명령 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("PaperCommandService 테스트")
@Tag("unit")
class PaperCommandServiceTest extends BaseUnitTest {

    @Mock
    private PaperCommandRepository paperCommandRepository;

    @Mock
    private PaperQueryRepository paperQueryRepository;

    @Mock
    private GlobalMemberQueryAdapter globalMemberQueryAdapter;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PaperCommandService paperCommandService;

    @Test
    @DisplayName("내 롤링페이퍼 메시지 삭제 - 성공")
    void shouldDeleteMessageInMyPaper_WhenOwnerDeletes() {
        // Given
        Long memberId = 1L;
        Long messageId = 123L;

        given(paperQueryRepository.findOwnerIdByMessageId(messageId)).willReturn(Optional.of(memberId));

        // When
        paperCommandService.deleteMessageInMyPaper(memberId, messageId);

        // Then
        verify(paperQueryRepository, times(1)).findOwnerIdByMessageId(messageId);
        verify(paperCommandRepository, times(1)).deleteMessage(memberId, messageId);
        verifyNoMoreInteractions(paperQueryRepository, paperCommandRepository);
    }

    @Test
    @DisplayName("내 롤링페이퍼 메시지 삭제 - 메시지 없음 예외")
    void shouldThrowException_WhenMessageNotFound() {
        // Given
        Long memberId = 999L;
        Long messageId = 999L;

        given(paperQueryRepository.findOwnerIdByMessageId(messageId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paperCommandService.deleteMessageInMyPaper(memberId, messageId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAPER_MESSAGE_NOT_FOUND);

        verify(paperQueryRepository, times(1)).findOwnerIdByMessageId(messageId);
        verify(paperCommandRepository, never()).deleteMessage(any(), any());
    }

    @Test
    @DisplayName("내 롤링페이퍼 메시지 삭제 - 소유자가 아닌 경우 예외")
    void shouldThrowException_WhenNotOwner() {
        // Given
        Long memberId = 1L;
        Long ownerId = 2L; // 다른 사용자
        Long messageId = 123L;

        given(paperQueryRepository.findOwnerIdByMessageId(messageId)).willReturn(Optional.of(ownerId));

        // When & Then
        assertThatThrownBy(() -> paperCommandService.deleteMessageInMyPaper(memberId, messageId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAPER_MESSAGE_DELETE_FORBIDDEN);

        verify(paperQueryRepository, times(1)).findOwnerIdByMessageId(messageId);
        verify(paperCommandRepository, never()).deleteMessage(any(), any());
    }

    @Test
    @DisplayName("메시지 작성 - 성공")
    void shouldWriteMessage_WhenValidInput() {
        // Given
        Long memberId = 1L;
        Member memberWithId = createTestMemberWithId(memberId);
        String memberName = memberWithId.getMemberName();
        DecoType decoType = DecoType.APPLE;
        String anonymity = "익명";
        String content = "테스트 메시지";
        int x = 2;
        int y = 2;

        given(globalMemberQueryAdapter.findByMemberName(memberName)).willReturn(Optional.of(memberWithId));

        // When
        paperCommandService.writeMessage(memberName, decoType, anonymity, content, x, y);

        // Then
        verify(globalMemberQueryAdapter, times(1)).findByMemberName(memberName);
        verify(paperCommandRepository, times(1)).save(any(Message.class));
        verify(eventPublisher, times(1)).publishEvent(any(RollingPaperEvent.class));
        verifyNoMoreInteractions(globalMemberQueryAdapter, paperCommandRepository, eventPublisher);
    }

    @Test
    @DisplayName("메시지 작성 - 사용자 없음 예외")
    void shouldThrowException_WhenUserNotFound() {
        // Given
        String memberName = "nonexistentuser";
        DecoType decoType = DecoType.APPLE;
        String anonymity = "익명";
        String content = "테스트 메시지";
        int x = 2;
        int y = 2;

        given(globalMemberQueryAdapter.findByMemberName(memberName)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paperCommandService.writeMessage(memberName, decoType, anonymity, content, x, y))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAPER_USERNAME_NOT_FOUND);

        verify(globalMemberQueryAdapter, times(1)).findByMemberName(memberName);
        verify(paperCommandRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("메시지 작성 - null 또는 빈 사용자명 예외")
    void shouldThrowException_WhenInvalidUserName() {
        // Given - null memberName
        String memberName = null;
        DecoType decoType = DecoType.APPLE;
        String anonymity = "익명";
        String content = "테스트 메시지";
        int x = 2;
        int y = 2;

        // When & Then - null case
        assertThatThrownBy(() -> paperCommandService.writeMessage(memberName, decoType, anonymity, content, x, y))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAPER_INVALID_INPUT_VALUE);

        // Given - empty memberName
        String emptyUserName = "   ";

        // When & Then - empty case
        assertThatThrownBy(() -> paperCommandService.writeMessage(emptyUserName, decoType, anonymity, content, x, y))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAPER_INVALID_INPUT_VALUE);

        verify(globalMemberQueryAdapter, never()).findByMemberName(any());
        verify(paperCommandRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("메시지 작성 - 이벤트 발행 검증")
    void shouldPublishCorrectEvent_WhenWriteMessage() {
        // Given
        Long memberId = 1L;
        Member memberWithId = createTestMemberWithId(memberId);
        String memberName = memberWithId.getMemberName();
        DecoType decoType = DecoType.APPLE;
        String anonymity = "익명";
        String content = "테스트 메시지";
        int x = 2;
        int y = 2;

        given(globalMemberQueryAdapter.findByMemberName(memberName)).willReturn(Optional.of(memberWithId));

        // When
        paperCommandService.writeMessage(memberName, decoType, anonymity, content, x, y);

        // Then
        verify(eventPublisher, times(1)).publishEvent(argThat((RollingPaperEvent event) ->
            event.paperOwnerId().equals(memberId) &&
            event.memberName().equals(memberName)
        ));
    }
}