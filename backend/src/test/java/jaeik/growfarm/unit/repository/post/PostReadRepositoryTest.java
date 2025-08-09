package jaeik.growfarm.unit.repository.post;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.dto.post.PostDTO;
import jaeik.growfarm.dto.post.SimplePostDTO;
import jaeik.growfarm.entity.post.PopularFlag;
import jaeik.growfarm.entity.post.QPost;
import jaeik.growfarm.entity.post.QPostLike;
import jaeik.growfarm.entity.user.QUsers;
import jaeik.growfarm.repository.comment.CommentRepository;
import jaeik.growfarm.repository.post.read.PostReadRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <h2>PostReadRepositoryImpl 단위 테스트</h2>
 * <p>
 * 게시글 조회 레포지터리의 단위 테스트
 * </p>
 *
 * @author Jaeik
 * @version 1.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostReadRepository 단위 테스트")
class PostReadRepositoryTest {

    @Mock
    private JPAQueryFactory jpaQueryFactory;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private JPAQuery<Tuple> jpaQuery;

    @Mock
    private JPAQuery<Long> countQuery;

    @Mock
    private Tuple tuple;

    private PostReadRepositoryImpl postReadRepository;

    @BeforeEach
    void setUp() {
        postReadRepository = new PostReadRepositoryImpl(jpaQueryFactory, commentRepository);
    }

    @Test
    @DisplayName("게시글 목록 조회 - 성공")
    void findPostsWithCommentAndLikeCounts_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Tuple> mockTuples = Arrays.asList(tuple, tuple);
        
