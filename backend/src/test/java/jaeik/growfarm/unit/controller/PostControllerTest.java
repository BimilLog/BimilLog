package jaeik.growfarm.unit.controller;

import jaeik.growfarm.controller.PostController;
import jaeik.growfarm.dto.post.PostDTO;
import jaeik.growfarm.dto.post.PostReqDTO;
import jaeik.growfarm.dto.post.SimplePostDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.service.post.PostService;
import jaeik.growfarm.service.redis.RedisPostService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PostControllerTest {

    @Mock
    private PostService postService;

    @Mock
    private RedisPostService redisPostService;

    @InjectMocks
    private PostController postController;

    private CustomUserDetails userDetails;
    private PostDTO postDTO;
    private PostReqDTO postReqDTO;
    private List<SimplePostDTO> simplePostDTOList;

    @BeforeEach
    void setUp() {
        // Setup mock data
        userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUserId()).thenReturn(1L);

        // Mock PostDTO
        postDTO = mock(PostDTO.class);
        when(postDTO.getPostId()).thenReturn(1L);
        when(postDTO.getUserId()).thenReturn(1L);
        when(postDTO.getUserName()).thenReturn("testUser");
        when(postDTO.getTitle()).thenReturn("Test Title");
        when(postDTO.getContent()).thenReturn("Test Content");
        when(postDTO.getViews()).thenReturn(0);
        when(postDTO.getLikes()).thenReturn(0);
        when(postDTO.getCreatedAt()).thenReturn(Instant.now());

        // Mock PostReqDTO
        postReqDTO = mock(PostReqDTO.class);
        when(postReqDTO.getTitle()).thenReturn("Test Title");
        when(postReqDTO.getContent()).thenReturn("Test Content");

        // Mock SimplePostDTO
        SimplePostDTO simplePostDTO = mock(SimplePostDTO.class);
        when(simplePostDTO.getPostId()).thenReturn(1L);
        when(simplePostDTO.getUserId()).thenReturn(1L);
        when(simplePostDTO.getUserName()).thenReturn("testUser");
        when(simplePostDTO.getTitle()).thenReturn("Test Title");
        when(simplePostDTO.getCommentCount()).thenReturn(0);
        when(simplePostDTO.getLikes()).thenReturn(0);
        when(simplePostDTO.getViews()).thenReturn(0);
        when(simplePostDTO.getCreatedAt()).thenReturn(Instant.now());

        simplePostDTOList = new ArrayList<>();
        simplePostDTOList.add(simplePostDTO);
    }

    @Test
    @DisplayName("게시판 조회 테스트")
    void testGetBoard() {
        // Given
        Page<SimplePostDTO> postPage = new PageImpl<>(simplePostDTOList);
        when(postService.getBoard(anyInt(), anyInt())).thenReturn(postPage);

        // When
        ResponseEntity<Page<SimplePostDTO>> response = postController.getBoard(0, 10);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getContent().size());
        verify(postService, times(1)).getBoard(0, 10);
    }

    @Test
    @DisplayName("실시간 인기글 조회 테스트")
    void testGetRealtimeBoard() {
        // Given
        when(redisPostService.getCachedPopularPosts(RedisPostService.PopularPostType.REALTIME))
                .thenReturn(simplePostDTOList);

        // When
        ResponseEntity<List<SimplePostDTO>> response = postController.getRealtimeBoard();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(redisPostService, times(1)).getCachedPopularPosts(RedisPostService.PopularPostType.REALTIME);
    }

    @Test
    @DisplayName("주간 인기글 조회 테스트")
    void testGetWeeklyBoard() {
        // Given
        when(redisPostService.getCachedPopularPosts(RedisPostService.PopularPostType.WEEKLY))
                .thenReturn(simplePostDTOList);

        // When
        ResponseEntity<List<SimplePostDTO>> response = postController.getWeeklyBoard();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(redisPostService, times(1)).getCachedPopularPosts(RedisPostService.PopularPostType.WEEKLY);
    }

    @Test
    @DisplayName("레전드 인기글 조회 테스트")
    void testGetLegendBoard() {
        // Given
        when(redisPostService.getCachedPopularPosts(RedisPostService.PopularPostType.LEGEND))
                .thenReturn(simplePostDTOList);

        // When
        ResponseEntity<List<SimplePostDTO>> response = postController.getLegendBoard();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(redisPostService, times(1)).getCachedPopularPosts(RedisPostService.PopularPostType.LEGEND);
    }

    @Test
    @DisplayName("게시글 검색 테스트")
    void testSearchPost() {
        // Given
        Page<SimplePostDTO> postPage = new PageImpl<>(simplePostDTOList);
        when(postService.searchPost(anyString(), anyString(), anyInt(), anyInt())).thenReturn(postPage);

        // When
        ResponseEntity<Page<SimplePostDTO>> response = postController.searchPost("title", "test", 0, 10);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getContent().size());
        verify(postService, times(1)).searchPost("title", "test", 0, 10);
    }

    @Test
    @DisplayName("게시글 조회 테스트")
    void testGetPost() {
        // Given
        when(postService.getPost(anyLong(), any())).thenReturn(postDTO);
        doNothing().when(postService).incrementViewCount(anyLong(), any(), any());

        // When
        ResponseEntity<PostDTO> response = postController.getPost(1L, userDetails, mock(HttpServletRequest.class),
                mock(HttpServletResponse.class));

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(postService, times(1)).getPost(1L, userDetails);
        verify(postService, times(1)).incrementViewCount(eq(1L), any(), any());
    }

    @Test
    @DisplayName("게시글 작성 테스트")
    void testWritePost() {
        // Given
        when(postService.writePost(any(), any())).thenReturn(postDTO);

        // When
        ResponseEntity<PostDTO> response = postController.writePost(userDetails, postReqDTO);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(postService, times(1)).writePost(userDetails, postReqDTO);
    }

    @Test
    @DisplayName("게시글 수정 테스트")
    void testUpdatePost() {
        // Given
        doNothing().when(postService).updatePost(any(), any());

        // When
        ResponseEntity<String> response = postController.updatePost(userDetails, postDTO);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("게시글 수정 완료", response.getBody());
        verify(postService, times(1)).updatePost(userDetails, postDTO);
    }

    @Test
    @DisplayName("게시글 삭제 테스트")
    void testDeletePost() {
        // Given
        doNothing().when(postService).deletePost(any(), any());

        // When
        ResponseEntity<String> response = postController.deletePost(userDetails, postDTO);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("게시글 삭제 완료", response.getBody());
        verify(postService, times(1)).deletePost(userDetails, postDTO);
    }

    @Test
    @DisplayName("게시글 추천 테스트")
    void testLikePost() {
        // Given
        doNothing().when(postService).likePost(any(), any());

        // When
        ResponseEntity<String> response = postController.likePost(userDetails, postDTO);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("추천 처리 완료", response.getBody());
        verify(postService, times(1)).likePost(postDTO, userDetails);
    }
}
