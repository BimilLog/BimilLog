//package jaeik.growfarm.unit.service;
//
//import jaeik.growfarm.dto.post.PostDTO;
//import jaeik.growfarm.dto.post.PostReqDTO;
//import jaeik.growfarm.dto.post.SimplePostDTO;
//import jaeik.growfarm.entity.post.Post;
//import jaeik.growfarm.entity.post.PostLike;
//import jaeik.growfarm.entity.user.Users;
//import jaeik.growfarm.global.auth.CustomUserDetails;
//import jaeik.growfarm.repository.comment.CommentRepository;
//import jaeik.growfarm.repository.post.PostLikeRepository;
//import jaeik.growfarm.repository.post.PostRepository;
//import jaeik.growfarm.repository.user.UserRepository;
//import jaeik.growfarm.service.post.PostService;
//import jaeik.growfarm.service.post.PostUpdateService;
//import jaeik.growfarm.service.redis.RedisPostService;
//import jakarta.servlet.http.Cookie;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockedStatic;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.mockito.junit.jupiter.MockitoSettings;
//import org.mockito.quality.Strictness;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.Pageable;
//
//import java.time.Instant;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
///**
// * <h2>PostService 단위 테스트</h2>
// * <p>
// * 게시글 관련 서비스의 메서드들을 테스트합니다.
// * </p>
// * @version 1.0.0
// * @author Jaeik
// */
//@ExtendWith(MockitoExtension.class)
//@MockitoSettings(strictness = Strictness.LENIENT)
//public class PostServiceTest {
//
//    @Mock
//    private PostRepository postRepository;
//
//    @Mock
//    private CommentRepository commentRepository;
//
//    @Mock
//    private PostLikeRepository postLikeRepository;
//
//    @Mock
//    private UserRepository userRepository;
//
//    @Mock
//    private PostUpdateService postUpdateService;
//
//    @Mock
//    private RedisPostService redisPostService;
//
//    @Mock
//    private HttpServletRequest request;
//
//    @Mock
//    private HttpServletResponse response;
//
//    @InjectMocks
//    private PostService postService;
//
//    private CustomUserDetails userDetails;
//    private Post post;
//    private PostDTO postDTO;
//    private PostReqDTO postReqDTO;
//    private List<SimplePostDTO> simplePostDTOList;
//
//    @BeforeEach
//    void setUp() {
//        // Setup mock data
//        userDetails = mock(CustomUserDetails.class);
//        when(userDetails.getUserId()).thenReturn(1L);
//
//        Users user = mock(Users.class);
//        when(user.getId()).thenReturn(1L);
//
//        // Create Post
//        post = mock(Post.class);
//        when(post.getId()).thenReturn(1L);
//        when(post.getUser()).thenReturn(user);
//        when(post.getTitle()).thenReturn("Test Post Title");
//        when(post.getContent()).thenReturn("Test Post Content");
//        when(post.getCreatedAt()).thenReturn(Instant.now());
//
//        // Create PostDTO
//        postDTO = mock(PostDTO.class);
//        when(postDTO.getPostId()).thenReturn(1L);
//        when(postDTO.getUserId()).thenReturn(1L);
//        when(postDTO.getTitle()).thenReturn("Test Post Title");
//        when(postDTO.getContent()).thenReturn("Test Post Content");
//
//        // Create PostReqDTO
//        postReqDTO = mock(PostReqDTO.class);
//        when(postReqDTO.getTitle()).thenReturn("Test Post Title");
//        when(postReqDTO.getContent()).thenReturn("Test Post Content");
//
//        // Create SimplePostDTO
//        SimplePostDTO simplePostDTO = mock(SimplePostDTO.class);
//        when(simplePostDTO.getPostId()).thenReturn(1L);
//        when(simplePostDTO.getUserId()).thenReturn(1L);
//        when(simplePostDTO.getTitle()).thenReturn("Test Post Title");
//
//        simplePostDTOList = new ArrayList<>();
//        simplePostDTOList.add(simplePostDTO);
//
//        // Setup mock repositories
//        when(userRepository.getReferenceById(anyLong())).thenReturn(user);
//        when(postRepository.getReferenceById(anyLong())).thenReturn(post);
//        when(postRepository.findById(anyLong())).thenReturn(Optional.of(post));
//    }
//
//    @Test
//    @DisplayName("게시판 조회 테스트")
//    void testGetBoard() {
//        // Given
//        Page<SimplePostDTO> postPage = new PageImpl<>(simplePostDTOList);
//        when(postRepository.findPostsWithCommentAndLikeCounts(any(Pageable.class))).thenReturn(postPage);
//
//        // When
//        Page<SimplePostDTO> result = postService.getBoard(0, 10);
//
//        // Then
//        assertNotNull(result);
//        assertEquals(1, result.getContent().size());
//        assertEquals("Test Post Title", result.getContent().getFirst().getTitle());
//        verify(postRepository, times(1)).findPostsWithCommentAndLikeCounts(any(Pageable.class));
//    }
//
//    @Test
//    @DisplayName("게시글 조회 테스트")
//    void testGetPost() {
//        // Given
//        when(postRepository.findPostById(anyLong(), anyLong())).thenReturn(postDTO);
//
//        // When
//        PostDTO result = postService.getPost(1L, userDetails);
//
//        // Then
//        assertNotNull(result);
//        assertEquals("Test Post Title", result.getTitle());
//        verify(postRepository, times(1)).findPostById(eq(1L), eq(1L));
//    }
//
//    @Test
//    @DisplayName("게시글 검색 테스트")
//    void testSearchPost() {
//        // Given
//        Page<SimplePostDTO> postPage = new PageImpl<>(simplePostDTOList);
//        when(postRepository.searchPosts(anyString(), anyString(), any(Pageable.class))).thenReturn(postPage);
//
//        // When
//        Page<SimplePostDTO> result = postService.searchPost("title", "test", 0, 10);
//
//        // Then
//        assertNotNull(result);
//        assertEquals(1, result.getContent().size());
//        assertEquals("Test Post Title", result.getContent().getFirst().getTitle());
//        verify(postRepository, times(1)).searchPosts(eq("test"), eq("title"), any(Pageable.class));
//    }
//
//    @Test
//    @DisplayName("게시글 작성 테스트 - 회원")
//    void testWritePostByUser() {
//        // Given
//        when(postRepository.save(any(Post.class))).thenReturn(post);
//
//        PostDTO expectedPostDTO = mock(PostDTO.class);
//        when(expectedPostDTO.getPostId()).thenReturn(1L);
//        when(expectedPostDTO.getTitle()).thenReturn("Test Post Title");
//
//        try (MockedStatic<Post> mockedPost = mockStatic(Post.class);
//                MockedStatic<PostDTO> mockedPostDTO = mockStatic(PostDTO.class)) {
//
//            mockedPost.when(() -> Post.createPost(any(Users.class), any(PostReqDTO.class))).thenReturn(post);
//            mockedPostDTO.when(() -> PostDTO.newPost(any(Post.class))).thenReturn(expectedPostDTO);
//
//            // When
//            PostDTO result = postService.writePost(userDetails, postReqDTO);
//
//            // Then
//            assertNotNull(result);
//            verify(postRepository, times(1)).save(any(Post.class));
//            mockedPost.verify(() -> Post.createPost(any(Users.class), any(PostReqDTO.class)), times(1));
//        }
//    }
//
//    @Test
//    @DisplayName("게시글 작성 테스트 - 비회원")
//    void testWritePostByGuest() {
//        // Given
//        when(postReqDTO.getPassword()).thenReturn(1234); // 비회원은 비밀번호 설정
//        when(postRepository.save(any(Post.class))).thenReturn(post);
//
//        PostDTO expectedPostDTO = mock(PostDTO.class);
//        when(expectedPostDTO.getPostId()).thenReturn(1L);
//        when(expectedPostDTO.getTitle()).thenReturn("Test Post Title");
//
//        try (MockedStatic<Post> mockedPost = mockStatic(Post.class);
//                MockedStatic<PostDTO> mockedPostDTO = mockStatic(PostDTO.class)) {
//
//            mockedPost.when(() -> Post.createPost(any(), any(PostReqDTO.class))).thenReturn(post);
//            mockedPostDTO.when(() -> PostDTO.newPost(any(Post.class))).thenReturn(expectedPostDTO);
//
//            // When
//            PostDTO result = postService.writePost(null, postReqDTO); // userDetails가 null인 경우 (비회원)
//
//            // Then
//            assertNotNull(result);
//            verify(postRepository, times(1)).save(any(Post.class));
//            mockedPost.verify(() -> Post.createPost(eq(null), any(PostReqDTO.class)), times(1));
//        }
//    }
//
//    @Test
//    @DisplayName("게시글 수정 테스트 - 회원")
//    void testUpdatePostByUser() {
//        // Given
//        when(postDTO.getUserId()).thenReturn(1L);
//
//        // When
//        postService.updatePost(userDetails, postDTO);
//
//        // Then
//        verify(post, times(1)).updatePost(any(PostDTO.class));
//    }
//
//    @Test
//    @DisplayName("게시글 수정 테스트 - 비회원 (비밀번호 확인)")
//    void testUpdatePostByGuest() {
//        // Given
//        when(postDTO.getUserId()).thenReturn(null); // 비회원 게시글
//        when(postDTO.getPassword()).thenReturn(1234);
//        when(post.getPassword()).thenReturn(1234); // 동일한 비밀번호
//
//        // When
//        postService.updatePost(null, postDTO); // userDetails가 null (비회원)
//
//        // Then
//        verify(post, times(1)).updatePost(any(PostDTO.class));
//    }
//
//    @Test
//    @DisplayName("게시글 삭제 테스트 - 회원")
//    void testDeletePostByUser() {
//        // Given
//        when(postDTO.getUserId()).thenReturn(1L);
//        when(commentRepository.findCommentIdsByPostId(anyLong())).thenReturn(List.of());
//        doNothing().when(postUpdateService).postDelete(any(Post.class), anyList());
//
//        // When
//        postService.deletePost(userDetails, postDTO);
//
//        // Then
//        verify(postUpdateService, times(1)).postDelete(any(Post.class), anyList());
//    }
//
//    @Test
//    @DisplayName("게시글 삭제 테스트 - 비회원 (비밀번호 확인)")
//    void testDeletePostByGuest() {
//        // Given
//        when(postDTO.getUserId()).thenReturn(null); // 비회원 게시글
//        when(postDTO.getPassword()).thenReturn(1234);
//        when(post.getPassword()).thenReturn(1234); // 동일한 비밀번호
//        when(commentRepository.findCommentIdsByPostId(anyLong())).thenReturn(List.of());
//        doNothing().when(postUpdateService).postDelete(any(Post.class), anyList());
//
//        // When
//        postService.deletePost(null, postDTO); // userDetails가 null (비회원)
//
//        // Then
//        verify(postUpdateService, times(1)).postDelete(any(Post.class), anyList());
//    }
//
//    @Test
//    @DisplayName("게시글 추천 테스트 - 새로운 추천")
//    void testLikePostNew() {
//        // Given
//        when(postLikeRepository.findByPostIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.empty());
//        doNothing().when(postUpdateService).savePostLike(any(), any(), any());
//
//        // When
//        postService.likePost(postDTO, userDetails);
//
//        // Then
//        verify(postUpdateService, times(1)).savePostLike(any(), any(Post.class), any(Users.class));
//    }
//
//    @Test
//    @DisplayName("게시글 추천 테스트 - 기존 추천 취소")
//    void testLikePostExisting() {
//        // Given
//        PostLike postLike = mock(PostLike.class);
//        when(postLikeRepository.findByPostIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(postLike));
//        doNothing().when(postUpdateService).savePostLike(any(), any(), any());
//
//        // When
//        postService.likePost(postDTO, userDetails);
//
//        // Then
//        verify(postUpdateService, times(1)).savePostLike(any(), any(Post.class), any(Users.class));
//    }
//
//    @Test
//    @DisplayName("게시글 조회수 증가 테스트 - 처음 조회")
//    void testIncrementViewCountFirstView() {
//        // Given
//        when(postRepository.existsById(anyLong())).thenReturn(true);
//        when(request.getCookies()).thenReturn(null);
//        doNothing().when(postUpdateService).updateViewCount(anyLong());
//
//        // When
//        postService.incrementViewCount(1L, request, response);
//
//        // Then
//        verify(postUpdateService, times(1)).updateViewCount(anyLong());
//    }
//
//    @Test
//    @DisplayName("게시글 조회수 증가 테스트 - 이미 조회함")
//    void testIncrementViewCountAlreadyViewed() {
//        // Given
//        when(postRepository.existsById(anyLong())).thenReturn(true);
//
//        // Create a cookie with encoded value that would decode to a list containing the
//        // post ID
//        Cookie cookie = mock(Cookie.class);
//        when(cookie.getName()).thenReturn("post_views");
//        when(cookie.getValue()).thenReturn("WzFd"); // Base64 encoded value for "[1]"
//        when(request.getCookies()).thenReturn(new Cookie[] { cookie });
//
//        // When
//        postService.incrementViewCount(1L, request, response);
//
//        // Then
//        verify(postUpdateService, never()).updateViewCount(anyLong());
//    }
//
//    @Test
//    @DisplayName("실시간 인기글 업데이트 테스트")
//    void testUpdateRealtimePopularPosts() {
//        // Given
//        when(postRepository.updateRealtimePopularPosts()).thenReturn(simplePostDTOList);
//        doNothing().when(redisPostService).cachePopularPosts(any(), anyList());
//
//        // When
//        postService.updateRealtimePopularPosts();
//
//        // Then
//        verify(postRepository, times(1)).updateRealtimePopularPosts();
//        verify(redisPostService, times(1)).cachePopularPosts(any(RedisPostService.PopularPostType.class),
//                anyList());
//    }
//}
