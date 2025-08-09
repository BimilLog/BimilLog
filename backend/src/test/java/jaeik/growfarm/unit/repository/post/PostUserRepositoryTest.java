package jaeik.growfarm.unit.repository.post;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.dto.post.SimplePostDTO;
import jaeik.growfarm.entity.post.PopularFlag;
import jaeik.growfarm.entity.post.QPost;
import jaeik.growfarm.entity.post.QPostLike;
import jaeik.growfarm.entity.user.QUsers;
import jaeik.growfarm.repository.comment.CommentRepository;
import jaeik.growfarm.repository.post.user.PostUserRepositoryImpl;
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
 * <h2>PostUserRepositoryImpl 단위 테스트</h2>
 * <p>
 * 사용자별 게시글 조회 레포지터리의 단위 테스트
 * </p>
 *
 * @author Jaeik
 * @version 1.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostUserRepository 단위 테스트")
class PostUserRepositoryTest {

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

    private PostUserRepositoryImpl postUserRepository;

    @BeforeEach
    void setUp() {
        postUserRepository = new PostUserRepositoryImpl(jpaQueryFactory, commentRepository);
    }

    @Test
    @DisplayName("사용자 작성 글 목록 조회 - 성공")
    void findPostsByUserId_Success() {
        // Given
        Long userId = 100L;
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
        when(countQuery.fetchOne()).thenReturn(5L);

        // Mock tuple values
        when(tuple.get(any(Expression.class))).thenReturn(
                1L, "User's Post Title", 50, false, null, Instant.now(), userId, "testUser"
        );

        // Mock comment and like counts
        when(commentRepository.findCommentCountsByPostIds(anyList()))
                .thenReturn(Map.of(1L, 3, 2L, 5));

        // Mock fetchLikeCounts (simulated through processPostTuples)
        when(jpaQueryFactory.select(any(), any())).thenReturn(mock(JPAQuery.class));

        // When
        Page<SimplePostDTO> result = postUserRepository.findPostsByUserId(userId, pageable);

        // Then
        assertNotNull(result);
        assertEquals(5L, result.getTotalElements());
        verify(jpaQueryFactory, atLeastOnce()).select(any(), any(), any(), any(), any(), any(), any(), any());
        verify(jpaQueryFactory, atLeastOnce()).select(any(Expression.class));
        verify(commentRepository).findCommentCountsByPostIds(anyList());
    }

    @Test
    @DisplayName("사용자 작성 글 목록 조회 - 빈 결과")
    void findPostsByUserId_EmptyResult() {
        // Given
        Long userId = 999L; // 게시글이 없는 사용자
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
        Page<SimplePostDTO> result = postUserRepository.findPostsByUserId(userId, pageable);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
        verifyNoInteractions(commentRepository);
    }

