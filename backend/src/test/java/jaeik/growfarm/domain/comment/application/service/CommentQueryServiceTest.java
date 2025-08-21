package jaeik.growfarm.domain.comment.application.service;

import jaeik.growfarm.domain.comment.application.port.out.CommentQueryPort;
import jaeik.growfarm.domain.comment.entity.Comment;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.infrastructure.adapter.comment.in.web.dto.CommentDTO;
import jaeik.growfarm.infrastructure.adapter.comment.in.web.dto.SimpleCommentDTO;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>CommentQueryService 단위 테스트</h2>
 * <p>댓글 조회 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>모든 외부 의존성을 모킹하여 순수한 비즈니스 로직만 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CommentQueryService 단위 테스트")
class CommentQueryServiceTest {

    @Mock
    private CommentQueryPort commentQueryPort;

    @Mock
    private CustomUserDetails userDetails;

    @InjectMocks
    private CommentQueryService commentQueryService;

    private Comment testComment;
    private CommentDTO commentDTO;
    private SimpleCommentDTO simpleCommentDTO;

    @BeforeEach
    void setUp() {
        User testUser = User.builder()
                .id(100L)
                .userName("testUser")
                .socialId("kakao123")
                .build();

        testComment = Comment.builder()
                .id(200L)
                .content("테스트 댓글")
                .user(testUser)
                .deleted(false)
                .build();

        commentDTO = new CommentDTO();
        commentDTO.setId(200L);
        commentDTO.setContent("테스트 댓글");
        commentDTO.setUserName("testUser");
        commentDTO.setLikes(5);
        commentDTO.setUserLike(false);

        simpleCommentDTO = new SimpleCommentDTO(
                200L, 
                300L, 
                "testUser", 
                "테스트 댓글",
                Instant.now(),
                5, 
                false
        );
    }

    @Test
    @DisplayName("인기 댓글 조회 성공 - 로그인 사용자")
    void shouldGetPopularComments_WhenLoggedInUser() {
        // Given
        Long postId = 300L;
        List<CommentDTO> popularComments = Collections.singletonList(commentDTO);
        List<Long> popularCommentIds = List.of(200L);
        List<Long> likedCommentIds = List.of(200L);

        given(userDetails.getUserId()).willReturn(100L);
        // 첫 번째 호출: 좋아요 ID 조회를 위해 빈 리스트로 호출
        given(commentQueryPort.findPopularComments(postId, Collections.emptyList())).willReturn(popularComments);
        given(commentQueryPort.findUserLikedCommentIds(popularCommentIds, 100L)).willReturn(likedCommentIds);
        // 두 번째 호출: 실제 결과 반환을 위해 좋아요 ID와 함께 호출
        given(commentQueryPort.findPopularComments(postId, likedCommentIds)).willReturn(popularComments);

        // When
        List<CommentDTO> result = commentQueryService.getPopularComments(postId, userDetails);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(200L);
        assertThat(result.getFirst().getContent()).isEqualTo("테스트 댓글");

        verify(commentQueryPort).findPopularComments(postId, Collections.emptyList());
        verify(commentQueryPort).findUserLikedCommentIds(popularCommentIds, 100L);
        verify(commentQueryPort).findPopularComments(postId, likedCommentIds);
    }

    @Test
    @DisplayName("인기 댓글 조회 성공 - 익명 사용자")
    void shouldGetPopularComments_WhenAnonymousUser() {
        // Given
        Long postId = 300L;
        List<CommentDTO> popularComments = Collections.singletonList(commentDTO);

        given(commentQueryPort.findPopularComments(postId, Collections.emptyList())).willReturn(popularComments);

        // When
        List<CommentDTO> result = commentQueryService.getPopularComments(postId, null);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(200L);

        verify(commentQueryPort).findPopularComments(postId, Collections.emptyList());
        verify(commentQueryPort, never()).findUserLikedCommentIds(anyList(), anyLong());
    }

