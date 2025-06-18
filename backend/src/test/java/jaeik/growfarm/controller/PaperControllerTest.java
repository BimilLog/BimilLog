//package jaeik.growfarm.controller;
//
//import jaeik.growfarm.dto.paper.MessageDTO;
//import jaeik.growfarm.dto.paper.VisitMessageDTO;
//import jaeik.growfarm.global.auth.CustomUserDetails;
//import jaeik.growfarm.service.PaperService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyLong;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.*;
//
//@SpringBootTest
//public class PaperControllerTest {
//
//    @Mock
//    private PaperService paperService;
//
//    @InjectMocks
//    private PaperController paperController;
//
//    private CustomUserDetails userDetails;
//    private MessageDTO messageDTO;
//    private List<MessageDTO> messageDTOList;
//    private List<VisitMessageDTO> visitMessageDTOList;
//
//    @BeforeEach
//    void setUp() {
//        // Setup mock data
//        userDetails = mock(CustomUserDetails.class);
//        when(userDetails.getClientDTO()).thenReturn(mock(jaeik.growfarm.dto.user.UserDTO.class));
//        when(userDetails.getClientDTO().getUserId()).thenReturn(1L);
//
//        messageDTO = new MessageDTO();
//        // Set properties for cropDTO if needed
//
//        messageDTOList = new ArrayList<>();
//        messageDTOList.add(messageDTO);
//
//        VisitMessageDTO visitMessageDTO = new VisitMessageDTO();
//        // Set properties for visitCropDTO if needed
//
//        visitMessageDTOList = new ArrayList<>();
//        visitMessageDTOList.add(visitMessageDTO);
//    }
//
////    @Test
////    @DisplayName("내 농장 가기 테스트")
////    void testMyPaper() {
////        // Given
////        when(paperService.myPaper(anyLong())).thenReturn(messageDTOList);
////
////        // When
////        ResponseEntity<List<MessageDTO>> response = paperController.myPaper(userDetails);
////
////        // Then
////        assertEquals(HttpStatus.OK, response.getStatusCode());
////        assertNotNull(response.getBody());
////        assertEquals(1, response.getBody().size());
////    }
//
//    @Test
//    @DisplayName("다른 농장 가기 테스트")
//    void testVisitPaper() {
//        // Given
//        when(paperService.visitPaper(anyString())).thenReturn(visitMessageDTOList);
//
//        // When
//        ResponseEntity<List<VisitMessageDTO>> response = paperController.visitPaper("testPaper");
//
//        // Then
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNotNull(response.getBody());
//        assertEquals(1, response.getBody().size());
//    }
//
//    @Test
//    @DisplayName("농작물 삭제 테스트")
//    void testDeleteCrop() {
//        // Given
//        doNothing().when(paperService).deleteCrop(any(), anyLong());
//
//        // When
//        ResponseEntity<String> response = paperController.deleteCrop(userDetails, 1L);
//
//        // Then
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals("농작물이 삭제되었습니다.", response.getBody());
//        verify(paperService, times(1)).deleteCrop(userDetails, 1L);
//    }
//
//    @Test
//    @DisplayName("농작물 심기 테스트")
//    void testPlantCrop() throws IOException {
//        // Given
//        doNothing().when(paperService).plantCrop(anyString(), any());
//
//        // When
//        ResponseEntity<String> response = paperController.plantCrop("testPaper", messageDTO);
//
//        // Then
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals("농작물이 심어졌습니다.", response.getBody());
//        verify(paperService, times(1)).plantCrop("testPaper", messageDTO);
//    }
//}
