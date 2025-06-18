//package jaeik.growfarm.controller;
//
//import jaeik.growfarm.dto.admin.ReportDTO;
//import jaeik.growfarm.dto.post.PostDTO;
//import jaeik.growfarm.dto.post.PostReqDTO;
//import jaeik.growfarm.dto.post.SimplePostDTO;
//import jaeik.growfarm.entity.report.ReportType;
//import jaeik.growfarm.global.auth.CustomUserDetails;
//import jaeik.growfarm.service.post.PostService;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//
//import java.time.Instant;
//import java.util.ArrayList;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@SpringBootTest
//public class PostControllerTest {
//
//    @Mock
//    private PostService postService;
//
//    @InjectMocks
//    private PostController postController;
//
//    private CustomUserDetails userDetails;
//    private PostDTO postDTO;
//    private PostReqDTO postReqDTO;
//    private SimplePostDTO simplePostDTO;
//    private ReportDTO reportDTO;
//    private List<SimplePostDTO> simplePostDTOList;
//
//    @BeforeEach
//    void setUp() {
//        // Setup mock data
//        userDetails = mock(CustomUserDetails.class);
//
//        // Create PostDTO with required parameters
//        postDTO = new PostDTO(
//                1L,                           // postId
//                1L,                           // userId
//                "testFarm",                   // userName
//                "Test Title",                 // title
//                "Test Content",               // content
//                0,                            // views
//                0,                            // likes
//                false,                        // is_notice
//                false,                        // is_RealtimePopular
//                false,                        // is_WeeklyPopular
//                false,                        // is_HallOfFame
//                Instant.now(),          // createdAt
//                new ArrayList<>(),            // comments
//                false                         // userLike
//        );
//
//        // Create PostReqDTO and set properties
//        postReqDTO = new PostReqDTO();
//        postReqDTO.setUserId(1L);
//        postReqDTO.setFarmName("testFarm");
//        postReqDTO.setTitle("Test Title");
//        postReqDTO.setContent("Test Content");
//
//        // Create SimplePostDTO with required parameters
//        simplePostDTO = new SimplePostDTO(
//                1L,                           // postId
//                1L,                           // userId
//                "testFarm",                   // userName
//                "Test Title",                 // title
//                0,                            // commentCount
//                0,                            // likes
//                0,                            // views
//                Instant.now(),          // createdAt
//                false                        // is_notice
//                // is_RealtimePopular
//                // is_WeeklyPopular
//                // is_HallOfFame
//        );
//
//        simplePostDTOList = new ArrayList<>();
//        simplePostDTOList.add(simplePostDTO);
//
//        reportDTO = ReportDTO.builder()
//                .reportId(1L)
//                .reportType(ReportType.POST)
//                .userId(1L)
//                .targetId(1L)
//                .content("Test report content")
//                .build();
//    }
//
//    @Test
//    @DisplayName("게시판 조회 테스트")
//    void testGetBoard() {
//        // Given
//        Page<SimplePostDTO> postPage = new PageImpl<>(simplePostDTOList);
//        when(postService.getBoard(anyInt(), anyInt())).thenReturn(postPage);
//
//        // When
//        ResponseEntity<Page<SimplePostDTO>> response = postController.getBoard(0, 10);
//
//        // Then
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNotNull(response.getBody());
//        assertEquals(1, response.getBody().getContent().size());
//    }
//
//    @Test
//    @DisplayName("실시간 인기글 조회 테스트")
//    void testGetRealtimeBoard() {
//        // Given
//        when(postService.getRealtimePopularPosts()).thenReturn(simplePostDTOList);
//
//        // When
//        ResponseEntity<List<SimplePostDTO>> response = postController.getRealtimeBoard();
//
//        // Then
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNotNull(response.getBody());
//        assertEquals(1, response.getBody().size());
//    }
//
//    @Test
//    @DisplayName("주간 인기글 조회 테스트")
//    void testGetWeeklyBoard() {
//        // Given
//        when(postService.getWeeklyPopularPosts()).thenReturn(simplePostDTOList);
//
//        // When
//        ResponseEntity<List<SimplePostDTO>> response = postController.getWeeklyBoard();
//
//        // Then
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNotNull(response.getBody());
//        assertEquals(1, response.getBody().size());
//    }
//
//    @Test
//    @DisplayName("명예의 전당 조회 테스트")
//    void testGetHallOfFameBoard() {
//        // Given
//        when(postService.getHallOfFamePosts()).thenReturn(simplePostDTOList);
//
//        // When
//        ResponseEntity<List<SimplePostDTO>> response = postController.getHallOfFameBoard();
//
//        // Then
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNotNull(response.getBody());
//        assertEquals(1, response.getBody().size());
//    }
//
//    @Test
//    @DisplayName("게시글 검색 테스트")
//    void testSearchPost() {
//        // Given
//        Page<SimplePostDTO> postPage = new PageImpl<>(simplePostDTOList);
//        when(postService.searchPost(anyString(), anyString(), anyInt(), anyInt())).thenReturn(postPage);
//
//        // When
//        ResponseEntity<Page<SimplePostDTO>> response = postController.searchPost("title", "test", 0, 10);
//
//        // Then
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNotNull(response.getBody());
//        assertEquals(1, response.getBody().getContent().size());
//    }
//
//    @Test
//    @DisplayName("게시글 조회 테스트")
//    void testGetPost() {
//        // Given
//        when(postService.getPost(anyLong(), any())).thenReturn(postDTO);
//
//        // When
//        ResponseEntity<PostDTO> response = postController.getPost(1L, 1L, mock(HttpServletRequest.class), mock(HttpServletResponse.class));
//
//        // Then
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNotNull(response.getBody());
//    }
//
//    @Test
//    @DisplayName("게시글 작성 테스트")
//    void testWritePost() {
//        // Given
//        when(postService.writePost(any(), any())).thenReturn(postDTO);
//
//        // When
//        ResponseEntity<PostDTO> response = postController.writePost(userDetails, postReqDTO);
//
//        // Then
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNotNull(response.getBody());
//    }
//
//    @Test
//    @DisplayName("게시글 수정 테스트")
//    void testUpdatePost() {
//        // Given
//        when(postService.updatePost(anyLong(), any(), any())).thenReturn(postDTO);
//
//        // When
//        ResponseEntity<PostDTO> response = postController.updatePost(1L, userDetails, postDTO);
//
//        // Then
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNotNull(response.getBody());
//    }
//
//    @Test
//    @DisplayName("게시글 삭제 테스트")
//    void testDeletePost() {
//        // Given
//        doNothing().when(postService).deletePost(anyLong(), any());
//
//        // When
//        ResponseEntity<String> response = postController.deletePost(1L, userDetails);
//
//        // Then
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals("게시글 삭제 완료", response.getBody());
//    }
//
//    @Test
//    @DisplayName("게시글 추천 테스트")
//    void testLikePost() {
//        // Given
//        doNothing().when(postService).likePost(anyLong(), any());
//
//        // When
//        ResponseEntity<String> response = postController.likePost(1L, userDetails);
//
//        // Then
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals("게시글 추천 완료", response.getBody());
//    }
//}