    @Test
    @DisplayName("사용자가 추천한 글 목록 조회 - 성공")
    void findLikedPostsByUserId_Success() {
        // Given
        Long userId = 100L;
        Pageable pageable = PageRequest.of(0, 10);
        List<Tuple> mockTuples = Arrays.asList(tuple);
        
        // Mock 추천 게시글 조회
        when(jpaQueryFactory.select(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(jpaQuery);
        when(jpaQuery.from(any(QPost.class))).thenReturn(jpaQuery);
        when(jpaQuery.join(any(QUsers.class), any(QUsers.class))).thenReturn(jpaQuery);
        when(jpaQuery.join(any(QPostLike.class))).thenReturn(jpaQuery);
        when(jpaQuery.on(any())).thenReturn(jpaQuery);
        when(jpaQuery.where(any())).thenReturn(jpaQuery);
        when(jpaQuery.orderBy(any())).thenReturn(jpaQuery);
        when(jpaQuery.offset(anyLong())).thenReturn(jpaQuery);
        when(jpaQuery.limit(anyLong())).thenReturn(jpaQuery);
        when(jpaQuery.fetch()).thenReturn(mockTuples);

        // Mock 추천 게시글 총 개수 조회
        when(jpaQueryFactory.select(any(Expression.class))).thenReturn(countQuery);
        when(countQuery.from(any(QPost.class))).thenReturn(countQuery);
        when(countQuery.join(any(QPostLike.class))).thenReturn(countQuery);
        when(countQuery.on(any())).thenReturn(countQuery);
        when(countQuery.where(any())).thenReturn(countQuery);
        when(countQuery.fetchOne()).thenReturn(3L);

        // Mock tuple values
        when(tuple.get(any(Expression.class))).thenReturn(
                10L, "Liked Post Title", 200, false, PopularFlag.WEEKLY, Instant.now(), 200L, "otherUser"
        );

        // Mock comment and like counts
        when(commentRepository.findCommentCountsByPostIds(anyList()))
                .thenReturn(Map.of(10L, 8));

        // Mock fetchLikeCounts
        when(jpaQueryFactory.select(any(), any())).thenReturn(mock(JPAQuery.class));

        // When
        Page<SimplePostDTO> result = postUserRepository.findLikedPostsByUserId(userId, pageable);

        // Then
        assertNotNull(result);
        assertEquals(3L, result.getTotalElements());
        assertFalse(result.getContent().isEmpty());
        
        verify(jpaQuery, times(2)).join(any()); // user join + postLike join
        verify(jpaQuery, times(2)).where(any()); // 조회와 카운트에서 각각 호출
        verify(commentRepository).findCommentCountsByPostIds(anyList());
    }

    @Test
    @DisplayName("사용자가 추천한 글 목록 조회 - 추천한 글 없음")
    void findLikedPostsByUserId_NoLikedPosts() {
        // Given
        Long userId = 100L;
        Pageable pageable = PageRequest.of(0, 10);
        
        when(jpaQueryFactory.select(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(jpaQuery);
        when(jpaQuery.from(any(QPost.class))).thenReturn(jpaQuery);
        when(jpaQuery.join(any(QUsers.class), any(QUsers.class))).thenReturn(jpaQuery);
        when(jpaQuery.join(any(QPostLike.class))).thenReturn(jpaQuery);
        when(jpaQuery.on(any())).thenReturn(jpaQuery);
        when(jpaQuery.where(any())).thenReturn(jpaQuery);
        when(jpaQuery.orderBy(any())).thenReturn(jpaQuery);
        when(jpaQuery.offset(anyLong())).thenReturn(jpaQuery);
        when(jpaQuery.limit(anyLong())).thenReturn(jpaQuery);
        when(jpaQuery.fetch()).thenReturn(Collections.emptyList());

        when(jpaQueryFactory.select(any(Expression.class))).thenReturn(countQuery);
        when(countQuery.from(any(QPost.class))).thenReturn(countQuery);
        when(countQuery.join(any(QPostLike.class))).thenReturn(countQuery);
        when(countQuery.on(any())).thenReturn(countQuery);
        when(countQuery.where(any())).thenReturn(countQuery);
        when(countQuery.fetchOne()).thenReturn(0L);

        // When
        Page<SimplePostDTO> result = postUserRepository.findLikedPostsByUserId(userId, pageable);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
        verifyNoInteractions(commentRepository);
    }

    @Test
    @DisplayName("사용자 작성 글 목록 조회 - 페이징 확인")
    void findPostsByUserId_Paging() {
        // Given
        Long userId = 100L;
        Pageable pageable = PageRequest.of(1, 5); // 2번째 페이지, 5개씩
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
        when(countQuery.fetchOne()).thenReturn(15L); // 총 15개 게시글

        when(tuple.get(any(Expression.class))).thenReturn(
                6L, "Page 2 Post", 30, false, null, Instant.now(), userId, "testUser"
        );

        when(commentRepository.findCommentCountsByPostIds(anyList()))
                .thenReturn(Map.of(6L, 2));

        when(jpaQueryFactory.select(any(), any())).thenReturn(mock(JPAQuery.class));

        // When
        Page<SimplePostDTO> result = postUserRepository.findPostsByUserId(userId, pageable);

        // Then
        assertNotNull(result);
        assertEquals(15L, result.getTotalElements());
        assertEquals(3, result.getTotalPages()); // 15개 ÷ 5개 = 3페이지
        assertEquals(1, result.getNumber()); // 현재 페이지 번호 (0부터 시작)
        assertEquals(5, result.getSize()); // 페이지 크기
        
        // offset(5), limit(5)가 호출되었는지 확인
        verify(jpaQuery).offset(5L);
        verify(jpaQuery).limit(5L);
    }

    @Test
    @DisplayName("사용자가 추천한 글 목록 조회 - 다른 사용자의 글만 포함")
    void findLikedPostsByUserId_ContainsOtherUsersPostsOnly() {
        // Given
        Long userId = 100L; // 추천한 사용자
        Long postAuthorId = 200L; // 게시글 작성자 (다른 사용자)
        Pageable pageable = PageRequest.of(0, 10);
        List<Tuple> mockTuples = Arrays.asList(tuple);
        
        when(jpaQueryFactory.select(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(jpaQuery);
        when(jpaQuery.from(any(QPost.class))).thenReturn(jpaQuery);
        when(jpaQuery.join(any(QUsers.class), any(QUsers.class))).thenReturn(jpaQuery);
        when(jpaQuery.join(any(QPostLike.class))).thenReturn(jpaQuery);
        when(jpaQuery.on(any())).thenReturn(jpaQuery);
        when(jpaQuery.where(any())).thenReturn(jpaQuery);
        when(jpaQuery.orderBy(any())).thenReturn(jpaQuery);
        when(jpaQuery.offset(anyLong())).thenReturn(jpaQuery);
        when(jpaQuery.limit(anyLong())).thenReturn(jpaQuery);
        when(jpaQuery.fetch()).thenReturn(mockTuples);

        when(jpaQueryFactory.select(any(Expression.class))).thenReturn(countQuery);
        when(countQuery.from(any(QPost.class))).thenReturn(countQuery);
        when(countQuery.join(any(QPostLike.class))).thenReturn(countQuery);
        when(countQuery.on(any())).thenReturn(countQuery);
        when(countQuery.where(any())).thenReturn(countQuery);
        when(countQuery.fetchOne()).thenReturn(1L);

        // Mock tuple values - 다른 사용자가 작성한 글
        when(tuple.get(any(Expression.class))).thenReturn(
                15L, "Other User's Post", 100, false, null, Instant.now(), postAuthorId, "otherUser"
        );

        when(commentRepository.findCommentCountsByPostIds(anyList()))
                .thenReturn(Map.of(15L, 4));

        when(jpaQueryFactory.select(any(), any())).thenReturn(mock(JPAQuery.class));

        // When
        Page<SimplePostDTO> result = postUserRepository.findLikedPostsByUserId(userId, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getTotalElements());
        
        SimplePostDTO likedPost = result.getContent().get(0);
        assertEquals(15L, likedPost.getPostId());
        assertEquals(postAuthorId, likedPost.getUserId()); // 게시글 작성자는 다른 사용자
        assertEquals("otherUser", likedPost.getUserName());
        
        // postLike.user.id.eq(userId) 조건으로 추천한 사용자 필터링 확인
        verify(jpaQuery, times(2)).where(any()); // 조회와 카운트에서 각각 where 조건 사용
    }

    @Test
    @DisplayName("사용자 작성 글 조회 - 공지글 제외 확인")
    void findPostsByUserId_ExcludesNotices() {
        // Given - This test verifies that isNotice.eq(false) condition is applied
        Long userId = 100L;
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
        when(countQuery.fetchOne()).thenReturn(1L);

        // Mock tuple showing a regular post (not notice)
        when(tuple.get(any(Expression.class))).thenReturn(
                1L, "Regular Post", 30, false, null, Instant.now(), userId, "testUser"
        );

        when(commentRepository.findCommentCountsByPostIds(anyList()))
                .thenReturn(Map.of(1L, 2));

        when(jpaQueryFactory.select(any(), any())).thenReturn(mock(JPAQuery.class));

        // When
        Page<SimplePostDTO> result = postUserRepository.findPostsByUserId(userId, pageable);

        // Then
        assertNotNull(result);
        assertFalse(result.getContent().isEmpty());
        
        SimplePostDTO post = result.getContent().get(0);
        assertFalse(post.isIs_notice()); // Verify it's not a notice
        
        // Verify that where condition was applied (should include both user condition and isNotice.eq(false))
        verify(jpaQuery, times(2)).where(any()); // Called for both data fetch and count
    }

    @Test
    @DisplayName("사용자가 추천한 글 조회 - 데이터베이스 오류")
    void findLikedPostsByUserId_DatabaseError() {
        // Given
        Long userId = 100L;
        Pageable pageable = PageRequest.of(0, 10);
        
        when(jpaQueryFactory.select(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(jpaQuery);
        when(jpaQuery.from(any(QPost.class))).thenReturn(jpaQuery);
        when(jpaQuery.join(any(QUsers.class), any(QUsers.class))).thenReturn(jpaQuery);
        when(jpaQuery.join(any(QPostLike.class))).thenReturn(jpaQuery);
        when(jpaQuery.on(any())).thenReturn(jpaQuery);
        when(jpaQuery.where(any())).thenReturn(jpaQuery);
        when(jpaQuery.orderBy(any())).thenReturn(jpaQuery);
        when(jpaQuery.offset(anyLong())).thenReturn(jpaQuery);
        when(jpaQuery.limit(anyLong())).thenReturn(jpaQuery);
        when(jpaQuery.fetch()).thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        assertThrows(RuntimeException.class, () -> 
            postUserRepository.findLikedPostsByUserId(userId, pageable)
        );
        
        verify(jpaQuery).fetch();
    }

    @Test
    @DisplayName("사용자 작성 글 조회 - 카운트 쿼리 오류")
    void findPostsByUserId_CountQueryError() {
        // Given
        Long userId = 100L;
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
        when(countQuery.fetchOne()).thenThrow(new RuntimeException("Count query failed"));

        // When & Then
        assertThrows(RuntimeException.class, () -> 
            postUserRepository.findPostsByUserId(userId, pageable)
        );
        
        verify(jpaQuery).fetch(); // Data fetch should succeed
        verify(countQuery).fetchOne(); // Count fetch should fail
    }
}