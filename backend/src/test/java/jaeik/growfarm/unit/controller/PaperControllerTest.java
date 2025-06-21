package jaeik.growfarm.unit.controller;

import jaeik.growfarm.controller.PaperController;
import jaeik.growfarm.dto.paper.MessageDTO;
import jaeik.growfarm.dto.paper.VisitMessageDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.service.paper.PaperService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * <h2>PaperController 단위 테스트</h2>
 * <p>
 * 롤링페이퍼 관련 컨트롤러의 메서드들을 테스트합니다.
 * </p>
 * @version 1.0.0
 * @author Jaeik
 */
@SpringBootTest
public class PaperControllerTest {

    @Mock
    private PaperService paperService;

    @InjectMocks
    private PaperController paperController;

    private CustomUserDetails userDetails;
    private MessageDTO messageDTO;
    private List<MessageDTO> messageDTOList;
    private List<VisitMessageDTO> visitMessageDTOList;

    @BeforeEach
    void setUp() {
        // Setup mock data
        userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUserId()).thenReturn(1L);

        messageDTO = new MessageDTO();
        messageDTO.setId(1L);
        messageDTO.setContent("Test message");

        messageDTOList = new ArrayList<>();
        messageDTOList.add(messageDTO);

        VisitMessageDTO visitMessageDTO = new VisitMessageDTO();
        visitMessageDTO.setId(1L);
        visitMessageDTO.setUserId(1L);

        visitMessageDTOList = new ArrayList<>();
        visitMessageDTOList.add(visitMessageDTO);
    }

    @Test
    @DisplayName("내 롤링페이퍼 조회 테스트")
    void testMyPaper() {
        // Given
        when(paperService.myPaper(any())).thenReturn(messageDTOList);

        // When
        ResponseEntity<List<MessageDTO>> response = paperController.myPaper(userDetails);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(paperService, times(1)).myPaper(userDetails);
    }

    @Test
    @DisplayName("다른 롤링페이퍼 방문 테스트")
    void testVisitPaper() {
        // Given
        when(paperService.visitPaper(anyString())).thenReturn(visitMessageDTOList);

        // When
        ResponseEntity<List<VisitMessageDTO>> response = paperController.visitPaper("testPaper");

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(paperService, times(1)).visitPaper("testPaper");
    }

    @Test
    @DisplayName("메시지 삭제 테스트")
    void testDeleteCrop() {
        // Given
        doNothing().when(paperService).deleteMessageInMyPaper(any(), any());

        // When
        ResponseEntity<String> response = paperController.deleteMessage(userDetails, messageDTO);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("메시지가 삭제되었습니다.", response.getBody());
        verify(paperService, times(1)).deleteMessageInMyPaper(userDetails, messageDTO);
    }

    @Test
    @DisplayName("메시지 작성 테스트")
    void testPlantCrop() {
        // Given
        doNothing().when(paperService).writeMessage(anyString(), any());

        // When
        ResponseEntity<String> response = paperController.writeMessage("testPaper", messageDTO);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("메시지가 작성되었습니다.", response.getBody());
        verify(paperService, times(1)).writeMessage("testPaper", messageDTO);
    }
}
