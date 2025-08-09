package jaeik.growfarm.unit.repository.post;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.jpa.impl.JPAUpdateClause;
import jaeik.growfarm.dto.post.SimplePostDTO;
import jaeik.growfarm.entity.post.PopularFlag;
import jaeik.growfarm.entity.post.QPost;
import jaeik.growfarm.entity.post.QPostLike;
import jaeik.growfarm.entity.user.QUsers;
import jaeik.growfarm.repository.comment.CommentRepository;
import jaeik.growfarm.repository.post.popular.PostPopularRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <h2>PostPopularRepositoryImpl 단위 테스트</h2>
 * <p>
 * 인기글 관리 레포지터리의 단위 테스트
 * </p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostPopularRepository 단위 테스트")
class PostPopularRepositoryTest {

    @Mock
    private JPAQueryFactory jpaQueryFactory;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private JPAQuery<Tuple> jpaQuery;

    @Mock
    private JPAUpdateClause jpaUpdateClause;

    @Mock
    private Tuple tuple;

    private PostPopularRepositoryImpl postPopularRepository;

    @BeforeEach
    void setUp() {
        postPopularRepository = new PostPopularRepositoryImpl(jpaQueryFactory, commentRepository);
    }

    @Test
    @DisplayName("실시간 인기글 선정 - 성공")
    void updateRealtimePopularPosts_Success() {
        // Given
        List<Tuple> mockTuples = Arrays.asList(tuple, tuple);
        List<Long> postIds = Arrays.asList(1L, 2L);
        Map<Long, Integer> commentCounts = Map.of(1L, 5, 2L, 3);
        Map<Long, Integer> likeCounts = Map.of(1L, 10, 2L, 8);

        // Mock JPAQueryFactory chain for reset
        when(jpaQueryFactory.update(any(QPost.class))).thenReturn(jpaUpdateClause);
        when(jpaUpdateClause.set(any(Expression.class), any())).thenReturn(jpaUpdateClause);
        when(jpaUpdateClause.where(any(Expression.class))).thenReturn(jpaUpdateClause);
        when(jpaUpdateClause.execute()).thenReturn(1L);

        // Mock JPAQueryFactory chain for select
        when(jpaQueryFactory.select(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(jpaQuery);
        when(jpaQuery.from(any(QPost.class))).thenReturn(jpaQuery);
        when(jpaQuery.leftJoin(any(QUsers.class))).thenReturn(jpaQuery);
        when(jpaQuery.on(any(Expression.class))).thenReturn(jpaQuery);
        when(jpaQuery.join(any(QPostLike.class))).thenReturn(jpaQuery);
        when(jpaQuery.leftJoin(any())).thenReturn(jpaQuery);
        when(jpaQuery.where(any(Expression.class))).thenReturn(jpaQuery);
        when(jpaQuery.groupBy(any(Expression.class))).thenReturn(jpaQuery);
        when(jpaQuery.orderBy(any(Expression.class))).thenReturn(jpaQuery);
        when(jpaQuery.limit(anyLong())).thenReturn(jpaQuery);
        when(jpaQuery.fetch()).thenReturn(mockTuples);

        // Mock tuple values
        when(tuple.get(any(Expression.class))).thenReturn(1L, 1L, "testUser", "Test Title", 100, Instant.now(), false, 10L, 5L, null);

        // Mock comment and like counts
        when(commentRepository.findCommentCountsByPostIds(anyList())).thenReturn(commentCounts);

        // Mock fetchLikeCounts (this would be a protected method call)
        // We'll need to test this indirectly through the public method

        // When
        List<SimplePostDTO> result = postPopularRepository.updateRealtimePopularPosts();

        // Then
        assertNotNull(result);
        verify(jpaQueryFactory, times(2)).update(any(QPost.class)); // reset + apply
        verify(jpaQuery, times(1)).fetch();
    }

    @Test
    @DisplayName("실시간 인기글 선정 - 결과 없음")
    void updateRealtimePopularPosts_NoResults() {
        // Given
        when(jpaQueryFactory.update(any(QPost.class))).thenReturn(jpaUpdateClause);
        when(jpaUpdateClause.set(any(Expression.class), any())).thenReturn(jpaUpdateClause);
        when(jpaUpdateClause.where(any(Expression.class))).thenReturn(jpaUpdateClause);
        when(jpaUpdateClause.execute()).thenReturn(0L);

        when(jpaQueryFactory.select(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(jpaQuery);
        when(jpaQuery.from(any(QPost.class))).thenReturn(jpaQuery);
        when(jpaQuery.leftJoin(any(QUsers.class))).thenReturn(jpaQuery);
        when(jpaQuery.on(any(Expression.class))).thenReturn(jpaQuery);
        when(jpaQuery.join(any(QPostLike.class))).thenReturn(jpaQuery);
        when(jpaQuery.leftJoin(any())).thenReturn(jpaQuery);
        when(jpaQuery.where(any(Expression.class))).thenReturn(jpaQuery);
        when(jpaQuery.groupBy(any(Expression.class))).thenReturn(jpaQuery);
        when(jpaQuery.orderBy(any(Expression.class))).thenReturn(jpaQuery);
        when(jpaQuery.limit(anyLong())).thenReturn(jpaQuery);
        when(jpaQuery.fetch()).thenReturn(Collections.emptyList());

        // When
        List<SimplePostDTO> result = postPopularRepository.updateRealtimePopularPosts();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(jpaQueryFactory, times(1)).update(any(QPost.class)); // only reset
        verify(jpaQuery, times(1)).fetch();
    }

    @Test
    @DisplayName("주간 인기글 선정 - 성공")
    void updateWeeklyPopularPosts_Success() {
        // Given
        List<Tuple> mockTuples = Arrays.asList(tuple);
        
        when(jpaQueryFactory.update(any(QPost.class))).thenReturn(jpaUpdateClause);
        when(jpaUpdateClause.set(any(Expression.class), any())).thenReturn(jpaUpdateClause);
        when(jpaUpdateClause.where(any(Expression.class))).thenReturn(jpaUpdateClause);
        when(jpaUpdateClause.execute()).thenReturn(1L);

        when(jpaQueryFactory.select(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(jpaQuery);
        when(jpaQuery.from(any(QPost.class))).thenReturn(jpaQuery);
        when(jpaQuery.leftJoin(any(QUsers.class))).thenReturn(jpaQuery);
        when(jpaQuery.on(any(Expression.class))).thenReturn(jpaQuery);
        when(jpaQuery.join(any(QPostLike.class))).thenReturn(jpaQuery);
        when(jpaQuery.leftJoin(any())).thenReturn(jpaQuery);
        when(jpaQuery.where(any(Expression.class))).thenReturn(jpaQuery);
        when(jpaQuery.groupBy(any(Expression.class))).thenReturn(jpaQuery);
        when(jpaQuery.orderBy(any(Expression.class))).thenReturn(jpaQuery);
        when(jpaQuery.limit(anyLong())).thenReturn(jpaQuery);
        when(jpaQuery.fetch()).thenReturn(mockTuples);

        when(tuple.get(any(Expression.class))).thenReturn(1L, 1L, "testUser", "Test Title", 100, Instant.now(), false, 15L, 8L, null);
        when(commentRepository.findCommentCountsByPostIds(anyList())).thenReturn(Map.of(1L, 8));

        // When
        List<SimplePostDTO> result = postPopularRepository.updateWeeklyPopularPosts();

        // Then
        assertNotNull(result);
        verify(jpaQueryFactory, times(2)).update(any(QPost.class));
        verify(jpaQuery, times(1)).fetch();
    }

    @Test
    @DisplayName("레전드 게시글 선정 - 성공")
    void updateLegendPosts_Success() {
        // Given
        List<Tuple> mockTuples = Arrays.asList(tuple, tuple);
        
        when(jpaQueryFactory.update(any(QPost.class))).thenReturn(jpaUpdateClause);
        when(jpaUpdateClause.set(any(Expression.class), any())).thenReturn(jpaUpdateClause);
        when(jpaUpdateClause.where(any(Expression.class))).thenReturn(jpaUpdateClause);
        when(jpaUpdateClause.execute()).thenReturn(1L);

        when(jpaQueryFactory.select(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(jpaQuery);
        when(jpaQuery.from(any(QPost.class))).thenReturn(jpaQuery);
        when(jpaQuery.leftJoin(any(QUsers.class))).thenReturn(jpaQuery);
        when(jpaQuery.on(any(Expression.class))).thenReturn(jpaQuery);
        when(jpaQuery.join(any(QPostLike.class))).thenReturn(jpaQuery);
        when(jpaQuery.leftJoin(any())).thenReturn(jpaQuery);
        when(jpaQuery.groupBy(any(Expression.class))).thenReturn(jpaQuery);
        when(jpaQuery.having(any(Expression.class))).thenReturn(jpaQuery);
        when(jpaQuery.orderBy(any(Expression.class))).thenReturn(jpaQuery);
        when(jpaQuery.limit(anyLong())).thenReturn(jpaQuery);
        when(jpaQuery.fetch()).thenReturn(mockTuples);

        when(tuple.get(any(Expression.class))).thenReturn(1L, 1L, "testUser", "Legend Post", 200, Instant.now(), false, 25L, 12L, null);
        when(commentRepository.findCommentCountsByPostIds(anyList())).thenReturn(Map.of(1L, 12, 2L, 8));

        // When
        List<SimplePostDTO> result = postPopularRepository.updateLegendPosts();

        // Then
        assertNotNull(result);
        verify(jpaQueryFactory, times(2)).update(any(QPost.class));
        verify(jpaQuery, times(1)).fetch();
        verify(jpaQuery, times(1)).having(any(Expression.class)); // Legend posts use having clause
    }

    @Test
    @DisplayName("레전드 게시글 선정 - 조건에 맞는 게시글 없음")
    void updateLegendPosts_NoQualifiedPosts() {
        // Given
        when(jpaQueryFactory.update(any(QPost.class))).thenReturn(jpaUpdateClause);
        when(jpaUpdateClause.set(any(Expression.class), any())).thenReturn(jpaUpdateClause);
        when(jpaUpdateClause.where(any(Expression.class))).thenReturn(jpaUpdateClause);
        when(jpaUpdateClause.execute()).thenReturn(0L);

        when(jpaQueryFactory.select(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(jpaQuery);
        when(jpaQuery.from(any(QPost.class))).thenReturn(jpaQuery);
        when(jpaQuery.leftJoin(any(QUsers.class))).thenReturn(jpaQuery);
        when(jpaQuery.on(any(Expression.class))).thenReturn(jpaQuery);
        when(jpaQuery.join(any(QPostLike.class))).thenReturn(jpaQuery);
        when(jpaQuery.leftJoin(any())).thenReturn(jpaQuery);
        when(jpaQuery.groupBy(any(Expression.class))).thenReturn(jpaQuery);
        when(jpaQuery.having(any(Expression.class))).thenReturn(jpaQuery);
        when(jpaQuery.orderBy(any(Expression.class))).thenReturn(jpaQuery);
        when(jpaQuery.limit(anyLong())).thenReturn(jpaQuery);
        when(jpaQuery.fetch()).thenReturn(Collections.emptyList());

        // When
        List<SimplePostDTO> result = postPopularRepository.updateLegendPosts();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(jpaQueryFactory, times(1)).update(any(QPost.class)); // only reset
        verify(jpaQuery, times(1)).fetch();
    }

    @Test
    @DisplayName("인기글 플래그 적용 - null/빈 리스트 처리")
    void applyPopularFlag_NullOrEmptyList() {
        // Given - applyPopularFlag는 private이므로 public 메서드를 통해 간접 테스트
        when(jpaQueryFactory.update(any(QPost.class))).thenReturn(jpaUpdateClause);
        when(jpaUpdateClause.set(any(Expression.class), any())).thenReturn(jpaUpdateClause);
        when(jpaUpdateClause.where(any(Expression.class))).thenReturn(jpaUpdateClause);
        when(jpaUpdateClause.execute()).thenReturn(0L);

        when(jpaQueryFactory.select(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(jpaQuery);
        when(jpaQuery.from(any(QPost.class))).thenReturn(jpaQuery);
        when(jpaQuery.leftJoin(any(QUsers.class))).thenReturn(jpaQuery);
        when(jpaQuery.on(any(Expression.class))).thenReturn(jpaQuery);
        when(jpaQuery.join(any(QPostLike.class))).thenReturn(jpaQuery);
        when(jpaQuery.leftJoin(any())).thenReturn(jpaQuery);
        when(jpaQuery.where(any(Expression.class))).thenReturn(jpaQuery);
        when(jpaQuery.groupBy(any(Expression.class))).thenReturn(jpaQuery);
        when(jpaQuery.orderBy(any(Expression.class))).thenReturn(jpaQuery);
        when(jpaQuery.limit(anyLong())).thenReturn(jpaQuery);
        when(jpaQuery.fetch()).thenReturn(Collections.emptyList());

        // When
        List<SimplePostDTO> result = postPopularRepository.updateRealtimePopularPosts();

        // Then - 빈 결과의 경우 applyPopularFlag가 호출되지 않아야 함 (null/빈 리스트 처리 확인)
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(jpaQueryFactory, times(1)).update(any(QPost.class)); // only resetPopularFlag
    }

    @Test
    @DisplayName("실시간 인기글 선정 - 데이터베이스 오류")
    void updateRealtimePopularPosts_DatabaseError() {
        // Given
        when(jpaQueryFactory.update(any(QPost.class))).thenReturn(jpaUpdateClause);
        when(jpaUpdateClause.set(any(Expression.class), any())).thenReturn(jpaUpdateClause);
        when(jpaUpdateClause.where(any(Expression.class))).thenReturn(jpaUpdateClause);
        when(jpaUpdateClause.execute()).thenThrow(new RuntimeException("Database connection error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> 
            postPopularRepository.updateRealtimePopularPosts()
        );
        
        verify(jpaQueryFactory, times(1)).update(any(QPost.class));
    }

    @Test
    @DisplayName("주간 인기글 선정 - 조회 중 오류")
    void updateWeeklyPopularPosts_QueryError() {
        // Given
        when(jpaQueryFactory.update(any(QPost.class))).thenReturn(jpaUpdateClause);
        when(jpaUpdateClause.set(any(Expression.class), any())).thenReturn(jpaUpdateClause);
        when(jpaUpdateClause.where(any(Expression.class))).thenReturn(jpaUpdateClause);
        when(jpaUpdateClause.execute()).thenReturn(1L);

        when(jpaQueryFactory.select(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(jpaQuery);
        when(jpaQuery.from(any(QPost.class))).thenReturn(jpaQuery);
        when(jpaQuery.leftJoin(any(QUsers.class))).thenReturn(jpaQuery);
        when(jpaQuery.on(any(Expression.class))).thenReturn(jpaQuery);
        when(jpaQuery.join(any(QPostLike.class))).thenReturn(jpaQuery);
        when(jpaQuery.leftJoin(any())).thenReturn(jpaQuery);
        when(jpaQuery.where(any(Expression.class))).thenReturn(jpaQuery);
        when(jpaQuery.groupBy(any(Expression.class))).thenReturn(jpaQuery);
        when(jpaQuery.orderBy(any(Expression.class))).thenReturn(jpaQuery);
        when(jpaQuery.limit(anyLong())).thenReturn(jpaQuery);
        when(jpaQuery.fetch()).thenThrow(new RuntimeException("Query execution failed"));

        // When & Then
        assertThrows(RuntimeException.class, () -> 
            postPopularRepository.updateWeeklyPopularPosts()
        );
        
        verify(jpaQueryFactory, times(1)).update(any(QPost.class)); // resetPopularFlag called
        verify(jpaQuery, times(1)).fetch();
    }
}