        // Mock fetchPosts 호출을 위한 설정
        when(jpaQueryFactory.select(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(jpaQuery);
        when(jpaQuery.from(any(QPost.class))).thenReturn(jpaQuery);
        when(jpaQuery.leftJoin(any(QUsers.class), any(QUsers.class))).thenReturn(jpaQuery);
        when(jpaQuery.where(any())).thenReturn(jpaQuery);
        when(jpaQuery.orderBy(any())).thenReturn(jpaQuery);
        when(jpaQuery.offset(anyLong())).thenReturn(jpaQuery);
        when(jpaQuery.limit(anyLong())).thenReturn(jpaQuery);
        when(jpaQuery.fetch()).thenReturn(mockTuples);

        // Mock fetchTotalCount 호출을 위한 설정
        when(jpaQueryFactory.select(any(Expression.class))).thenReturn(countQuery);
        when(countQuery.from(any(QPost.class))).thenReturn(countQuery);
        when(countQuery.where(any())).thenReturn(countQuery);
        when(countQuery.fetchOne()).thenReturn(20L);

        // Mock tuple values
        when(tuple.get(any(Expression.class))).thenReturn(
                1L, "Test Title", 100, false, PopularFlag.REALTIME, Instant.now(), 1L, "testUser"
        );

        // Mock comment and like counts
        when(commentRepository.findCommentCountsByPostIds(anyList()))
                .thenReturn(Map.of(1L, 5, 2L, 3));

        // Mock fetchLikeCounts (simulated through processPostTuples)
        when(jpaQueryFactory.select(any(), any())).thenReturn(mock(JPAQuery.class));

        // When
        Page<SimplePostDTO> result = postReadRepository.findPostsWithCommentAndLikeCounts(pageable);

        // Then
        assertNotNull(result);
        verify(jpaQueryFactory, atLeastOnce()).select(any(), any(), any(), any(), any(), any(), any(), any());
        verify(jpaQueryFactory, atLeastOnce()).select(any(Expression.class));
        verify(commentRepository).findCommentCountsByPostIds(anyList());
    }

    @Test
    @DisplayName("게시글 목록 조회 - 빈 결과")
    void findPostsWithCommentAndLikeCounts_EmptyResult() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        
        when(jpaQueryFactory.select(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(jpaQuery);
        when(jpaQuery.from(any(QPost.class))).thenReturn(jpaQuery);
        when(jpaQuery.leftJoin(any(QUsers.class), any(QUsers.class))).thenReturn(jpaQuery);
        when(jpaQuery.where(any())).thenReturn(jpaQuery);
        when(jpaQuery.orderBy(any())).thenReturn(jpaQuery);
        when(jpaQuery.offset(anyLong())).thenReturn(jpaQuery);
        when(jpaQuery.limit(anyLong())).thenReturn(jpaQuery);
        when(jpaQuery.fetch()).thenReturn(Collections.emptyList());

        when(jpaQueryFactory.select(any(Expression.class))).thenReturn(countQuery);
        when(countQuery.from(any(QPost.class))).thenReturn(countQuery);
        when(countQuery.where(any())).thenReturn(countQuery);
        when(countQuery.fetchOne()).thenReturn(0L);

        // When
        Page<SimplePostDTO> result = postReadRepository.findPostsWithCommentAndLikeCounts(pageable);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
        verifyNoInteractions(commentRepository);
    }

    @Test
    @DisplayName("게시글 상세 조회 - 성공 (로그인 사용자)")
    void findPostById_Success_WithUser() {
        // Given
        Long postId = 1L;
        Long userId = 100L;
        Instant createdAt = Instant.now();
        
        when(jpaQueryFactory.select(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(jpaQuery);
        when(jpaQuery.from(any(QPost.class))).thenReturn(jpaQuery);
        when(jpaQuery.leftJoin(any(), any())).thenReturn(jpaQuery);
        when(jpaQuery.where(any())).thenReturn(jpaQuery);
        when(jpaQuery.fetchOne()).thenReturn(tuple);

        // Mock tuple values for post details
        when(tuple.get(any(Expression.class))).thenReturn(
                postId, "Test Post Title", "Test Post Content", 150, false, 
                PopularFlag.WEEKLY, createdAt, 2L, "postAuthor"
        );

        // Mock like counts
        JPAQuery<Tuple> likeQuery = mock(JPAQuery.class);
        when(jpaQueryFactory.select(any(), any())).thenReturn(likeQuery);
        when(likeQuery.from(any(QPostLike.class))).thenReturn(likeQuery);
        when(likeQuery.where(any())).thenReturn(likeQuery);
        when(likeQuery.groupBy(any())).thenReturn(likeQuery);
        when(likeQuery.fetch()).thenReturn(Arrays.asList(mock(Tuple.class)));

        // Mock user like check
        JPAQuery<Long> userLikeQuery = mock(JPAQuery.class);
        when(jpaQueryFactory.select(any(Expression.class))).thenReturn(userLikeQuery);
        when(userLikeQuery.from(any(QPostLike.class))).thenReturn(userLikeQuery);
        when(userLikeQuery.where(any())).thenReturn(userLikeQuery);
        when(userLikeQuery.fetchOne()).thenReturn(1L); // User has liked the post

        // When
        PostDTO result = postReadRepository.findPostById(postId, userId);

        // Then
        assertNotNull(result);
        assertEquals(postId, result.getPostId());
        assertEquals("Test Post Title", result.getTitle());
        assertEquals("Test Post Content", result.getContent());
        assertEquals(150, result.getViews());
        assertEquals("postAuthor", result.getUserName());
        assertEquals(PopularFlag.WEEKLY, result.getPopularFlag());
        assertTrue(result.isUserLike()); // User liked the post
        verify(jpaQueryFactory, atLeastOnce()).select(any(Expression.class)); // For user like check
    }

    @Test
    @DisplayName("게시글 상세 조회 - 성공 (비로그인 사용자)")
    void findPostById_Success_WithoutUser() {
        // Given
        Long postId = 1L;
        Long userId = null; // 비로그인 사용자
        Instant createdAt = Instant.now();
        
        when(jpaQueryFactory.select(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(jpaQuery);
        when(jpaQuery.from(any(QPost.class))).thenReturn(jpaQuery);
        when(jpaQuery.leftJoin(any(), any())).thenReturn(jpaQuery);
        when(jpaQuery.where(any())).thenReturn(jpaQuery);
        when(jpaQuery.fetchOne()).thenReturn(tuple);

        when(tuple.get(any(Expression.class))).thenReturn(
                postId, "Test Post Title", "Test Post Content", 75, true, 
                null, createdAt, 3L, "noticeAuthor"
        );

        // Mock like counts
        JPAQuery<Tuple> likeQuery = mock(JPAQuery.class);
        when(jpaQueryFactory.select(any(), any())).thenReturn(likeQuery);
        when(likeQuery.from(any(QPostLike.class))).thenReturn(likeQuery);
        when(likeQuery.where(any())).thenReturn(likeQuery);
        when(likeQuery.groupBy(any())).thenReturn(likeQuery);
        when(likeQuery.fetch()).thenReturn(Arrays.asList(mock(Tuple.class)));

        // When
        PostDTO result = postReadRepository.findPostById(postId, userId);

        // Then
        assertNotNull(result);
        assertEquals(postId, result.getPostId());
        assertEquals("Test Post Title", result.getTitle());
        assertEquals("Test Post Content", result.getContent());
        assertEquals(75, result.getViews());
        assertEquals("noticeAuthor", result.getUserName());
        assertTrue(result.isNotice());
        assertNull(result.getPopularFlag());
        assertFalse(result.isUserLike()); // 비로그인 사용자는 좋아요 없음
    }

    @Test
    @DisplayName("게시글 상세 조회 - 게시글 없음")
    void findPostById_NotFound() {
        // Given
        Long postId = 999L;
        Long userId = 100L;
        
        when(jpaQueryFactory.select(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(jpaQuery);
        when(jpaQuery.from(any(QPost.class))).thenReturn(jpaQuery);
        when(jpaQuery.leftJoin(any(), any())).thenReturn(jpaQuery);
        when(jpaQuery.where(any())).thenReturn(jpaQuery);
        when(jpaQuery.fetchOne()).thenReturn(null); // 게시글 없음

        // When
        PostDTO result = postReadRepository.findPostById(postId, userId);

        // Then
        assertNull(result);
        verify(jpaQuery).fetchOne();
        verifyNoMoreInteractions(jpaQueryFactory); // 추가적인 쿼리 호출 없어야 함
    }

    @Test
    @DisplayName("게시글 상세 조회 - 익명 사용자 (userName null)")
    void findPostById_AnonymousAuthor() {
        // Given
        Long postId = 1L;
        Long userId = 100L;
        Instant createdAt = Instant.now();
        
        when(jpaQueryFactory.select(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(jpaQuery);
        when(jpaQuery.from(any(QPost.class))).thenReturn(jpaQuery);
        when(jpaQuery.leftJoin(any(), any())).thenReturn(jpaQuery);
        when(jpaQuery.where(any())).thenReturn(jpaQuery);
        when(jpaQuery.fetchOne()).thenReturn(tuple);

        // userName이 null인 경우 (익명 사용자)
        when(tuple.get(any(Expression.class))).thenReturn(
                postId, "Anonymous Post", "Anonymous Content", null, false, 
                null, createdAt, null, null // userName이 null
        );

        // Mock like counts
        JPAQuery<Tuple> likeQuery = mock(JPAQuery.class);
        when(jpaQueryFactory.select(any(), any())).thenReturn(likeQuery);
        when(likeQuery.from(any(QPostLike.class))).thenReturn(likeQuery);
        when(likeQuery.where(any())).thenReturn(likeQuery);
        when(likeQuery.groupBy(any())).thenReturn(likeQuery);
        when(likeQuery.fetch()).thenReturn(Collections.emptyList());

        // Mock user like check
        JPAQuery<Long> userLikeQuery = mock(JPAQuery.class);
        when(jpaQueryFactory.select(any(Expression.class))).thenReturn(userLikeQuery);
        when(userLikeQuery.from(any(QPostLike.class))).thenReturn(userLikeQuery);
        when(userLikeQuery.where(any())).thenReturn(userLikeQuery);
        when(userLikeQuery.fetchOne()).thenReturn(0L);

        // When
        PostDTO result = postReadRepository.findPostById(postId, userId);

        // Then
        assertNotNull(result);
        assertEquals("익명", result.getUserName()); // null userName은 "익명"으로 처리
        assertEquals(0, result.getViews()); // null views는 0으로 처리
        assertEquals(0, result.getLikes()); // 빈 like counts는 0으로 처리
    }

    @Test
    @DisplayName("게시글 상세 조회 - 사용자 좋아요 확인 쿼리 오류")
    void findPostById_UserLikeCheckError() {
        // Given
        Long postId = 1L;
        Long userId = 100L;
        Instant createdAt = Instant.now();
        
        when(jpaQueryFactory.select(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(jpaQuery);
        when(jpaQuery.from(any(QPost.class))).thenReturn(jpaQuery);
        when(jpaQuery.leftJoin(any(), any())).thenReturn(jpaQuery);
        when(jpaQuery.where(any())).thenReturn(jpaQuery);
        when(jpaQuery.fetchOne()).thenReturn(tuple);

        when(tuple.get(any(Expression.class))).thenReturn(
                postId, "Test Post", "Content", 50, false, 
                null, createdAt, 2L, "author"
        );

        // Mock like counts
        JPAQuery<Tuple> likeQuery = mock(JPAQuery.class);
        when(jpaQueryFactory.select(any(), any())).thenReturn(likeQuery);
        when(likeQuery.from(any(QPostLike.class))).thenReturn(likeQuery);
        when(likeQuery.where(any())).thenReturn(likeQuery);
        when(likeQuery.groupBy(any())).thenReturn(likeQuery);
        when(likeQuery.fetch()).thenReturn(Collections.emptyList());

        // Mock user like check - 오류 발생
        JPAQuery<Long> userLikeQuery = mock(JPAQuery.class);
        when(jpaQueryFactory.select(any(Expression.class))).thenReturn(userLikeQuery);
        when(userLikeQuery.from(any(QPostLike.class))).thenReturn(userLikeQuery);
        when(userLikeQuery.where(any())).thenReturn(userLikeQuery);
        when(userLikeQuery.fetchOne()).thenThrow(new RuntimeException("User like check failed"));

        // When & Then
        assertThrows(RuntimeException.class, () -> 
            postReadRepository.findPostById(postId, userId)
        );
    }

    @Test
    @DisplayName("게시글 상세 조회 - 좋아요 수 조회 오류")
    void findPostById_LikeCountError() {
        // Given
        Long postId = 1L;
        Long userId = null; // 비로그인 사용자로 설정하여 사용자 좋아요 체크 건너뜀
        Instant createdAt = Instant.now();
        
        when(jpaQueryFactory.select(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(jpaQuery);
        when(jpaQuery.from(any(QPost.class))).thenReturn(jpaQuery);
        when(jpaQuery.leftJoin(any(), any())).thenReturn(jpaQuery);
        when(jpaQuery.where(any())).thenReturn(jpaQuery);
        when(jpaQuery.fetchOne()).thenReturn(tuple);

        when(tuple.get(any(Expression.class))).thenReturn(
                postId, "Test Post", "Content", 75, false, 
                null, createdAt, 3L, "author"
        );

        // Mock like counts - 오류 발생
        JPAQuery<Tuple> likeQuery = mock(JPAQuery.class);
        when(jpaQueryFactory.select(any(), any())).thenReturn(likeQuery);
        when(likeQuery.from(any(QPostLike.class))).thenReturn(likeQuery);
        when(likeQuery.where(any())).thenReturn(likeQuery);
        when(likeQuery.groupBy(any())).thenReturn(likeQuery);
        when(likeQuery.fetch()).thenThrow(new RuntimeException("Like count query failed"));

        // When & Then
        assertThrows(RuntimeException.class, () -> 
            postReadRepository.findPostById(postId, userId)
        );
    }

    @Test
    @DisplayName("게시글 목록 조회 - 댓글 수 조회 오류")
    void findPostsWithCommentAndLikeCounts_CommentCountError() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Tuple> mockTuples = Arrays.asList(tuple);
        
        when(jpaQueryFactory.select(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(jpaQuery);
        when(jpaQuery.from(any(QPost.class))).thenReturn(jpaQuery);
        when(jpaQuery.leftJoin(any(QUsers.class), any(QUsers.class))).thenReturn(jpaQuery);
        when(jpaQuery.where(any())).thenReturn(jpaQuery);
        when(jpaQuery.orderBy(any())).thenReturn(jpaQuery);
        when(jpaQuery.offset(anyLong())).thenReturn(jpaQuery);
        when(jpaQuery.limit(anyLong())).thenReturn(jpaQuery);
        when(jpaQuery.fetch()).thenReturn(mockTuples);

        when(jpaQueryFactory.select(any(Expression.class))).thenReturn(countQuery);
        when(countQuery.from(any(QPost.class))).thenReturn(countQuery);
        when(countQuery.where(any())).thenReturn(countQuery);
        when(countQuery.fetchOne()).thenReturn(5L);

        when(tuple.get(any(Expression.class))).thenReturn(
                1L, "Test Title", 100, false, null, Instant.now(), 1L, "testUser"
        );

        // Mock comment counts - 오류 발생
        when(commentRepository.findCommentCountsByPostIds(anyList()))
                .thenThrow(new RuntimeException("Comment count query failed"));

        // When & Then
        assertThrows(RuntimeException.class, () -> 
            postReadRepository.findPostsWithCommentAndLikeCounts(pageable)
        );
    }
}