    @Test
    @DisplayName("인기 댓글 조회 - 댓글이 없는 경우")
    void shouldReturnEmptyList_WhenNoPopularComments() {
        // Given
        Long postId = 300L;
        List<CommentDTO> emptyComments = Collections.emptyList();

        given(commentQueryPort.findPopularComments(postId, Collections.emptyList())).willReturn(emptyComments);

        // When
        List<CommentDTO> result = commentQueryService.getPopularComments(postId, userDetails);

        // Then
        assertThat(result).isEmpty();

        verify(commentQueryPort, times(2)).findPopularComments(postId, Collections.emptyList());
        verify(commentQueryPort, never()).findUserLikedCommentIds(anyList(), anyLong());
    }

    @Test
    @DisplayName("과거순 댓글 조회 성공 - 로그인 사용자")
    void shouldGetCommentsOldestOrder_WhenLoggedInUser() {
        // Given
        Long postId = 300L;
        int page = 0;
        List<CommentDTO> comments = Collections.singletonList(commentDTO);
        Page<CommentDTO> commentPage = new PageImpl<>(comments);
        List<Long> pageCommentIds = List.of(200L);
        List<Long> likedCommentIds = List.of(200L);

        given(userDetails.getUserId()).willReturn(100L);
        given(commentQueryPort.findCommentsWithOldestOrder(eq(postId), any(Pageable.class), eq(Collections.emptyList())))
                .willReturn(commentPage);
        given(commentQueryPort.findUserLikedCommentIds(pageCommentIds, 100L)).willReturn(likedCommentIds);
        given(commentQueryPort.findCommentsWithOldestOrder(eq(postId), any(Pageable.class), eq(likedCommentIds)))
                .willReturn(commentPage);

        // When
        Page<CommentDTO> result = commentQueryService.getCommentsOldestOrder(postId, page, userDetails);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getId()).isEqualTo(200L);
    }

    @Test
    @DisplayName("과거순 댓글 조회 성공 - 익명 사용자")
    void shouldGetCommentsOldestOrder_WhenAnonymousUser() {
        // Given
        Long postId = 300L;
        int page = 0;
        List<CommentDTO> comments = Collections.singletonList(commentDTO);
        Page<CommentDTO> commentPage = new PageImpl<>(comments);

        given(commentQueryPort.findCommentsWithOldestOrder(eq(postId), any(Pageable.class), eq(Collections.emptyList())))
                .willReturn(commentPage);

        // When
        Page<CommentDTO> result = commentQueryService.getCommentsOldestOrder(postId, page, null);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getId()).isEqualTo(200L);

        verify(commentQueryPort).findCommentsWithOldestOrder(eq(postId), any(Pageable.class), eq(Collections.emptyList()));
        verify(commentQueryPort, never()).findUserLikedCommentIds(anyList(), anyLong());
    }

    @Test
    @DisplayName("과거순 댓글 조회 - 빈 페이지")
    void shouldReturnEmptyPage_WhenNoCommentsInPage() {
        // Given
        Long postId = 300L;
        int page = 0;
        Page<CommentDTO> emptyPage = new PageImpl<>(Collections.emptyList());

        given(commentQueryPort.findCommentsWithOldestOrder(eq(postId), any(Pageable.class), eq(Collections.emptyList())))
                .willReturn(emptyPage);

        // When
        Page<CommentDTO> result = commentQueryService.getCommentsOldestOrder(postId, page, userDetails);

        // Then
        assertThat(result.getContent()).isEmpty();

        verify(commentQueryPort, times(2)).findCommentsWithOldestOrder(eq(postId), any(Pageable.class), eq(Collections.emptyList()));
        verify(commentQueryPort, never()).findUserLikedCommentIds(anyList(), anyLong());
    }

    @Test
    @DisplayName("ID로 댓글 조회 성공")
    void shouldFindById_WhenCommentExists() {
        // Given
        Long commentId = 200L;
        given(commentQueryPort.findById(commentId)).willReturn(Optional.of(testComment));

        // When
        Optional<Comment> result = commentQueryService.findById(commentId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(200L);
        assertThat(result.get().getContent()).isEqualTo("테스트 댓글");

        verify(commentQueryPort).findById(commentId);
    }

    @Test
    @DisplayName("ID로 댓글 조회 - 존재하지 않는 댓글")
    void shouldReturnEmpty_WhenCommentNotExists() {
        // Given
        Long commentId = 999L;
        given(commentQueryPort.findById(commentId)).willReturn(Optional.empty());

        // When
        Optional<Comment> result = commentQueryService.findById(commentId);

        // Then
        assertThat(result).isEmpty();

        verify(commentQueryPort).findById(commentId);
    }

    @Test
    @DisplayName("사용자 작성 댓글 목록 조회 성공")
    void shouldGetUserComments_WhenUserHasComments() {
        // Given
        Long userId = 100L;
        Pageable pageable = Pageable.ofSize(20).withPage(0);
        List<SimpleCommentDTO> comments = Collections.singletonList(simpleCommentDTO);
        Page<SimpleCommentDTO> commentPage = new PageImpl<>(comments);

        given(commentQueryPort.findCommentsByUserId(userId, pageable)).willReturn(commentPage);

        // When
        Page<SimpleCommentDTO> result = commentQueryService.getUserComments(userId, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getId()).isEqualTo(200L);
        assertThat(result.getContent().getFirst().getUserName()).isEqualTo("testUser");

        verify(commentQueryPort).findCommentsByUserId(userId, pageable);
    }

    @Test
    @DisplayName("사용자 작성 댓글 목록 조회 - 작성한 댓글이 없는 경우")
    void shouldReturnEmptyPage_WhenUserHasNoComments() {
        // Given
        Long userId = 100L;
        Pageable pageable = Pageable.ofSize(20).withPage(0);
        Page<SimpleCommentDTO> emptyPage = new PageImpl<>(Collections.emptyList());

        given(commentQueryPort.findCommentsByUserId(userId, pageable)).willReturn(emptyPage);

        // When
        Page<SimpleCommentDTO> result = commentQueryService.getUserComments(userId, pageable);

        // Then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);

        verify(commentQueryPort).findCommentsByUserId(userId, pageable);
    }

    @Test
    @DisplayName("사용자 추천한 댓글 목록 조회 성공")
    void shouldGetUserLikedComments_WhenUserHasLikedComments() {
        // Given
        Long userId = 100L;
        Pageable pageable = Pageable.ofSize(20).withPage(0);
        SimpleCommentDTO likedComment = new SimpleCommentDTO(
                200L, 300L, "anotherUser", "추천한 댓글", Instant.now(), 10, true
        );
        List<SimpleCommentDTO> likedComments = List.of(likedComment);
        Page<SimpleCommentDTO> commentPage = new PageImpl<>(likedComments);

        given(commentQueryPort.findLikedCommentsByUserId(userId, pageable)).willReturn(commentPage);

        // When
        Page<SimpleCommentDTO> result = commentQueryService.getUserLikedComments(userId, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getId()).isEqualTo(200L);
        assertThat(result.getContent().getFirst().isUserLike()).isTrue();
        assertThat(result.getContent().getFirst().getLikes()).isEqualTo(10);

        verify(commentQueryPort).findLikedCommentsByUserId(userId, pageable);
    }

    @Test
    @DisplayName("사용자 추천한 댓글 목록 조회 - 추천한 댓글이 없는 경우")
    void shouldReturnEmptyPage_WhenUserHasNoLikedComments() {
        // Given
        Long userId = 100L;
        Pageable pageable = Pageable.ofSize(20).withPage(0);
        Page<SimpleCommentDTO> emptyPage = new PageImpl<>(Collections.emptyList());

        given(commentQueryPort.findLikedCommentsByUserId(userId, pageable)).willReturn(emptyPage);

        // When
        Page<SimpleCommentDTO> result = commentQueryService.getUserLikedComments(userId, pageable);

        // Then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);

        verify(commentQueryPort).findLikedCommentsByUserId(userId, pageable);
    }

    @Test
    @DisplayName("익명 댓글 처리 검증")
    void shouldHandleAnonymousComments() {
        // Given
        SimpleCommentDTO anonymousComment = new SimpleCommentDTO(
                203L, 300L, null, "익명 댓글", Instant.now(), 3, false
        );
        
        // SimpleCommentDTO 생성자에서 userName이 null이면 "익명"으로 처리됨을 검증
        assertThat(anonymousComment.getUserName()).isEqualTo("익명");
        assertThat(anonymousComment.getContent()).isEqualTo("익명 댓글");
        assertThat(anonymousComment.getId()).isEqualTo(203L);
    }
}