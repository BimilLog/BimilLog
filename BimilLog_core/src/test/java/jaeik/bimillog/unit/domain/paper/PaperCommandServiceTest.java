package jaeik.bimillog.unit.domain.paper;

import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.paper.dto.MessageWriteDTO;
import jaeik.bimillog.domain.paper.entity.Message;
import jaeik.bimillog.domain.paper.event.MessageDeletedEvent;
import jaeik.bimillog.domain.paper.event.RollingPaperEvent;
import jaeik.bimillog.domain.paper.repository.PaperRepository;
import jaeik.bimillog.domain.paper.adapter.PaperToMemberAdapter;
import jaeik.bimillog.domain.paper.service.PaperCommandService;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.fixtures.TestFixtures;
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
    private PaperRepository paperRepository;

    @Mock
    private PaperToMemberAdapter paperToMemberAdapter;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private jaeik.bimillog.infrastructure.redis.paper.RedisPaperDeleteAdapter redisPaperDeleteAdapter;

    @InjectMocks
    private PaperCommandService paperCommandService;

    @Test
    @DisplayName("내 롤링페이퍼 메시지 삭제 - 성공")
    void shouldDeleteMessage_WhenOwnerDeletes() {
        // Given
        Long memberId = 1L;
        Long messageId = 123L;
        Member owner = createTestMemberWithId(memberId);
        Message message = mock(Message.class);
        given(message.getMember()).willReturn(owner);

        given(paperRepository.findById(messageId)).willReturn(Optional.of(message));

        // When
        paperCommandService.deleteMessage(memberId, messageId);

        // Then
        verify(paperRepository, times(1)).findById(messageId);
        verify(paperRepository, times(1)).deleteById(messageId);
        verify(eventPublisher, times(1)).publishEvent(any(MessageDeletedEvent.class));
    }

    @Test
    @DisplayName("내 롤링페이퍼 메시지 삭제 - 메시지 없음 예외")
    void shouldThrowException_WhenMessageNotFound() {
        // Given
        Long memberId = 999L;
        Long messageId = 999L;

        given(paperRepository.findById(messageId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paperCommandService.deleteMessage(memberId, messageId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAPER_MESSAGE_NOT_FOUND);

        verify(paperRepository, times(1)).findById(messageId);
        verify(paperRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("내 롤링페이퍼 메시지 삭제 - 소유자가 아닌 경우 예외")
    void shouldThrowException_WhenNotOwner() {
        // Given
        Long memberId = 1L;
        Long ownerId = 2L; // 다른 사용자
        Long messageId = 123L;
        Member owner = createTestMemberWithId(ownerId);
        Message message = mock(Message.class);
        given(message.getMember()).willReturn(owner);

        given(paperRepository.findById(messageId)).willReturn(Optional.of(message));

        // When & Then
        assertThatThrownBy(() -> paperCommandService.deleteMessage(memberId, messageId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAPER_MESSAGE_DELETE_FORBIDDEN);

        verify(paperRepository, times(1)).findById(messageId);
        verify(paperRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("메시지 작성 - 성공")
    void shouldWriteMessage_WhenValidInput() {
        // Given
        Long memberId = 1L;
        Long ownerId = 2L;
        Member owner = createTestMemberWithId(ownerId);
        MessageWriteDTO dto = TestFixtures.createMessageWriteDTO(ownerId, "테스트 메시지", 2, 2);

        given(paperToMemberAdapter.getMemberById(ownerId)).willReturn(owner);

        // When
        paperCommandService.writeMessage(memberId, dto);

        // Then
        verify(paperToMemberAdapter, times(1)).getMemberById(ownerId);
        verify(paperRepository, times(1)).save(any(Message.class));
        verify(eventPublisher, times(1)).publishEvent(any(RollingPaperEvent.class));
    }

    @Test
    @DisplayName("메시지 작성 - 소유자 없음 예외")
    void shouldThrowException_WhenOwnerNotFound() {
        // Given
        Long memberId = 1L;
        Long nonExistentOwnerId = 999L;
        MessageWriteDTO dto = TestFixtures.createMessageWriteDTO(nonExistentOwnerId, "테스트 메시지", 2, 2);

        given(paperToMemberAdapter.getMemberById(nonExistentOwnerId))
                .willThrow(new CustomException(ErrorCode.MEMBER_USER_NOT_FOUND));

        // When & Then
        assertThatThrownBy(() -> paperCommandService.writeMessage(memberId, dto))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_USER_NOT_FOUND);

        verify(paperToMemberAdapter, times(1)).getMemberById(nonExistentOwnerId);
        verify(paperRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

}