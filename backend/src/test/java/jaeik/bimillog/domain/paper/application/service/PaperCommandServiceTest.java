package jaeik.bimillog.domain.paper.application.service;

import jaeik.bimillog.domain.paper.application.port.out.LoadUserPort;
import jaeik.bimillog.domain.paper.application.port.out.PaperCommandPort;
import jaeik.bimillog.domain.paper.application.port.out.PaperQueryPort;
import jaeik.bimillog.domain.paper.entity.Message;
import jaeik.bimillog.domain.paper.entity.MessageCommand;
import jaeik.bimillog.domain.paper.event.RollingPaperEvent;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.adapter.paper.in.web.dto.MessageDTO;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
@ExtendWith(MockitoExtension.class)
@DisplayName("PaperCommandService 테스트")
class PaperCommandServiceTest {

    @Mock
    private PaperCommandPort paperCommandPort;

    @Mock
    private PaperQueryPort paperQueryPort;

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;


    @Mock
    private User user;

    @Mock
    private Message message;

    @InjectMocks
    private PaperCommandService paperCommandService;

    @Test
    @DisplayName("내 롤링페이퍼 메시지 삭제 - 성공")
    void shouldDeleteMessageInMyPaper_WhenOwnerDeletes() {
        // Given
        Long userId = 1L;
        Long messageId = 123L;
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setId(messageId);

        given(paperQueryPort.findOwnerIdByMessageId(messageId)).willReturn(Optional.of(userId));

        // When
        paperCommandService.deleteMessageInMyPaper(userId, messageDTO.toCommand());

        // Then
        verify(paperQueryPort, times(1)).findOwnerIdByMessageId(messageId);
        verify(paperCommandPort, times(1)).deleteById(messageId);
        verifyNoMoreInteractions(paperQueryPort, paperCommandPort);
    }

