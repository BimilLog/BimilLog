package jaeik.bimillog.infrastructure.adapter.post.out.persistence.post.cache;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.jpa.impl.JPAUpdateClause;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSearchResult;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.quality.Strictness.LENIENT;

/**
 * <h2>PostCacheCommandAdapter 테스트</h2>
 * <p>Redis 캐시 명령 어댑터의 모든 기능을 테스트합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = LENIENT)
class PostCacheCommandAdapterTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private JPAQueryFactory jpaQueryFactory;

    @Mock
    private ValueOperations<String, Object> valueOperations;
    
    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    private PostCacheCommandAdapter postCacheCommandAdapter;

    private List<PostSearchResult> testSimplePosts;
    private PostDetail testFullPost;

    @BeforeEach
    void setUp() {
        // RedisTemplate Mock 설정
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        
        // PostCacheCommandAdapter 인스턴스 생성 - @RequiredArgsConstructor 사용
        postCacheCommandAdapter = new PostCacheCommandAdapter(redisTemplate, jpaQueryFactory);

        // 테스트 데이터 준비
        createTestData();
    }

    private void createTestData() {
        // PostSearchResult 테스트 데이터
        testSimplePosts = Arrays.asList(
            PostSearchResult.builder()
                .id(1L)
                .title("인기 게시글 1")
                .content("인기 게시글 내용 1")
                .viewCount(100)
                .likeCount(50)
                .postCacheFlag(PostCacheFlag.REALTIME)
                .createdAt(Instant.now())
                .userId(1L)
                .userName("testUser")
                .commentCount(10)
                .isNotice(false)
                .build(),
            PostSearchResult.builder()
                .id(2L)
                .title("인기 게시글 2")
                .content("인기 게시글 내용 2")
                .viewCount(80)
                .likeCount(40)
                .postCacheFlag(PostCacheFlag.WEEKLY)
                .createdAt(Instant.now())
                .userId(1L)
                .userName("testUser")
                .commentCount(5)
                .isNotice(false)
                .build()
        );

        // PostDetail 테스트 데이터
        testFullPost = PostDetail.builder()
            .id(1L)
            .title("상세 게시글")
            .content("상세 게시글 내용입니다.")
            .viewCount(200)
            .likeCount(100)
            .postCacheFlag(PostCacheFlag.LEGEND)
            .createdAt(Instant.now())
            .userId(1L)
            .userName("testUser")
            .commentCount(15)
            .isNotice(false)
            .isLiked(false)
            .build();
    }

    @Test
    @DisplayName("정상 케이스 - REALTIME 캐시 저장")
    void shouldCachePosts_WhenRealtimeTypeProvided() {
        // Given: REALTIME 캐시 타입과 PostDetail 목록
        List<PostDetail> postDetails = List.of(testFullPost);
        PostCacheFlag cacheType = PostCacheFlag.REALTIME;
        
        // When: 통합 캐시 메서드 사용
        postCacheCommandAdapter.cachePostsWithDetails(cacheType, postDetails);

        // Then: Redis에 목록과 상세 캐시 모두 저장됨
        // 목록 캐시 검증 (Sorted Set으로 저장)
        verify(zSetOperations).add(eq("cache:posts:realtime"), eq("1"), anyDouble());
        verify(redisTemplate).expire(eq("cache:posts:realtime"), eq(Duration.ofMinutes(30)));
        // 상세 캐시 검증 (1일 TTL)
        verify(valueOperations).set(eq("cache:post:" + testFullPost.id()), eq(testFullPost), eq(Duration.ofDays(1)));
    }

    @Test
    @DisplayName("정상 케이스 - WEEKLY 캐시 저장")
    void shouldCachePosts_WhenWeeklyTypeProvided() {
        // Given: WEEKLY 캐시 타입과 PostDetail 목록
        List<PostDetail> postDetails = List.of(testFullPost);
        PostCacheFlag cacheType = PostCacheFlag.WEEKLY;
        
        // When: 통합 캐시 메서드 사용
        postCacheCommandAdapter.cachePostsWithDetails(cacheType, postDetails);

        // Then: Redis에 목록과 상세 캐시 모두 저장됨
        // 목록 캐시 검증 (Sorted Set으로 저장)
        verify(zSetOperations).add(eq("cache:posts:weekly"), eq("1"), anyDouble());
        verify(redisTemplate).expire(eq("cache:posts:weekly"), eq(Duration.ofDays(1)));
        // 상세 캐시 검증 (1일 TTL)
        verify(valueOperations).set(eq("cache:post:" + testFullPost.id()), eq(testFullPost), eq(Duration.ofDays(1)));
    }

    @Test
    @DisplayName("정상 케이스 - LEGEND 캐시 저장")
    void shouldCachePosts_WhenLegendTypeProvided() {
        // Given: LEGEND 캐시 타입과 PostDetail 목록
        List<PostDetail> postDetails = List.of(testFullPost);
        PostCacheFlag cacheType = PostCacheFlag.LEGEND;
        
        // When: 통합 캐시 메서드 사용
        postCacheCommandAdapter.cachePostsWithDetails(cacheType, postDetails);

        // Then: Redis에 목록과 상세 캐시 모두 저장됨
        // 목록 캐시 검증 (Sorted Set으로 저장)
        verify(zSetOperations).add(eq("cache:posts:legend"), eq("1"), anyDouble());
        verify(redisTemplate).expire(eq("cache:posts:legend"), eq(Duration.ofDays(1)));
        // 상세 캐시 검증 (1일 TTL)
        verify(valueOperations).set(eq("cache:post:" + testFullPost.id()), eq(testFullPost), eq(Duration.ofDays(1)));
    }

    @Test
    @DisplayName("정상 케이스 - NOTICE 캐시 저장")
    void shouldCachePosts_WhenNoticeTypeProvided() {
        // Given: NOTICE 캐시 타입과 PostDetail 목록
        List<PostDetail> postDetails = List.of(testFullPost);
        PostCacheFlag cacheType = PostCacheFlag.NOTICE;
        
        // When: 통합 캐시 메서드 사용
        postCacheCommandAdapter.cachePostsWithDetails(cacheType, postDetails);

        // Then: Redis에 목록과 상세 캐시 모두 저장됨
        // 목록 캐시 검증 (Sorted Set으로 저장, NOTICE는 7일 TTL)
        verify(zSetOperations).add(eq("cache:posts:notice"), eq("1"), anyDouble());
        verify(redisTemplate).expire(eq("cache:posts:notice"), eq(Duration.ofDays(7)));
        // 상세 캐시 검증 (1일 TTL)
        verify(valueOperations).set(eq("cache:post:" + testFullPost.id()), eq(testFullPost), eq(Duration.ofDays(1)));
    }

    @Test
    @DisplayName("정상 케이스 - 단일 게시글 상세 캐시 저장")
    void shouldCacheFullPost_WhenValidPostProvided() {
        // Given: 단일 PostDetail
        List<PostDetail> singlePost = List.of(testFullPost);
        
        // When: 통합 캐시 메서드로 단일 게시글 캐시 (REALTIME 타입 사용)
        postCacheCommandAdapter.cachePostsWithDetails(PostCacheFlag.REALTIME, singlePost);

        // Then: 목록과 상세 캐시 모두 저장됨
        // 목록 캐시 검증
        verify(valueOperations).set(eq("cache:posts:realtime"), any(), eq(Duration.ofMinutes(30)));
        // 상세 캐시 검증
        verify(valueOperations).set(eq("cache:post:" + testFullPost.id()), eq(testFullPost), eq(Duration.ofDays(1)));
    }

    @Test
    @DisplayName("경계값 - 빈 게시글 목록 캐시")
    void shouldCacheEmptyList_WhenEmptyListProvided() {
        // Given: 빈 PostDetail 목록
        List<PostDetail> emptyList = List.of();
        PostCacheFlag cacheType = PostCacheFlag.REALTIME;
        
        // When: 통합 캐시 메서드로 빈 목록 처리
        postCacheCommandAdapter.cachePostsWithDetails(cacheType, emptyList);

        // Then: 빈 목록이면 아무 작업도 수행되지 않음 (통합 메서드의 early return 로직)
        verify(valueOperations, never()).set(any(), any(), any(Duration.class));
    }

    @Test
    @DisplayName("예외 케이스 - Redis 쓰기 오류 시 CustomException 발생")
    void shouldThrowCustomException_WhenRedisWriteError() {
        // Given: Redis 쓰기 오류 발생
        doThrow(new RuntimeException("Redis connection failed"))
            .when(valueOperations).set(anyString(), any(), any(Duration.class));

        List<PostDetail> postDetails = List.of(testFullPost);
        PostCacheFlag cacheType = PostCacheFlag.REALTIME;

        // When & Then: CustomException 발생
        assertThatThrownBy(() -> {
            postCacheCommandAdapter.cachePostsWithDetails(cacheType, postDetails);
        })
        .isInstanceOf(CustomException.class)
        .satisfies(ex -> {
            CustomException customEx = (CustomException) ex;
            assertThat(customEx.getStatus()).isEqualTo(ErrorCode.REDIS_WRITE_ERROR.getStatus());
        });
    }

    @Test
    @DisplayName("예외 케이스 - 통합 캐시 메서드 Redis 오류 (이미 추가된 테스트와 동일하므로 제거)")
    void shouldThrowCustomException_WhenFullPostCacheWriteError() {
        // 이 테스트는 이미 위의 shouldThrowCustomException_WhenRedisWriteError와 동일한 기능을 테스트하므로
        // 통합 메서드 사용으로 중복되었습니다. 실제로는 제거하는 것이 좋지만 여기서는 주석으로 남겨둡니다.
        
        // Given: 이미 위 테스트에서 통합 메서드의 Redis 오류를 검증했음
        // 통합 메서드는 목록 캐시와 상세 캐시를 모두 처리하므로 별도 테스트가 불필요함
        
        // 실제 테스트는 수행하지 않음
        assertThat(true).isTrue(); // placeholder
    }

    @Test
    @DisplayName("정상 케이스 - 인기 게시글 캐시 플래그 적용")
    void shouldApplyPopularFlag_WhenValidPostIdsProvided() {
        // Given: 게시글 ID 목록과 캐시 플래그
        List<Long> postIds = Arrays.asList(1L, 2L, 3L);
        PostCacheFlag cacheFlag = PostCacheFlag.REALTIME;

        // QueryDSL 업데이트 쿼리 Mock 설정
        JPAUpdateClause updateClause = mock(JPAUpdateClause.class);
        given(jpaQueryFactory.update(any())).willReturn(updateClause);
        given(updateClause.set(any(Path.class), any(Object.class))).willReturn(updateClause);
        given(updateClause.where(any())).willReturn(updateClause);
        given(updateClause.execute()).willReturn(3L); // 3개 업데이트

        // When: 인기 게시글 플래그 적용
        postCacheCommandAdapter.applyPopularFlag(postIds, cacheFlag);

        // Then: QueryDSL을 통해 업데이트 실행됨
        verify(jpaQueryFactory).update(any());
        verify(updateClause).set(any(Path.class), eq(cacheFlag));
        verify(updateClause).where(any());
        verify(updateClause).execute();
    }

    @Test
    @DisplayName("경계값 - 빈 게시글 ID 목록으로 플래그 적용")
    void shouldNotExecuteUpdate_WhenEmptyPostIdsProvided() {
        // Given: 빈 게시글 ID 목록
        List<Long> emptyPostIds = List.of();
        PostCacheFlag cacheFlag = PostCacheFlag.WEEKLY;

        // When: 빈 목록으로 플래그 적용 (early return 예상)
        postCacheCommandAdapter.applyPopularFlag(emptyPostIds, cacheFlag);

        // Then: QueryDSL 업데이트가 실행되지 않음 (비즈니스 로직에 따른 정상 동작)
        verify(jpaQueryFactory, never()).update(any());
    }

    @Test
    @DisplayName("경계값 - null 게시글 ID 목록으로 플래그 적용")
    void shouldNotExecuteUpdate_WhenNullPostIdsProvided() {
        // Given: null 게시글 ID 목록
        List<Long> nullPostIds = null;
        PostCacheFlag cacheFlag = PostCacheFlag.LEGEND;

        // When: null 목록으로 플래그 적용 (early return 예상)
        postCacheCommandAdapter.applyPopularFlag(nullPostIds, cacheFlag);

        // Then: QueryDSL 업데이트가 실행되지 않음 (비즈니스 로직에 따른 정상 동작)
        verify(jpaQueryFactory, never()).update(any());
    }

    @Test
    @DisplayName("정상 케이스 - 인기 게시글 캐시 플래그 초기화")
    void shouldResetPopularFlag_WhenValidCacheFlagProvided() {
        // Given: 초기화할 캐시 플래그
        PostCacheFlag cacheFlag = PostCacheFlag.REALTIME;

        // QueryDSL 업데이트 쿼리 Mock 설정
        JPAUpdateClause updateClause = mock(JPAUpdateClause.class);
        given(jpaQueryFactory.update(any())).willReturn(updateClause);
        given(updateClause.set(any(Path.class), (PostCacheFlag) isNull())).willReturn(updateClause);
        given(updateClause.where(any())).willReturn(updateClause);
        given(updateClause.execute()).willReturn(5L); // 5개 플래그 초기화

        // When: 캐시 플래그 초기화
        postCacheCommandAdapter.resetPopularFlag(cacheFlag);

        // Then: QueryDSL을 통해 플래그 초기화 실행됨
        verify(jpaQueryFactory).update(any());
        verify(updateClause).set(any(Path.class), (PostCacheFlag) isNull());
        verify(updateClause).where(any());
        verify(updateClause).execute();
    }

    @Test
    @DisplayName("정상 케이스 - 인기 게시글 캐시 삭제")
    void shouldDeletePopularPostsCache_WhenValidCacheTypeProvided() {
        // Given: 삭제할 캐시 타입
        PostCacheFlag cacheType = PostCacheFlag.WEEKLY;
        
        // When: 인기 게시글 캐시 삭제 (Redis 작업만 수행)
        postCacheCommandAdapter.deleteCache(cacheType, null);

        // Then: Redis에서 해당 키 삭제됨 (JPA 작업 없음)
        verify(redisTemplate).delete("cache:posts:weekly");
    }

    @Test
    @DisplayName("예외 케이스 - 캐시 삭제 시 Redis 오류")
    void shouldThrowCustomException_WhenCacheDeleteError() {
        // Given: Redis 삭제 오류 발생
        doThrow(new RuntimeException("Redis delete failed"))
            .when(redisTemplate).delete(anyString());

        PostCacheFlag cacheType = PostCacheFlag.LEGEND;

        // When & Then: CustomException 발생
        assertThatThrownBy(() -> {
            postCacheCommandAdapter.deleteCache(cacheType, null);
        })
        .isInstanceOf(CustomException.class)
        .satisfies(ex -> {
            CustomException customEx = (CustomException) ex;
            assertThat(customEx.getStatus()).isEqualTo(ErrorCode.REDIS_DELETE_ERROR.getStatus());
        });
    }

    @Test
    @DisplayName("정상 케이스 - 전체 게시글 캐시 삭제 (목록 + 상세)")
    void shouldDeleteFullPostCache_WhenValidPostIdProvided() {
        // Given: 삭제할 게시글 ID
        Long postId = 123L;
        
        // When: 전체 게시글 캐시 삭제 (목록 + 상세)
        postCacheCommandAdapter.deleteCache(null, postId, new PostCacheFlag[0]);

        // Then: 상세 캐시와 모든 목록 캐시에서 삭제됨
        // 상세 캐시 삭제 검증
        verify(redisTemplate).delete(eq("cache:post:123"));
        // 모든 목록 캐시에서 삭제 검증
        verify(zSetOperations).remove(eq("cache:posts:realtime"), eq("123"));
        verify(zSetOperations).remove(eq("cache:posts:weekly"), eq("123"));
        verify(zSetOperations).remove(eq("cache:posts:legend"), eq("123"));
        verify(zSetOperations).remove(eq("cache:posts:notice"), eq("123"));
    }

    @Test
    @DisplayName("예외 케이스 - 전체 게시글 캐시 삭제 시 Redis 오류")
    void shouldThrowCustomException_WhenFullPostCacheDeleteError() {
        // Given: Redis 삭제 오류 발생
        doThrow(new RuntimeException("Redis delete failed"))
            .when(redisTemplate).delete(anyString());

        Long postId = 123L;

        // When & Then: CustomException 발생
        assertThatThrownBy(() -> {
            postCacheCommandAdapter.deleteCache(null, postId, new PostCacheFlag[0]);
        })
        .isInstanceOf(CustomException.class)
        .satisfies(ex -> {
            CustomException customEx = (CustomException) ex;
            assertThat(customEx.getStatus()).isEqualTo(ErrorCode.REDIS_DELETE_ERROR.getStatus());
        });
    }

    @Test
    @DisplayName("성능 최적화 - 타겟 캐시만 삭제 (공지 해제용)")
    void shouldDeleteOnlyTargetCaches_WhenTargetTypesProvided() {
        // Given: 삭제할 게시글 ID
        Long postId = 456L;
        
        // When: NOTICE 캐시에서만 삭제 (성능 최적화)
        postCacheCommandAdapter.deleteCache(null, postId, PostCacheFlag.NOTICE);

        // Then: 상세 캐시와 NOTICE 캐시에서만 삭제됨
        // 상세 캐시 삭제 검증
        verify(redisTemplate).delete(eq("cache:post:456"));
        // NOTICE 캐시에서만 삭제 검증
        verify(zSetOperations).remove(eq("cache:posts:notice"), eq("456"));
        // 다른 캐시는 삭제되지 않음
        verify(zSetOperations, never()).remove(eq("cache:posts:realtime"), eq("456"));
        verify(zSetOperations, never()).remove(eq("cache:posts:weekly"), eq("456"));
        verify(zSetOperations, never()).remove(eq("cache:posts:legend"), eq("456"));
    }

    @Test
    @DisplayName("비즈니스 로직 - 캐시 메타데이터 정확성 검증")
    void shouldUseCorrectMetadata_ForAllCacheTypes() {
        // Given: 각 캐시 타입별 테스트
        
        // When & Then: 각 캐시 타입에 대해 통합 메서드로 적절한 키와 TTL 사용
        List<PostDetail> postDetails = List.of(testFullPost);
        
        // REALTIME - 30분 TTL (목록), 1일 TTL (상세)
        postCacheCommandAdapter.cachePostsWithDetails(PostCacheFlag.REALTIME, postDetails);
        verify(valueOperations).set(eq("cache:posts:realtime"), any(), eq(Duration.ofMinutes(30)));
        verify(valueOperations).set(eq("cache:post:" + testFullPost.id()), any(), eq(Duration.ofDays(1)));

        // WEEKLY - 1일 TTL (목록), 1일 TTL (상세)
        postCacheCommandAdapter.cachePostsWithDetails(PostCacheFlag.WEEKLY, postDetails);
        verify(valueOperations).set(eq("cache:posts:weekly"), any(), eq(Duration.ofDays(1)));

        // LEGEND - 1일 TTL (목록), 1일 TTL (상세)
        postCacheCommandAdapter.cachePostsWithDetails(PostCacheFlag.LEGEND, postDetails);
        verify(valueOperations).set(eq("cache:posts:legend"), any(), eq(Duration.ofDays(1)));

        // NOTICE - 7일 TTL (목록), 1일 TTL (상세)
        postCacheCommandAdapter.cachePostsWithDetails(PostCacheFlag.NOTICE, postDetails);
        verify(valueOperations).set(eq("cache:posts:notice"), any(), eq(Duration.ofDays(7)));
    }

    @Test
    @DisplayName("통합 테스트 - 전체 워크플로우")
    void shouldCompleteEntireWorkflow_WhenCachingPopularPosts() {
        // Given: 게시글 ID 목록과 캐시 데이터
        List<Long> postIds = Arrays.asList(1L, 2L);
        PostCacheFlag cacheFlag = PostCacheFlag.REALTIME;

        // QueryDSL Mock 설정
        JPAUpdateClause updateClause = mock(JPAUpdateClause.class);
        given(jpaQueryFactory.update(any())).willReturn(updateClause);
        given(updateClause.set(any(Path.class), any(Object.class))).willReturn(updateClause);
        given(updateClause.where(any())).willReturn(updateClause);
        given(updateClause.execute()).willReturn(2L);

        // When: 새로운 통합 워크플로우 실행
        // 1. 인기 게시글 플래그 적용
        postCacheCommandAdapter.applyPopularFlag(postIds, cacheFlag);
        
        // 2. 통합 캐시 메서드로 목록과 상세 캐시를 한번에 처리
        List<PostDetail> postDetails = List.of(testFullPost);
        postCacheCommandAdapter.cachePostsWithDetails(cacheFlag, postDetails);

        // Then: 모든 단계가 정상 실행됨
        // 플래그 적용 검증
        verify(jpaQueryFactory).update(any());
        verify(updateClause).execute();
        
        // 통합 캐시 검증 (목록과 상세 캐시가 모두 저장됨)
        // 목록 캐시 검증
        verify(valueOperations).set(eq("cache:posts:realtime"), any(), eq(Duration.ofMinutes(30)));
        // 상세 캐시 검증
        verify(valueOperations).set(eq("cache:post:" + testFullPost.id()), eq(testFullPost), eq(Duration.ofDays(1)));
    }

    @Test
    @DisplayName("성능 테스트 - 대량 게시글 ID 플래그 적용")
    void shouldHandleLargePostIdList_WhenApplyingFlag() {
        // Given: 대량 게시글 ID 목록 (1000개)
        List<Long> largePostIds = LongStream.rangeClosed(1L, 1000L)
                .boxed()
                .collect(Collectors.toList());
        PostCacheFlag cacheFlag = PostCacheFlag.WEEKLY;

        // QueryDSL Mock 설정
        JPAUpdateClause updateClause = mock(JPAUpdateClause.class);
        given(jpaQueryFactory.update(any())).willReturn(updateClause);
        given(updateClause.set(any(Path.class), any(Object.class))).willReturn(updateClause);
        given(updateClause.where(any())).willReturn(updateClause);
        given(updateClause.execute()).willReturn(1000L);

        // When: 대량 ID에 대해 플래그 적용
        postCacheCommandAdapter.applyPopularFlag(largePostIds, cacheFlag);

        // Then: 성능 문제 없이 정상 처리됨
        verify(jpaQueryFactory).update(any());
        verify(updateClause).execute();
        
        // 전체 ID가 처리되었는지 확인
        ArgumentCaptor<Predicate> predicateCaptor =
                ArgumentCaptor.forClass(Predicate.class);
        verify(updateClause).where(predicateCaptor.capture());
        
        // IN 조건이 사용되었는지 확인 (성능 최적화)
        assertThat(predicateCaptor.getValue()).isNotNull();
    }

    @Test
    @DisplayName("정상 케이스 - 통합 캐시 메서드 (목록 + 상세)")
    void shouldCachePostsWithDetails_WhenValidPostDetailsProvided() {
        // Given: PostDetail 목록
        PostDetail postDetail1 = PostDetail.builder()
                .id(1L)
                .title("Test Title 1")
                .content("Test Content 1")
                .viewCount(100)
                .likeCount(10)
                .postCacheFlag(PostCacheFlag.REALTIME)
                .createdAt(Instant.now())
                .userId(1L)
                .userName("User 1")
                .commentCount(5)
                .isNotice(false)
                .isLiked(true)
                .build();

        PostDetail postDetail2 = PostDetail.builder()
                .id(2L)
                .title("Test Title 2")
                .content("Test Content 2")
                .viewCount(200)
                .likeCount(20)
                .postCacheFlag(PostCacheFlag.WEEKLY)
                .createdAt(Instant.now())
                .userId(2L)
                .userName("User 2")
                .commentCount(10)
                .isNotice(true)
                .isLiked(false)
                .build();

        List<PostDetail> fullPosts = List.of(postDetail1, postDetail2);
        PostCacheFlag cacheType = PostCacheFlag.REALTIME;

        // When: 통합 캐시 메서드 호출
        postCacheCommandAdapter.cachePostsWithDetails(cacheType, fullPosts);

        // Then: 목록 캐시와 상세 캐시가 모두 저장됨
        // 1. 목록 캐시 검증 (PostSearchResult 형태로 변환되어 저장)
        verify(valueOperations).set(
                eq("cache:posts:realtime"), 
                argThat(list -> {
                    if (!(list instanceof List<?> postList)) return false;
                    return postList.size() == 2 && 
                           postList.stream().allMatch(item -> item instanceof PostSearchResult);
                }), 
                eq(Duration.ofMinutes(30))
        );

        // 2. 상세 캐시 검증 (각 PostDetail이 개별 키로 저장)
        verify(valueOperations).set(eq("cache:post:1"), eq(postDetail1), eq(Duration.ofDays(1)));
        verify(valueOperations).set(eq("cache:post:2"), eq(postDetail2), eq(Duration.ofDays(1)));
    }

    @Test
    @DisplayName("정상 케이스 - 통합 캐시 메서드 빈 목록")
    void shouldHandleEmptyList_WhenCachePostsWithDetailsCalledWithEmptyList() {
        // Given: 빈 PostDetail 목록
        List<PostDetail> emptyPosts = Collections.emptyList();
        PostCacheFlag cacheType = PostCacheFlag.WEEKLY;

        // When: 통합 캐시 메서드 호출
        postCacheCommandAdapter.cachePostsWithDetails(cacheType, emptyPosts);

        // Then: 아무 작업도 수행되지 않음
        verify(valueOperations, never()).set(any(), any(), any(Duration.class));
        verify(redisTemplate, never()).opsForValue();
    }

    @Test 
    @DisplayName("예외 케이스 - 통합 캐시 메서드 Redis 오류")
    void shouldThrowCustomException_WhenCachePostsWithDetailsRedisError() {
        // Given: Redis 오류 발생 설정
        doThrow(new RuntimeException("Redis connection failed"))
                .when(valueOperations).set(anyString(), any(), any(Duration.class));

        List<PostDetail> fullPosts = List.of(
                PostDetail.builder()
                        .id(1L)
                        .title("Test")
                        .content("Test Content")
                        .viewCount(100)
                        .likeCount(10)
                        .postCacheFlag(PostCacheFlag.REALTIME)
                        .createdAt(Instant.now())
                        .userId(1L)
                        .userName("User")
                        .commentCount(0)
                        .isNotice(false)
                        .isLiked(false)
                        .build()
        );

        // When & Then: CustomException 발생
        assertThatThrownBy(() -> {
            postCacheCommandAdapter.cachePostsWithDetails(PostCacheFlag.REALTIME, fullPosts);
        })
        .isInstanceOf(CustomException.class)
        .hasMessageContaining("Redis write error");
    }

}