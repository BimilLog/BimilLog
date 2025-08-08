package jaeik.growfarm.unit.service.post;

import jaeik.growfarm.dto.post.PostDTO;
import jaeik.growfarm.dto.post.PostReqDTO;
import jaeik.growfarm.entity.post.Post;
import jaeik.growfarm.entity.post.PostLike;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.comment.CommentRepository;
import jaeik.growfarm.repository.post.PostLikeRepository;
import jaeik.growfarm.repository.post.PostRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jaeik.growfarm.service.post.command.PostCommandServiceImpl;
import jaeik.growfarm.service.post.PostInteractionService;
import jaeik.growfarm.service.post.PostPersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <h2>PostCommandService 단위 테스트</h2>
 * <p>
 * 게시글 CUD(생성, 수정, 삭제) 및 추천 관련 서비스의 메서드들을 테스트합니다.
 * </p>
 * @version 1.0.0
 * @author Jaeik
 */
@ExtendWith(MockitoExtension.class)
public class PostCommandServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostPersistenceService postPersistenceService;

    @Mock
    private PostInteractionService postInteractionService;

    @InjectMocks
    private PostCommandServiceImpl postCommandService;

    private CustomUserDetails userDetails;
    private Users user;
    private Post post;
    private PostDTO postDTO;
    private PostReqDTO postReqDTO;

    @BeforeEach
    void setUp() {
        userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUserId()).thenReturn(1L);

        user = mock(Users.class);
        when(user.getId()).thenReturn(1L);

        post = mock(Post.class);
        when(post.getId()).thenReturn(1L);
        when(post.getUser()).thenReturn(user);
        when(post.getTitle()).thenReturn("Test Post Title");
        when(post.getContent()).thenReturn("Test Post Content");
        when(post.getCreatedAt()).thenReturn(Instant.now());
        when(post.getPassword()).thenReturn(1234);

        postDTO = PostDTO.existedPost(
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

        postReqDTO = new PostReqDTO();
        postReqDTO.setTitle("Test Post Title");
        postReqDTO.setContent("Test Post Content");
    }

    @Test
    @DisplayName("게시글 작성 테스트 - 회원")
    void testWritePostByUser() {
        // Given
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(postRepository.save(any(Post.class))).thenReturn(post);

        try (MockedStatic<Post> mockedPost = mockStatic(Post.class);
             MockedStatic<PostDTO> mockedPostDTO = mockStatic(PostDTO.class)) {

            mockedPost.when(() -> Post.createPost(user, postReqDTO)).thenReturn(post);
            mockedPostDTO.when(() -> PostDTO.newPost(post)).thenReturn(postDTO);

            // When
            PostDTO result = postCommandService.writePost(userDetails, postReqDTO);

            // Then
            assertNotNull(result);
            assertEquals("Test Post Title", result.getTitle());
            verify(postRepository, times(1)).save(any(Post.class));
            verify(userRepository, times(1)).getReferenceById(1L);
        }
    }

    @Test
    @DisplayName("게시글 작성 테스트 - 비회원")
    void testWritePostByGuest() {
        // Given
        postReqDTO.setPassword(1234);
        when(postRepository.save(any(Post.class))).thenReturn(post);

        try (MockedStatic<Post> mockedPost = mockStatic(Post.class);
             MockedStatic<PostDTO> mockedPostDTO = mockStatic(PostDTO.class)) {

            mockedPost.when(() -> Post.createPost(null, postReqDTO)).thenReturn(post);
            mockedPostDTO.when(() -> PostDTO.newPost(post)).thenReturn(postDTO);

            // When
            PostDTO result = postCommandService.writePost(null, postReqDTO);

            // Then
            assertNotNull(result);
            assertEquals("Test Post Title", result.getTitle());
            verify(postRepository, times(1)).save(any(Post.class));
            verify(userRepository, never()).getReferenceById(anyLong());
        }
    }

    @Test
    @DisplayName("게시글 수정 테스트 - 회원")
    void testUpdatePostByUser() {
        // Given
        when(postRepository.getReferenceById(1L)).thenReturn(post);
        when(post.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(1L);

        // When
        postCommandService.updatePost(userDetails, postDTO);

        // Then
        verify(postRepository, times(1)).getReferenceById(1L);
        verify(postPersistenceService, times(1)).updatePost(postDTO, post);
    }

    @Test
    @DisplayName("게시글 수정 테스트 - 비회원 (올바른 비밀번호)")
    void testUpdatePostByGuestCorrectPassword() {
        // Given
        PostDTO guestPostDTO = PostDTO.existedPost(
                1L,
                null, 
                "익명",
                "Updated Title",
                "Updated Content",
                0,
                0,
                false,
                null,
                Instant.now(),
                false);
        guestPostDTO.setPassword(1234);

        when(postRepository.getReferenceById(1L)).thenReturn(post);
        when(post.getUser()).thenReturn(null);
        when(post.getPassword()).thenReturn(1234);

        // When
        postCommandService.updatePost(null, guestPostDTO);

        // Then
        verify(postRepository, times(1)).getReferenceById(1L);
        verify(postPersistenceService, times(1)).updatePost(guestPostDTO, post);
    }

    @Test
    @DisplayName("게시글 수정 테스트 - 비회원 (잘못된 비밀번호)")
    void testUpdatePostByGuestIncorrectPassword() {
        // Given
        PostDTO guestPostDTO = PostDTO.existedPost(
                1L,
                null,
                "익명", 
                "Title",
                "Content",
                0,
                0,
                false,
                null,
                Instant.now(),
                false);
        guestPostDTO.setPassword(9999);

        when(postRepository.getReferenceById(1L)).thenReturn(post);
        when(post.getUser()).thenReturn(null);
        when(post.getPassword()).thenReturn(1234);

        // When & Then
        CustomException exception = assertThrows(CustomException.class,
                () -> postCommandService.updatePost(null, guestPostDTO));

        assertEquals(ErrorCode.POST_PASSWORD_NOT_MATCH, exception.getErrorCode());
        verify(postPersistenceService, never()).updatePost(any(), any());
    }

    @Test
    @DisplayName("게시글 수정 테스트 - 권한 없음")
    void testUpdatePostUnauthorized() {
        // Given
        when(postRepository.getReferenceById(1L)).thenReturn(post);
        when(post.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(2L); // 다른 사용자 ID

        // When & Then
        CustomException exception = assertThrows(CustomException.class,
                () -> postCommandService.updatePost(userDetails, postDTO));

        assertEquals(ErrorCode.POST_UPDATE_FORBIDDEN, exception.getErrorCode());
        verify(postPersistenceService, never()).updatePost(any(), any());
    }

    @Test
    @DisplayName("게시글 삭제 테스트 - 회원")
    void testDeletePostByUser() {
        // Given
        when(postRepository.getReferenceById(1L)).thenReturn(post);
        when(post.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(1L);
        when(commentRepository.findCommentIdsByPostId(1L)).thenReturn(List.of());

        // When
        postCommandService.deletePost(userDetails, postDTO);

        // Then
        verify(postRepository, times(1)).getReferenceById(1L);
        verify(postPersistenceService, times(1)).deletePost(1L);
    }

    @Test
    @DisplayName("게시글 삭제 테스트 - 비회원 (올바른 비밀번호)")
    void testDeletePostByGuestCorrectPassword() {
        // Given
        PostDTO guestPostDTO = PostDTO.existedPost(
                1L,
                null,
                "익명",
                "Title", 
                "Content",
                0,
                0,
                false,
                null,
                Instant.now(),
                false);
        guestPostDTO.setPassword(1234);

        when(postRepository.getReferenceById(1L)).thenReturn(post);
        when(post.getUser()).thenReturn(null);
        when(post.getPassword()).thenReturn(1234);
        when(commentRepository.findCommentIdsByPostId(1L)).thenReturn(List.of());

        // When
        postCommandService.deletePost(null, guestPostDTO);

        // Then
        verify(postRepository, times(1)).getReferenceById(1L);
        verify(postPersistenceService, times(1)).deletePost(1L);
    }

    @Test
    @DisplayName("게시글 추천 테스트 - 새로운 추천")
    void testLikePostNew() {
        // Given
        when(postRepository.getReferenceById(1L)).thenReturn(post);
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(postLikeRepository.findByPostIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

        // When
        postCommandService.likePost(postDTO, userDetails);

        // Then
        verify(postRepository, times(1)).getReferenceById(1L);
        verify(userRepository, times(1)).getReferenceById(1L);
        verify(postInteractionService, times(1)).toggleLike(Optional.empty(), post, user);
    }

    @Test
    @DisplayName("게시글 추천 테스트 - 기존 추천 취소")
    void testLikePostExisting() {
        // Given
        PostLike existingLike = mock(PostLike.class);
        when(postRepository.getReferenceById(1L)).thenReturn(post);
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(postLikeRepository.findByPostIdAndUserId(1L, 1L)).thenReturn(Optional.of(existingLike));

        // When
        postCommandService.likePost(postDTO, userDetails);

        // Then
        verify(postRepository, times(1)).getReferenceById(1L);
        verify(userRepository, times(1)).getReferenceById(1L);
        verify(postInteractionService, times(1)).toggleLike(Optional.of(existingLike), post, user);
    }

    @Test
    @DisplayName("게시글 추천 테스트 - 비로그인 사용자")
    void testLikePostGuestUser() {
        // When & Then
        CustomException exception = assertThrows(CustomException.class,
                () -> postCommandService.likePost(postDTO, null));

        assertEquals(ErrorCode.POST_UPDATE_FORBIDDEN, exception.getErrorCode());
        verify(postInteractionService, never()).toggleLike(any(), any(), any());
    }
}