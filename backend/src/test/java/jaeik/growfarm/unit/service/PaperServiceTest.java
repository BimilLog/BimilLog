package jaeik.growfarm.unit.service;

import jaeik.growfarm.dto.paper.MessageDTO;
import jaeik.growfarm.dto.paper.VisitMessageDTO;
import jaeik.growfarm.entity.message.Message;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.event.MessageEvent;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.repository.message.MessageRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jaeik.growfarm.service.paper.PaperService;
import jaeik.growfarm.service.paper.PaperUpdateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PaperServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PaperUpdateService paperUpdateService;

    @InjectMocks
    private PaperService paperService;

    private CustomUserDetails userDetails;
    private MessageDTO messageDTO;

    @BeforeEach
    void setUp() {
        // Setup mock data
        userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUserId()).thenReturn(1L);

        Users user = mock(Users.class);
        when(user.getId()).thenReturn(1L);
        when(user.getUserName()).thenReturn("testUser");

        // Create MessageDTO
        messageDTO = new MessageDTO();
        messageDTO.setId(1L);
        messageDTO.setUserId(1L);
        messageDTO.setContent("Test message content");

        List<MessageDTO> messageDTOList = new ArrayList<>();
        messageDTOList.add(messageDTO);

        // Create VisitMessageDTO
        VisitMessageDTO visitMessageDTO = new VisitMessageDTO();
        visitMessageDTO.setId(1L);
        visitMessageDTO.setUserId(1L);

        List<VisitMessageDTO> visitMessageDTOList = new ArrayList<>();
        visitMessageDTOList.add(visitMessageDTO);

        // Setup mock repositories
        when(messageRepository.findMessageDTOsByUserId(anyLong())).thenReturn(messageDTOList);
        when(messageRepository.findVisitMessageDTOsByUserName(anyString())).thenReturn(visitMessageDTOList);
        when(userRepository.findByUserName(anyString())).thenReturn(user);
    }

    @Test
    @DisplayName("내 롤링페이퍼 조회 테스트")
    void testMyPaper() {
        // When
        List<MessageDTO> result = paperService.myPaper(userDetails);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test message content", result.getFirst().getContent());
        verify(messageRepository, times(1)).findMessageDTOsByUserId(eq(1L));
    }

    @Test
    @DisplayName("다른 롤링페이퍼 방문 테스트 - 성공")
    void testVisitPaperSuccess() {
        // Given
        when(userRepository.existsByUserName(anyString())).thenReturn(true);

        // When
        List<VisitMessageDTO> result = paperService.visitPaper("testUser");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.getFirst().getId());
        assertEquals(1L, result.getFirst().getUserId());
        verify(userRepository, times(1)).existsByUserName(eq("testUser"));
        verify(messageRepository, times(1)).findVisitMessageDTOsByUserName(eq("testUser"));
    }

    @Test
    @DisplayName("다른 롤링페이퍼 방문 테스트 - 실패 (사용자 없음)")
    void testVisitPaperUserNotFound() {
        // Given
        when(userRepository.existsByUserName(anyString())).thenReturn(false);

        // When & Then
        assertThrows(CustomException.class, () -> paperService.visitPaper("nonExistentUser"));
        verify(userRepository, times(1)).existsByUserName(eq("nonExistentUser"));
        verify(messageRepository, never()).findVisitMessageDTOsByUserName(anyString());
    }

    @Test
    @DisplayName("메시지 작성 테스트 - 성공")
    void testWriteMessageSuccess() {
        // Given
        doNothing().when(paperUpdateService).saveMessage(any(Message.class));
        doNothing().when(eventPublisher).publishEvent(any(MessageEvent.class));

        // When
        paperService.writeMessage("testUser", messageDTO);

        // Then
        verify(userRepository, times(1)).findByUserName(eq("testUser"));
        verify(paperUpdateService, times(1)).saveMessage(any(Message.class));
        verify(eventPublisher, times(1)).publishEvent(any(MessageEvent.class));
    }

    @Test
    @DisplayName("메시지 작성 테스트 - 실패 (사용자 없음)")
    void testWriteMessageUserNotFound() {
        // Given
        when(userRepository.findByUserName(anyString())).thenReturn(null);

        // When & Then
        assertThrows(CustomException.class, () -> paperService.writeMessage("nonExistentUser", messageDTO));
        verify(userRepository, times(1)).findByUserName(eq("nonExistentUser"));
        verify(paperUpdateService, never()).saveMessage(any(Message.class));
        verify(eventPublisher, never()).publishEvent(any(MessageEvent.class));
    }

    @Test
    @DisplayName("메시지 삭제 테스트 - 성공")
    void testDeleteMessageInMyPaperSuccess() {
        // Given
        doNothing().when(paperUpdateService).deleteMessage(any(MessageDTO.class));

        // When
        paperService.deleteMessageInMyPaper(userDetails, messageDTO);

        // Then
        verify(paperUpdateService, times(1)).deleteMessage(eq(messageDTO));
    }

    @Test
    @DisplayName("메시지 삭제 테스트 - 실패 (권한 없음)")
    void testDeleteMessageInMyPaperForbidden() {
        // Given
        MessageDTO otherUserMessageDTO = new MessageDTO();
        otherUserMessageDTO.setId(2L);
        otherUserMessageDTO.setUserId(2L); // Different user ID

        // When & Then
        assertThrows(CustomException.class,
                () -> paperService.deleteMessageInMyPaper(userDetails, otherUserMessageDTO));
        verify(paperUpdateService, never()).deleteMessage(any(MessageDTO.class));
    }
}
