package jaeik.growfarm.unit.service.post;

import jaeik.growfarm.dto.post.FullPostResDTO;
import jaeik.growfarm.dto.post.SimplePostResDTO;
import jaeik.growfarm.entity.post.Post;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.post.PostRepository;
import jaeik.growfarm.repository.post.read.PostReadRepository;
import jaeik.growfarm.service.post.PostInteractionService;
import jaeik.growfarm.service.post.read.PostReadServiceImpl;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <h2>PostQueryService 단위 테스트</h2>
 * <p>
 * 게시글 조회 관련 서비스의 메서드들을 테스트합니다.
 * </p>
 * @version 1.0.0
 * @author Jaeik
 */
@ExtendWith(MockitoExtension.class)
public class PostReadServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostReadRepository postReadRepository;

    @Mock
    private PostInteractionService postInteractionService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private PostReadServiceImpl postQueryService;

    private CustomUserDetails userDetails;
    private Post post;
    private FullPostResDTO fullPostResDTO;

    @BeforeEach
    void setUp() {
        userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUserId()).thenReturn(1L);

        Users user = mock(Users.class);
        when(user.getId()).thenReturn(1L);

        post = mock(Post.class);
        when(post.getId()).thenReturn(1L);
        when(post.getUser()).thenReturn(user);
        when(post.getTitle()).thenReturn("Test Post Title");
        when(post.getContent()).thenReturn("Test Post Content");
        when(post.getCreatedAt()).thenReturn(Instant.now());

        fullPostResDTO = FullPostResDTO.existedPost(
                1L,
                1L,
                "testUser",
                "Test Post Title",
                "Test Post Content",
                0,
                0,
                false,
                null,
                Instant.now(),
                false);
    }

    @Test
    @DisplayName("게시글 상세 조회 테스트 - 로그인 사용자")
    void testGetPostWithUser() {
        // Given
        when(postReadRepository.findPostById(1L, 1L)).thenReturn(fullPostResDTO);

        // When
        FullPostResDTO result = postQueryService.getPost(1L, userDetails);

        // Then
        assertNotNull(result);
        assertEquals("Test Post Title", result.getTitle());
        assertEquals("Test Post Content", result.getContent());
        verify(postReadRepository, times(1)).findPostById(1L, 1L);
    }

    @Test
    @DisplayName("게시글 상세 조회 테스트 - 비로그인 사용자")
    void testGetPostWithoutUser() {
        // Given
        when(postReadRepository.findPostById(1L, null)).thenReturn(fullPostResDTO);

        // When
        FullPostResDTO result = postQueryService.getPost(1L, null);

        // Then
        assertNotNull(result);
        assertEquals("Test Post Title", result.getTitle());
        verify(postReadRepository, times(1)).findPostById(1L, null);
    }

    @Test
    @DisplayName("게시글 상세 조회 테스트 - 존재하지 않는 게시글")
    void testGetPostNotFound() {
        // Given
        when(postReadRepository.findPostById(999L, 1L)).thenReturn(null);

        // When & Then
        assertDoesNotThrow(() -> {
            FullPostResDTO result = postQueryService.getPost(999L, userDetails);
            assertNull(result);
        });
        
        verify(postReadRepository, times(1)).findPostById(999L, 1L);
    }

    @Test
    @DisplayName("조회수 증가 테스트 - 처음 조회")
    void testIncrementViewCountFirstView() {
        // Given
        when(postRepository.existsById(1L)).thenReturn(true);
        when(request.getCookies()).thenReturn(null);

        // When
        postQueryService.incrementViewCount(1L, request, response);

        // Then
        verify(postInteractionService, times(1)).incrementViewCount(1L);
        verify(response, times(1)).addCookie(any(Cookie.class));
    }

    @Test
    @DisplayName("조회수 증가 테스트 - 이미 조회함")
    void testIncrementViewCountAlreadyViewed() {
        // Given
        when(postRepository.existsById(1L)).thenReturn(true);
        
        Cookie cookie = new Cookie("post_views", "WzFd"); // Base64 encoded "[1]"
        when(request.getCookies()).thenReturn(new Cookie[]{cookie});

        // When
        postQueryService.incrementViewCount(1L, request, response);

        // Then
        verify(postInteractionService, never()).incrementViewCount(1L);
        verify(response, never()).addCookie(any(Cookie.class));
    }

    @Test
    @DisplayName("조회수 증가 테스트 - 존재하지 않는 게시글")
    void testIncrementViewCountPostNotFound() {
        // Given
        when(postRepository.existsById(999L)).thenReturn(false);

        // When & Then
        CustomException exception = assertThrows(CustomException.class,
            () -> postQueryService.incrementViewCount(999L, request, response));
        
        assertEquals(ErrorCode.POST_NOT_FOUND, exception.getErrorCode());
        verify(postInteractionService, never()).incrementViewCount(anyLong());
    }

    @Test
    @DisplayName("게시판 목록 조회 테스트")
    void testGetBoard() {
        // Given
        SimplePostResDTO mockSimplePost = SimplePostResDTO.builder()
                .postId(1L)
                .userId(1L)
                .userName("testUser")
                .title("Test Title")
                .commentCount(0)
                .likes(0)
                .views(0)
                .createdAt(Instant.now())
                .isNotice(false)
                .build();
        
        Page<SimplePostResDTO> mockPage = new PageImpl<>(List.of(mockSimplePost));
        when(postReadRepository.findSimplePost(any(Pageable.class))).thenReturn(mockPage);

        // When
        Page<SimplePostResDTO> result = postQueryService.getBoard(0, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(postReadRepository, times(1)).findSimplePost(any(Pageable.class));
    }
}