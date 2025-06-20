package jaeik.growfarm.unit.controller;

import jaeik.growfarm.controller.CommentController;
import jaeik.growfarm.dto.comment.CommentDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.service.comment.CommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <h2>CommentController 단위 테스트</h2>
 * <p>
 * 댓글 관련 컨트롤러의 메서드들을 테스트합니다.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CommentControllerTest {

    @Mock
    private CommentService commentService;

    @InjectMocks
    private CommentController commentController;

    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUserId()).thenReturn(1L);
    }

    @Test
    @DisplayName("댓글 조회 테스트")
    void testGetComments() {
        // Given
        CommentDTO commentDTO = mock(CommentDTO.class);
        List<CommentDTO> commentList = new ArrayList<>();
        commentList.add(commentDTO);
        Page<CommentDTO> commentPage = new PageImpl<>(commentList);

        when(commentService.getCommentsLatestOrder(anyLong(), anyInt(), any())).thenReturn(commentPage);

        // When
        ResponseEntity<Page<CommentDTO>> response = commentController.getComments(userDetails, 1L, 0);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getContent().size());
        verify(commentService, times(1)).getCommentsLatestOrder(1L, 0, userDetails);
    }

    @Test
    @DisplayName("인기댓글 조회 테스트")
    void testGetPopularComments() {
        // Given
        CommentDTO commentDTO = mock(CommentDTO.class);
        List<CommentDTO> commentList = new ArrayList<>();
        commentList.add(commentDTO);

        when(commentService.getPopularComments(anyLong(), any())).thenReturn(commentList);

        // When
        ResponseEntity<List<CommentDTO>> response = commentController.getPopularComments(userDetails, 1L);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(commentService, times(1)).getPopularComments(1L, userDetails);
    }

    @Test
    @DisplayName("댓글 작성 테스트")
    void testWriteComment() {
        // Given
        CommentDTO commentDTO = mock(CommentDTO.class);
        doNothing().when(commentService).writeComment(any(), any());

        // When
        ResponseEntity<String> response = commentController.writeComment(commentDTO, userDetails);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("댓글 작성 완료", response.getBody());
        verify(commentService, times(1)).writeComment(userDetails, commentDTO);
    }

    @Test
    @DisplayName("댓글 수정 테스트")
    void testUpdateComment() {
        // Given
        CommentDTO commentDTO = mock(CommentDTO.class);
        doNothing().when(commentService).updateComment(any(), any());

        // When
        ResponseEntity<String> response = commentController.updateComment(userDetails, commentDTO);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("댓글 수정 완료", response.getBody());
        verify(commentService, times(1)).updateComment(commentDTO, userDetails);
    }

    @Test
    @DisplayName("댓글 삭제 테스트")
    void testDeleteComment() {
        // Given
        CommentDTO commentDTO = mock(CommentDTO.class);
        doNothing().when(commentService).deleteComment(any(), any());

        // When
        ResponseEntity<String> response = commentController.deleteComment(userDetails, commentDTO);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("댓글 삭제 완료", response.getBody());
        verify(commentService, times(1)).deleteComment(commentDTO, userDetails);
    }

    @Test
    @DisplayName("댓글 추천 테스트")
    void testLikeComment() {
        // Given
        CommentDTO commentDTO = mock(CommentDTO.class);
        doNothing().when(commentService).likeComment(any(), any());

        // When
        ResponseEntity<String> response = commentController.likeComment(commentDTO, userDetails);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("추천 처리 완료", response.getBody());
        verify(commentService, times(1)).likeComment(commentDTO, userDetails);
    }
}
