package jaeik.growfarm.unit.service;

import com.querydsl.core.Tuple;
import jaeik.growfarm.dto.comment.CommentDTO;
import jaeik.growfarm.entity.comment.Comment;
import jaeik.growfarm.entity.comment.QComment;
import jaeik.growfarm.entity.comment.QCommentClosure;
import jaeik.growfarm.entity.comment.QCommentLike;
import jaeik.growfarm.entity.post.Post;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.event.CommentCreatedEvent;
import jaeik.growfarm.repository.comment.CommentClosureRepository;
import jaeik.growfarm.repository.comment.CommentRepository;
import jaeik.growfarm.repository.post.PostRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jaeik.growfarm.service.comment.CommentService;
import jaeik.growfarm.service.comment.CommentUpdateService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <h2>CommentService 단위 테스트</h2>
 * <p>
 * 댓글 서비스의 비즈니스 로직을 테스트합니다.
 * </p>
 * @version 1.0.0
 * @author Jaeik
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private CommentClosureRepository commentClosureRepository;

    @Mock
    private CommentUpdateService commentUpdateService;

    @InjectMocks
    private CommentService commentService;

    private CustomUserDetails userDetails;
    private CommentDTO commentDTO;
    private Comment comment;
    private Tuple mockTuple;

    @BeforeEach
    void setUp() {
        // Setup mock data
        userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUserId()).thenReturn(1L);
        when(userDetails.getClientDTO()).thenReturn(mock(jaeik.growfarm.dto.user.ClientDTO.class));
        when(userDetails.getClientDTO().getUserId()).thenReturn(1L);

        // Create CommentDTO
        commentDTO = mock(CommentDTO.class);
        when(commentDTO.getId()).thenReturn(1L);
        when(commentDTO.getPostId()).thenReturn(1L);
        when(commentDTO.getContent()).thenReturn("Test comment content");
        when(commentDTO.getUserName()).thenReturn("testUser");
        when(commentDTO.getPassword()).thenReturn(null);
        when(commentDTO.getParentId()).thenReturn(null);

        // Create Post
        Post post = mock(Post.class);
        when(post.getId()).thenReturn(1L);

        // Create User
        Users user = mock(Users.class);
        when(user.getId()).thenReturn(1L);
        when(user.getUserName()).thenReturn("testUser");

        // Create Comment with proper mocking
        comment = mock(Comment.class);
        when(comment.getId()).thenReturn(1L);
        when(comment.getContent()).thenReturn("Test comment content");
        when(comment.getUser()).thenReturn(user);
        when(comment.getPost()).thenReturn(post);
        when(comment.getCreatedAt()).thenReturn(Instant.now());
        when(comment.getPassword()).thenReturn(null);
        when(comment.isDeleted()).thenReturn(false);

        // Setup Post with User
        when(post.getUser()).thenReturn(user);

        // Setup mock repositories
        when(postRepository.getReferenceById(anyLong())).thenReturn(post);
        when(userRepository.getReferenceById(anyLong())).thenReturn(user);
        when(commentRepository.getReferenceById(anyLong())).thenReturn(comment);

        // Setup mock tuple
        mockTuple = mock(Tuple.class);
        when(mockTuple.get(QComment.comment)).thenReturn(comment);
        when(mockTuple.get(QCommentLike.commentLike.count().coalesce(0L))).thenReturn(5L);
        when(mockTuple.get(QCommentClosure.commentClosure.depth)).thenReturn(0);
    }

    @Test
    @DisplayName("인기 댓글 조회 테스트")
    void testGetPopularComments() {
        // Given
        List<Tuple> popularTuples = new ArrayList<>();
        popularTuples.add(mockTuple);
        when(commentRepository.findPopularComments(anyLong())).thenReturn(popularTuples);
        when(commentRepository.findUserLikedCommentIds(anyList(), anyLong())).thenReturn(List.of());

        // When
        List<CommentDTO> result = commentService.getPopularComments(1L, userDetails);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.getFirst().isPopular());
    }

    @Test
    @DisplayName("댓글 최신순 조회 테스트")
    void testGetCommentsLatestOrder() {
        // Given
        List<Tuple> commentTuples = new ArrayList<>();
        commentTuples.add(mockTuple);
        when(commentRepository.findCommentsWithLatestOrder(anyLong(), any(Pageable.class))).thenReturn(commentTuples);
        when(commentRepository.findUserLikedCommentIds(anyList(), anyLong())).thenReturn(List.of());
        when(commentRepository.countRootCommentsByPostId(anyLong())).thenReturn(1L);
        when(mockTuple.get(QCommentClosure.commentClosure.depth)).thenReturn(0);

        // When
        Page<CommentDTO> result = commentService.getCommentsLatestOrder(1L, 0, userDetails);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("댓글 작성 테스트 - 회원")
    void testWriteCommentByUser() {
        // Given
        doNothing().when(commentUpdateService).saveCommentWithClosure(any(), any(), anyString(), any(Integer.class),
                any(Long.class));
        doNothing().when(eventPublisher).publishEvent(any(CommentCreatedEvent.class));

        // When
        commentService.writeComment(userDetails, commentDTO);

        // Then
        verify(commentUpdateService, times(1)).saveCommentWithClosure(
                any(Post.class),
                any(Users.class),
                anyString(),
                any(),
                any());
        verify(eventPublisher, times(1)).publishEvent(any(CommentCreatedEvent.class));
    }

    @Test
    @DisplayName("댓글 작성 테스트 - 비회원")
    void testWriteCommentByGuest() {
        // Given
        when(commentDTO.getPassword()).thenReturn(1234); // 비회원은 비밀번호 설정
        doNothing().when(commentUpdateService).saveCommentWithClosure(any(), any(), anyString(), any(Integer.class),
                any(Long.class));
        doNothing().when(eventPublisher).publishEvent(any(CommentCreatedEvent.class));

        // When
        commentService.writeComment(null, commentDTO); // userDetails가 null인 경우 (비회원)

        // Then
        verify(commentUpdateService, times(1)).saveCommentWithClosure(
                any(Post.class),
                any(), // 비회원이므로 user는 null
                anyString(),
                any(),
                any());
        verify(eventPublisher, times(1)).publishEvent(any(CommentCreatedEvent.class));
    }

    @Test
    @DisplayName("댓글 수정 테스트 - 회원")
    void testUpdateCommentByUser() {
        // Given
        when(comment.getPassword()).thenReturn(null); // 회원 댓글은 비밀번호가 null

        // When
        commentService.updateComment(commentDTO, userDetails);

        // Then
        verify(comment, times(1)).updateComment(anyString());
    }

    @Test
    @DisplayName("댓글 수정 테스트 - 비회원 (비밀번호 일치)")
    void testUpdateCommentByGuestValidPassword() {
        // Given
        when(commentDTO.getPassword()).thenReturn(1234);
        when(comment.getPassword()).thenReturn(1234); // 동일한 비밀번호

        // When
        commentService.updateComment(commentDTO, userDetails);

        // Then
        verify(comment, times(1)).updateComment(anyString());
    }

    @Test
    @DisplayName("댓글 삭제 테스트 - 회원")
    void testDeleteCommentByUser() {
        // Given
        when(comment.getPassword()).thenReturn(null); // 회원 댓글
        when(commentClosureRepository.hasDescendants(anyLong())).thenReturn(false);
        doNothing().when(commentUpdateService).commentDelete(anyBoolean(), anyLong(), any(Comment.class));

        // When
        commentService.deleteComment(commentDTO, userDetails);

        // Then
        verify(comment, never()).softDelete();
        verify(commentUpdateService, times(1)).commentDelete(eq(false), anyLong(), any(Comment.class));
    }

    @Test
    @DisplayName("댓글 삭제 테스트 - 비회원 (비밀번호 일치)")
    void testDeleteCommentByGuestValidPassword() {
        // Given
        when(commentDTO.getPassword()).thenReturn(1234);
        when(comment.getPassword()).thenReturn(1234); // 동일한 비밀번호
        when(commentClosureRepository.hasDescendants(anyLong())).thenReturn(false);
        doNothing().when(commentUpdateService).commentDelete(anyBoolean(), anyLong(), any(Comment.class));

        // When
        commentService.deleteComment(commentDTO, userDetails);

        // Then
        verify(comment, never()).softDelete();
        verify(commentUpdateService, times(1)).commentDelete(eq(false), anyLong(), any(Comment.class));
    }
}
