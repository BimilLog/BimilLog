package jaeik.bimillog.domain.comment.service;

import jaeik.bimillog.domain.comment.application.port.out.CommentQueryPort;
import jaeik.bimillog.domain.comment.application.service.CommentQueryService;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.comment.entity.CommentInfo;
import jaeik.bimillog.domain.comment.entity.SimpleCommentInfo;
import jaeik.bimillog.domain.comment.exception.CommentCustomException;
import jaeik.bimillog.domain.comment.exception.CommentErrorCode;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.adapter.auth.out.auth.CustomUserDetails;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
    private CommentInfo commentInfo;
    private SimpleCommentInfo simpleCommentInfo;

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

        commentInfo = CommentInfo.builder()
                .id(200L)
                .content("테스트 댓글")
                .userName("testUser")
                .likeCount(5)
                .userLike(false)
                .build();

        simpleCommentInfo = new SimpleCommentInfo(
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
        List<CommentInfo> popularComments = Collections.singletonList(commentInfo);

        given(userDetails.getUserId()).willReturn(100L);
        // 단일 쿼리로 인기 댓글과 사용자 추천 여부를 동시에 조회
        given(commentQueryPort.findPopularComments(postId, 100L)).willReturn(popularComments);

        // When
        List<CommentInfo> result = commentQueryService.getPopularComments(postId, userDetails);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(200L);
        assertThat(result.getFirst().getContent()).isEqualTo("테스트 댓글");

        // 단일 쿼리로 최적화되어 한 번만 호출됨
        verify(commentQueryPort).findPopularComments(postId, 100L);
    }

    @Test
    @DisplayName("인기 댓글 조회 성공 - 익명 사용자")
    void shouldGetPopularComments_WhenAnonymousUser() {
        // Given
        Long postId = 300L;
        List<CommentInfo> popularComments = Collections.singletonList(commentInfo);

        given(commentQueryPort.findPopularComments(postId, null)).willReturn(popularComments);

        // When
        List<CommentInfo> result = commentQueryService.getPopularComments(postId, null);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(200L);

        verify(commentQueryPort).findPopularComments(postId, null);
    }

    @Test
    @DisplayName("인기 댓글 조회 - 댓글이 없는 경우")
    void shouldReturnEmptyList_WhenNoPopularComments() {
        // Given
        Long postId = 300L;
        List<CommentInfo> emptyComments = Collections.emptyList();

        given(userDetails.getUserId()).willReturn(100L);
        given(commentQueryPort.findPopularComments(postId, 100L)).willReturn(emptyComments);

        // When
        List<CommentInfo> result = commentQueryService.getPopularComments(postId, userDetails);

        // Then
        assertThat(result).isEmpty();

        // 단일 쿼리로 최적화되어 한 번만 호출됨
        verify(commentQueryPort, times(1)).findPopularComments(postId, 100L);
    }

    @Test
    @DisplayName("과거순 댓글 조회 성공 - 로그인 사용자")
    void shouldGetCommentsOldestOrder_WhenLoggedInUser() {
        // Given
        Long postId = 300L;
        int page = 0;
        List<CommentInfo> comments = Collections.singletonList(commentInfo);
        Page<CommentInfo> commentPage = new PageImpl<>(comments);

        given(userDetails.getUserId()).willReturn(100L);
        given(commentQueryPort.findCommentsWithOldestOrder(eq(postId), any(Pageable.class), eq(100L)))
                .willReturn(commentPage);

        // When
        Pageable pageable = Pageable.ofSize(20).withPage(page);
        Page<CommentInfo> result = commentQueryService.getCommentsOldestOrder(postId, pageable, userDetails);

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
        List<CommentInfo> comments = Collections.singletonList(commentInfo);
        Page<CommentInfo> commentPage = new PageImpl<>(comments);

        given(commentQueryPort.findCommentsWithOldestOrder(eq(postId), any(Pageable.class), isNull()))
                .willReturn(commentPage);

        // When
        Pageable pageable = Pageable.ofSize(20).withPage(page);
        Page<CommentInfo> result = commentQueryService.getCommentsOldestOrder(postId, pageable, null);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getId()).isEqualTo(200L);

        verify(commentQueryPort).findCommentsWithOldestOrder(eq(postId), any(Pageable.class), isNull());
    }

    @Test
    @DisplayName("과거순 댓글 조회 - 빈 페이지")
    void shouldReturnEmptyPage_WhenNoCommentsInPage() {
        // Given
        Long postId = 300L;
        int page = 0;
        Page<CommentInfo> emptyPage = new PageImpl<>(Collections.emptyList());

        given(userDetails.getUserId()).willReturn(100L);
        given(commentQueryPort.findCommentsWithOldestOrder(eq(postId), any(Pageable.class), eq(100L)))
                .willReturn(emptyPage);

        // When
        Pageable pageable = Pageable.ofSize(20).withPage(page);
        Page<CommentInfo> result = commentQueryService.getCommentsOldestOrder(postId, pageable, userDetails);

        // Then
        assertThat(result.getContent()).isEmpty();

        verify(commentQueryPort, times(1)).findCommentsWithOldestOrder(eq(postId), any(Pageable.class), eq(100L));
    }

    @Test
    @DisplayName("ID로 댓글 조회 성공")
    void shouldFindById_WhenCommentExists() {
        // Given
        Long commentId = 200L;
        given(commentQueryPort.findById(commentId)).willReturn(testComment);

        // When
        Comment result = commentQueryService.findById(commentId);

        // Then
        assertThat(result.getId()).isEqualTo(200L);
        assertThat(result.getContent()).isEqualTo("테스트 댓글");

        verify(commentQueryPort).findById(commentId);
    }

    @Test
    @DisplayName("ID로 댓글 조회 - 존재하지 않는 댓글")
    void shouldReturnEmpty_WhenCommentNotExists() {
        // Given
        Long commentId = 999L;
        given(commentQueryPort.findById(commentId)).willThrow(new CommentCustomException(CommentErrorCode.COMMENT_NOT_FOUND));

        // When & Then
        assertThatThrownBy(() -> commentQueryService.findById(commentId))
                .isInstanceOf(CommentCustomException.class)
                .hasMessage(CommentErrorCode.COMMENT_NOT_FOUND.getMessage());

        verify(commentQueryPort).findById(commentId);
    }

    @Test
    @DisplayName("사용자 작성 댓글 목록 조회 성공")
    void shouldGetUserComments_WhenUserHasComments() {
        // Given
        Long userId = 100L;
        Pageable pageable = Pageable.ofSize(20).withPage(0);
        List<SimpleCommentInfo> comments = Collections.singletonList(simpleCommentInfo);
        Page<SimpleCommentInfo> commentPage = new PageImpl<>(comments);

        given(commentQueryPort.findCommentsByUserId(userId, pageable)).willReturn(commentPage);

        // When
        Page<SimpleCommentInfo> result = commentQueryService.getUserComments(userId, pageable);

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
        Page<SimpleCommentInfo> emptyPage = new PageImpl<>(Collections.emptyList());

        given(commentQueryPort.findCommentsByUserId(userId, pageable)).willReturn(emptyPage);

        // When
        Page<SimpleCommentInfo> result = commentQueryService.getUserComments(userId, pageable);

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
        SimpleCommentInfo likedComment = new SimpleCommentInfo(
                200L, 300L, "anotherUser", "추천한 댓글", Instant.now(), 10, true
        );
        List<SimpleCommentInfo> likedComments = List.of(likedComment);
        Page<SimpleCommentInfo> commentPage = new PageImpl<>(likedComments);

        given(commentQueryPort.findLikedCommentsByUserId(userId, pageable)).willReturn(commentPage);

        // When
        Page<SimpleCommentInfo> result = commentQueryService.getUserLikedComments(userId, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getId()).isEqualTo(200L);
        assertThat(result.getContent().getFirst().isUserLike()).isTrue();
        assertThat(result.getContent().getFirst().getLikeCount()).isEqualTo(10);

        verify(commentQueryPort).findLikedCommentsByUserId(userId, pageable);
    }

    @Test
    @DisplayName("사용자 추천한 댓글 목록 조회 - 추천한 댓글이 없는 경우")
    void shouldReturnEmptyPage_WhenUserHasNoLikedComments() {
        // Given
        Long userId = 100L;
        Pageable pageable = Pageable.ofSize(20).withPage(0);
        Page<SimpleCommentInfo> emptyPage = new PageImpl<>(Collections.emptyList());

        given(commentQueryPort.findLikedCommentsByUserId(userId, pageable)).willReturn(emptyPage);

        // When
        Page<SimpleCommentInfo> result = commentQueryService.getUserLikedComments(userId, pageable);

        // Then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);

        verify(commentQueryPort).findLikedCommentsByUserId(userId, pageable);
    }

    @Test
    @DisplayName("익명 댓글 처리 검증")
    void shouldHandleAnonymousComments() {
        // Given
        SimpleCommentInfo anonymousComment = new SimpleCommentInfo(
                203L, 300L, null, "익명 댓글", Instant.now(), 3, false
        );
        
        // SimpleCommentInfo 생성자에서 userName이 null이면 "익명"으로 처리됨을 검증
        assertThat(anonymousComment.getUserName()).isEqualTo("익명");
        assertThat(anonymousComment.getContent()).isEqualTo("익명 댓글");
        assertThat(anonymousComment.getId()).isEqualTo(203L);
    }

    // === 누락된 배치 조회 테스트 케이스 ===

    @Test
    @DisplayName("게시글 목록의 댓글 수 조회 성공")
    void shouldFindCommentCountsByPostIds_WhenMultiplePostsExist() {
        // Given
        List<Long> postIds = List.of(100L, 200L, 300L);
        Map<Long, Integer> expectedCommentCounts = Map.of(
                100L, 5,
                200L, 3,
                300L, 0
        );

        given(commentQueryPort.findCommentCountsByPostIds(postIds)).willReturn(expectedCommentCounts);

        // When
        Map<Long, Integer> result = commentQueryService.findCommentCountsByPostIds(postIds);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.get(100L)).isEqualTo(5);
        assertThat(result.get(200L)).isEqualTo(3);
        assertThat(result.get(300L)).isEqualTo(0);

        verify(commentQueryPort).findCommentCountsByPostIds(postIds);
    }

    @Test
    @DisplayName("게시글 목록이 비어있는 경우 빈 Map 반환")
    void shouldReturnEmptyMap_WhenPostIdsListIsEmpty() {
        // Given
        List<Long> emptyPostIds = Collections.emptyList();
        Map<Long, Integer> emptyCommentCounts = Collections.emptyMap();

        given(commentQueryPort.findCommentCountsByPostIds(emptyPostIds)).willReturn(emptyCommentCounts);

        // When
        Map<Long, Integer> result = commentQueryService.findCommentCountsByPostIds(emptyPostIds);

        // Then
        assertThat(result).isEmpty();

        verify(commentQueryPort).findCommentCountsByPostIds(emptyPostIds);
    }

    @Test
    @DisplayName("댓글이 없는 게시글들의 댓글 수 조회")
    void shouldReturnZeroCount_WhenPostsHaveNoComments() {
        // Given
        List<Long> postIds = List.of(400L, 500L);
        Map<Long, Integer> commentCounts = Map.of(
                400L, 0,
                500L, 0
        );

        given(commentQueryPort.findCommentCountsByPostIds(postIds)).willReturn(commentCounts);

        // When
        Map<Long, Integer> result = commentQueryService.findCommentCountsByPostIds(postIds);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(400L)).isEqualTo(0);
        assertThat(result.get(500L)).isEqualTo(0);

        verify(commentQueryPort).findCommentCountsByPostIds(postIds);
    }

    @Test
    @DisplayName("대용량 게시글 목록의 댓글 수 조회")
    void shouldFindCommentCountsByPostIds_WhenLargePostIdsList() {
        // Given
        List<Long> largePostIds = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 
                                          11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L);
        Map<Long, Integer> commentCounts = Map.of(
                1L, 10, 2L, 5, 3L, 3, 4L, 0, 5L, 2,
                6L, 7, 7L, 1, 8L, 0, 9L, 4, 10L, 6
        );

        given(commentQueryPort.findCommentCountsByPostIds(largePostIds)).willReturn(commentCounts);

        // When
        Map<Long, Integer> result = commentQueryService.findCommentCountsByPostIds(largePostIds);

        // Then
        assertThat(result).hasSize(10); // 실제 댓글이 있는 게시글만 반환
        assertThat(result.get(1L)).isEqualTo(10);
        assertThat(result.get(4L)).isEqualTo(0);

        verify(commentQueryPort).findCommentCountsByPostIds(largePostIds);
    }
}