    @Test
    @DisplayName("내 롤링페이퍼 메시지 삭제 - 메시지 없음 예외")
    void shouldThrowException_WhenMessageNotFound() {
        // Given
        Long messageId = 999L;
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setId(messageId);

        given(paperQueryPort.findOwnerIdByMessageId(messageId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paperCommandService.deleteMessageInMyPaper(999L, messageDTO.toCommand()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MESSAGE_NOT_FOUND);

        verify(paperQueryPort, times(1)).findOwnerIdByMessageId(messageId);
        verify(paperCommandPort, never()).deleteById(any());
    }

    @Test
    @DisplayName("내 롤링페이퍼 메시지 삭제 - 소유자가 아닌 경우 예외")
    void shouldThrowException_WhenNotOwner() {
        // Given
        Long userId = 1L;
        Long ownerId = 2L; // 다른 사용자
        Long messageId = 123L;
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setId(messageId);

        given(paperQueryPort.findOwnerIdByMessageId(messageId)).willReturn(Optional.of(ownerId));

        // When & Then
        assertThatThrownBy(() -> paperCommandService.deleteMessageInMyPaper(userId, messageDTO.toCommand()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MESSAGE_DELETE_FORBIDDEN);

        verify(paperQueryPort, times(1)).findOwnerIdByMessageId(messageId);
        verify(paperCommandPort, never()).deleteById(any());
    }

    @Test
    @DisplayName("메시지 작성 - 성공")
    void shouldWriteMessage_WhenValidInput() {
        // Given
        String userName = "testuser";
        Long userId = 1L;
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setContent("테스트 메시지");
        
        given(loadUserPort.findByUserName(userName)).willReturn(Optional.of(user));
        given(user.getId()).willReturn(userId);

        // When
        paperCommandService.writeMessage(userName, messageDTO.toCommand());

        // Then
        verify(loadUserPort, times(1)).findByUserName(userName);
        verify(paperCommandPort, times(1)).save(any(Message.class));
        verify(eventPublisher, times(1)).publishEvent(any(RollingPaperEvent.class));
        verifyNoMoreInteractions(loadUserPort, paperCommandPort, eventPublisher);
    }

    @Test
    @DisplayName("메시지 작성 - 사용자 없음 예외")
    void shouldThrowException_WhenUserNotFound() {
        // Given
        String userName = "nonexistentuser";
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setContent("테스트 메시지");

        given(loadUserPort.findByUserName(userName)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paperCommandService.writeMessage(userName, messageDTO.toCommand()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

        verify(loadUserPort, times(1)).findByUserName(userName);
        verify(paperCommandPort, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("메시지 작성 - null 사용자명")
    void shouldThrowException_WhenUserNameIsNull() {
        // Given
        String userName = null;
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setContent("테스트 메시지");

        given(loadUserPort.findByUserName(userName)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paperCommandService.writeMessage(userName, messageDTO.toCommand()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

        verify(loadUserPort, times(1)).findByUserName(userName);
        verify(paperCommandPort, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("메시지 작성 - 빈 메시지 내용")
    void shouldWriteMessage_WhenEmptyContent() {
        // Given
        String userName = "testuser";
        Long userId = 1L;
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setContent("");
        
        given(loadUserPort.findByUserName(userName)).willReturn(Optional.of(user));
        given(user.getId()).willReturn(userId);

        // When
        paperCommandService.writeMessage(userName, messageDTO.toCommand());

        // Then
        verify(loadUserPort, times(1)).findByUserName(userName);
        verify(paperCommandPort, times(1)).save(any(Message.class));
        verify(eventPublisher, times(1)).publishEvent(any(RollingPaperEvent.class));
    }

    @Test
    @DisplayName("메시지 작성 - null 메시지 Command")
    void shouldThrowException_WhenNullMessageCommand() {
        // Given
        String userName = "testuser";
        MessageCommand messageCommand = null;

        // When & Then
        assertThatThrownBy(() -> paperCommandService.writeMessage(userName, messageCommand))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);

        verify(loadUserPort, never()).findByUserName(any());
        verify(paperCommandPort, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("메시지 작성 - 이벤트 발행 검증")
    void shouldPublishCorrectEvent_WhenWriteMessage() {
        // Given
        String userName = "testuser";
        Long userId = 123L;
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setContent("테스트 메시지");
        
        given(loadUserPort.findByUserName(userName)).willReturn(Optional.of(user));
        given(user.getId()).willReturn(userId);

        // When
        paperCommandService.writeMessage(userName, messageDTO.toCommand());

        // Then
        verify(eventPublisher, times(1)).publishEvent(argThat((RollingPaperEvent event) -> 
            event.getPaperOwnerId().equals(userId) && 
            event.getUserName().equals(userName)
        ));
    }

    @Test
    @DisplayName("내 롤링페이퍼 메시지 삭제 - null Command")
    void shouldThrowException_WhenMessageCommandIsNull() {
        // Given
        MessageCommand messageCommand = null;

        // When & Then
        assertThatThrownBy(() -> paperCommandService.deleteMessageInMyPaper(1L, messageCommand))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
    }


    @Test
    @DisplayName("메시지 작성 - 길이 제한 내 긴 메시지")
    void shouldWriteMessage_WhenLongContentWithinLimit() {
        // Given
        String userName = "testuser";
        Long userId = 1L;
        String longContent = "A".repeat(255); // 최대 길이
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setContent(longContent);
        
        given(loadUserPort.findByUserName(userName)).willReturn(Optional.of(user));
        given(user.getId()).willReturn(userId);

        // When
        paperCommandService.writeMessage(userName, messageDTO.toCommand());

        // Then
        verify(loadUserPort, times(1)).findByUserName(userName);
        verify(paperCommandPort, times(1)).save(any(Message.class));
        verify(eventPublisher, times(1)).publishEvent(any(RollingPaperEvent.class));
    }

    @Test
    @DisplayName("메시지 작성 - 익명 이름 설정")
    void shouldWriteMessage_WithAnonymousName() {
        // Given
        String userName = "testuser";
        Long userId = 1L;
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setContent("테스트 메시지");
        messageDTO.setAnonymity("익명123");
        
        given(loadUserPort.findByUserName(userName)).willReturn(Optional.of(user));
        given(user.getId()).willReturn(userId);

        // When
        paperCommandService.writeMessage(userName, messageDTO.toCommand());

        // Then
        verify(loadUserPort, times(1)).findByUserName(userName);
        verify(paperCommandPort, times(1)).save(any(Message.class));
        verify(eventPublisher, times(1)).publishEvent(any(RollingPaperEvent.class));
    }

    @Test
    @DisplayName("메시지 작성 - 데코레이션 타입 설정")
    void shouldWriteMessage_WithDecoType() {
        // Given
        String userName = "testuser";
        Long userId = 1L;
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setContent("테스트 메시지");
        messageDTO.setWidth(100);
        messageDTO.setHeight(50);
        
        given(loadUserPort.findByUserName(userName)).willReturn(Optional.of(user));
        given(user.getId()).willReturn(userId);

        // When
        paperCommandService.writeMessage(userName, messageDTO.toCommand());

        // Then
        verify(loadUserPort, times(1)).findByUserName(userName);
        verify(paperCommandPort, times(1)).save(any(Message.class));
        verify(eventPublisher, times(1)).publishEvent(argThat((RollingPaperEvent event) -> 
            event.getPaperOwnerId().equals(userId) && 
            event.getUserName().equals(userName)
        ));
    }

    @Test
    @DisplayName("여러 메시지 삭제 시나리오")
    void shouldDeleteMultipleMessages_WhenOwner() {
        // Given
        Long userId = 1L;
        Long[] messageIds = {100L, 200L, 300L};
        
        for (Long messageId : messageIds) {
            MessageDTO messageDTO = new MessageDTO();
            messageDTO.setId(messageId);
            
            given(paperQueryPort.findOwnerIdByMessageId(messageId)).willReturn(Optional.of(userId));
            
            // When
            paperCommandService.deleteMessageInMyPaper(userId, messageDTO.toCommand());
            
            // Then
            verify(paperCommandPort, times(1)).deleteById(messageId);
        }
    }

    @Test
    @DisplayName("메시지 작성 - 특수문자가 포함된 사용자명")
    void shouldWriteMessage_WithSpecialCharacterUserName() {
        // Given
        String userName = "user@test_123";
        Long userId = 1L;
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setContent("테스트 메시지");
        
        given(loadUserPort.findByUserName(userName)).willReturn(Optional.of(user));
        given(user.getId()).willReturn(userId);

        // When
        paperCommandService.writeMessage(userName, messageDTO.toCommand());

        // Then
        verify(loadUserPort, times(1)).findByUserName(userName);
        verify(paperCommandPort, times(1)).save(any(Message.class));
        verify(eventPublisher, times(1)).publishEvent(any(RollingPaperEvent.class));
    }

    @Test
    @DisplayName("메시지 삭제 - 소유자 검증 순서 확인")
    void shouldVerifyOwnershipBeforeDeletion() {
        // Given
        Long userId = 1L;
        Long ownerId = 2L; // 다른 사용자
        Long messageId = 123L;
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setId(messageId);

        given(paperQueryPort.findOwnerIdByMessageId(messageId)).willReturn(Optional.of(ownerId));

        // When & Then
        assertThatThrownBy(() -> paperCommandService.deleteMessageInMyPaper(userId, messageDTO.toCommand()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MESSAGE_DELETE_FORBIDDEN);

        // 소유권 검증에 실패했으므로 삭제가 호출되지 않음
        verify(paperQueryPort, times(1)).findOwnerIdByMessageId(messageId);
        verify(paperCommandPort, never()).deleteById(any());
    }
}