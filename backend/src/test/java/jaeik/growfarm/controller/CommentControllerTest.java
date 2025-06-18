package jaeik.growfarm.controller;

import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
public class CommentControllerTest {
//
//    @Mock
//    private CommentService commentService;
//
//    @InjectMocks
//    private CommentController commentController;
//
//    private CommentDTO commentDTO;
//    private ReportDTO reportDTO;
//    private CustomUserDetails userDetails;

//    @BeforeEach
//    void setUp() {
//        // Setup mock data
//        commentDTO = new CommentDTO(
//                1L,                           // id
//                1L,                           // postId
//                1L,                           // userId
//                "testPaper",                   // userName
//                "Test comment content",       // content
//                0,                            // likes
//                Instant.now(),          // createdAt
//                false,                        // is_featured
//                false                         // userLike
//        );
//
//        reportDTO = ReportDTO.builder()
//                .reportId(1L)
//                .reportType(ReportType.COMMENT)
//                .userId(1L)
//                .targetId(1L)
//                .content("Test report content")
//                .build();
//
//        userDetails = mock(CustomUserDetails.class);
//    }

//    @Test
//    @DisplayName("댓글 작성 테스트")
//    void testWriteComment() throws IOException {
//        // Given
//        doNothing().when(commentService).writeComment(any(), anyLong(), any());
//
//        // When
//        ResponseEntity<String> response = commentController.writeComment(userDetails, 1L, commentDTO);
//
//        // Then
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals("댓글 작성 완료", response.getBody());
//        verify(commentService, times(1)).writeComment(userDetails, 1L, commentDTO);
//    }

//    @Test
//    @DisplayName("댓글 수정 테스트")
//    void testUpdateComment() {
//        // Given
//        doNothing().when(commentService).updateComment(anyLong(), any(), any());
//
//        // When
//        ResponseEntity<String> response = commentController.updateComment(userDetails, 1L, 1L, commentDTO);
//
//        // Then
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals("댓글 수정 완료", response.getBody());
//        verify(commentService, times(1)).updateComment(1L, commentDTO, userDetails);
//    }
//
//    @Test
//    @DisplayName("댓글 삭제 테스트")
//    void testDeleteComment() {
//        // Given
//        doNothing().when(commentService).deleteComment(anyLong(), any());
//
//        // When
//        ResponseEntity<String> response = commentController.deleteComment(userDetails, 1L, 1L);
//
//        // Then
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals("댓글 삭제 완료", response.getBody());
//        verify(commentService, times(1)).deleteComment(1L, userDetails);
//    }
//
//    @Test
//    @DisplayName("댓글 추천 테스트")
//    void testLikeComment() {
//        // Given
//        doNothing().when(commentService).likeComment(anyLong(), anyLong(), any());
//
//        // When
//        ResponseEntity<String> response = commentController.likeComment(1L, 1L, userDetails);
//
//        // Then
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals("댓글 추천 완료", response.getBody());
//        verify(commentService, times(1)).likeComment(1L, 1L, userDetails);
//    }
//}
