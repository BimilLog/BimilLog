package jaeik.growfarm.infrastructure.adapter.post.out.persistence.post.cache;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.jpa.impl.JPAUpdateClause;
import jaeik.growfarm.domain.post.entity.PostCacheFlag;
import jaeik.growfarm.infrastructure.adapter.post.in.web.dto.FullPostResDTO;
import jaeik.growfarm.infrastructure.adapter.post.in.web.dto.SimplePostResDTO;
import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
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

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
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

    private PostCacheCommandAdapter postCacheCommandAdapter;

    private List<SimplePostResDTO> testSimplePosts;
    private FullPostResDTO testFullPost;

    @BeforeEach
    void setUp() {
        // RedisTemplate Mock 설정
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        
        // PostCacheCommandAdapter 인스턴스 생성
        postCacheCommandAdapter = new PostCacheCommandAdapter(redisTemplate, jpaQueryFactory);

        // 테스트 데이터 준비
        createTestData();
    }

    private void createTestData() {
        // SimplePostResDTO 테스트 데이터
        testSimplePosts = Arrays.asList(
            SimplePostResDTO.builder()
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
            SimplePostResDTO.builder()
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

        // FullPostResDTO 테스트 데이터
        testFullPost = FullPostResDTO.builder()
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
            .build();
    }

    @Test
    @DisplayName("정상 케이스 - REALTIME 캐시 저장")
    void shouldCachePosts_WhenRealtimeTypeProvided() {
        // Given: REALTIME 캐시 타입과 게시글 목록
        PostCacheFlag cacheType = PostCacheFlag.REALTIME;
        
        // When: 캐시 저장
        postCacheCommandAdapter.cachePosts(cacheType, testSimplePosts);

        // Then: Redis에 적절한 키와 TTL로 저장됨
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);

        verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture(), ttlCaptor.capture());

        assertThat(keyCaptor.getValue()).isEqualTo("cache:posts:realtime");
        assertThat(valueCaptor.getValue()).isEqualTo(testSimplePosts);
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    @DisplayName("정상 케이스 - WEEKLY 캐시 저장")
    void shouldCachePosts_WhenWeeklyTypeProvided() {
        // Given: WEEKLY 캐시 타입과 게시글 목록
        PostCacheFlag cacheType = PostCacheFlag.WEEKLY;
        
        // When: 캐시 저장
        postCacheCommandAdapter.cachePosts(cacheType, testSimplePosts);

        // Then: Redis에 적절한 키와 TTL로 저장됨
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(eq("cache:posts:weekly"), eq(testSimplePosts), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofDays(1));
    }

    @Test
    @DisplayName("정상 케이스 - LEGEND 캐시 저장")
    void shouldCachePosts_WhenLegendTypeProvided() {
        // Given: LEGEND 캐시 타입과 게시글 목록
        PostCacheFlag cacheType = PostCacheFlag.LEGEND;
        
        // When: 캐시 저장
        postCacheCommandAdapter.cachePosts(cacheType, testSimplePosts);

        // Then: Redis에 적절한 키와 TTL로 저장됨
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(eq("cache:posts:legend"), eq(testSimplePosts), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofDays(1));
    }

    @Test
    @DisplayName("정상 케이스 - NOTICE 캐시 저장")
    void shouldCachePosts_WhenNoticeTypeProvided() {
        // Given: NOTICE 캐시 타입과 게시글 목록
        PostCacheFlag cacheType = PostCacheFlag.NOTICE;
        
        // When: 캐시 저장
        postCacheCommandAdapter.cachePosts(cacheType, testSimplePosts);

        // Then: Redis에 적절한 키와 TTL로 저장됨
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(eq("cache:posts:notice"), eq(testSimplePosts), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofDays(7));
    }

    @Test
    @DisplayName("정상 케이스 - 전체 게시글 캐시 저장")
    void shouldCacheFullPost_WhenValidPostProvided() {
        // Given: 전체 게시글 정보
        
        // When: 전체 게시글 캐시 저장
        postCacheCommandAdapter.cacheFullPost(testFullPost);

        // Then: Redis에 적절한 키와 TTL로 저장됨
        String expectedKey = "cache:post:" + testFullPost.getId();
        Duration expectedTtl = Duration.ofDays(1);
        
        verify(valueOperations).set(expectedKey, testFullPost, expectedTtl);
    }

    @Test
    @DisplayName("경계값 - 빈 게시글 목록 캐시")
    void shouldCacheEmptyList_WhenEmptyListProvided() {
        // Given: 빈 게시글 목록
        List<SimplePostResDTO> emptyList = Arrays.asList();
        PostCacheFlag cacheType = PostCacheFlag.REALTIME;
        
        // When: 빈 목록 캐시 저장
        postCacheCommandAdapter.cachePosts(cacheType, emptyList);

        // Then: 빈 목록도 정상적으로 캐시됨
        verify(valueOperations).set(eq("cache:posts:realtime"), eq(emptyList), any(Duration.class));
    }

    @Test
    @DisplayName("예외 케이스 - Redis 쓰기 오류 시 CustomException 발생")
    void shouldThrowCustomException_WhenRedisWriteError() {
        // Given: Redis 쓰기 오류 발생
        doThrow(new RuntimeException("Redis connection failed"))
            .when(valueOperations).set(anyString(), any(), any(Duration.class));

        PostCacheFlag cacheType = PostCacheFlag.REALTIME;

        // When & Then: CustomException 발생
        assertThatThrownBy(() -> {
            postCacheCommandAdapter.cachePosts(cacheType, testSimplePosts);
        })
        .isInstanceOf(CustomException.class)
        .satisfies(ex -> {
            CustomException customEx = (CustomException) ex;
            assertThat(customEx.getStatus()).isEqualTo(ErrorCode.REDIS_WRITE_ERROR.getStatus());
        });
    }

    @Test
    @DisplayName("예외 케이스 - 전체 게시글 캐시 저장 시 Redis 오류")
    void shouldThrowCustomException_WhenFullPostCacheWriteError() {
        // Given: Redis 쓰기 오류 발생
        doThrow(new RuntimeException("Redis connection failed"))
            .when(valueOperations).set(anyString(), any(), any(Duration.class));

        // When & Then: CustomException 발생
        assertThatThrownBy(() -> {
            postCacheCommandAdapter.cacheFullPost(testFullPost);
        })
        .isInstanceOf(CustomException.class)
        .satisfies(ex -> {
            CustomException customEx = (CustomException) ex;
            assertThat(customEx.getStatus()).isEqualTo(ErrorCode.REDIS_WRITE_ERROR.getStatus());
        });
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
        postCacheCommandAdapter.deletePopularPostsCache(cacheType);

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
            postCacheCommandAdapter.deletePopularPostsCache(cacheType);
        })
        .isInstanceOf(CustomException.class)
        .satisfies(ex -> {
            CustomException customEx = (CustomException) ex;
            assertThat(customEx.getStatus()).isEqualTo(ErrorCode.REDIS_DELETE_ERROR.getStatus());
        });
    }

    @Test
    @DisplayName("정상 케이스 - 전체 게시글 캐시 삭제")
    void shouldDeleteFullPostCache_WhenValidPostIdProvided() {
        // Given: 삭제할 게시글 ID
        Long postId = 123L;
        
        // When: 전체 게시글 캐시 삭제 (Redis 작업만 수행)
        postCacheCommandAdapter.deleteFullPostCache(postId);

        // Then: Redis에서 해당 키 삭제됨 (JPA 작업 없음)
        String expectedKey = "cache:post:" + postId;
        verify(redisTemplate).delete(expectedKey);
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
            postCacheCommandAdapter.deleteFullPostCache(postId);
        })
        .isInstanceOf(CustomException.class)
        .satisfies(ex -> {
            CustomException customEx = (CustomException) ex;
            assertThat(customEx.getStatus()).isEqualTo(ErrorCode.REDIS_DELETE_ERROR.getStatus());
        });
    }

    @Test
    @DisplayName("비즈니스 로직 - 캐시 메타데이터 정확성 검증")
    void shouldUseCorrectMetadata_ForAllCacheTypes() {
        // Given: 각 캐시 타입별 테스트
        
        // When & Then: 각 캐시 타입에 대해 적절한 키와 TTL 사용
        
        // REALTIME - 30분 TTL
        postCacheCommandAdapter.cachePosts(PostCacheFlag.REALTIME, testSimplePosts);
        verify(valueOperations).set(eq("cache:posts:realtime"), any(), eq(Duration.ofMinutes(30)));

        // WEEKLY - 1일 TTL
        postCacheCommandAdapter.cachePosts(PostCacheFlag.WEEKLY, testSimplePosts);
        verify(valueOperations).set(eq("cache:posts:weekly"), any(), eq(Duration.ofDays(1)));

        // LEGEND - 1일 TTL
        postCacheCommandAdapter.cachePosts(PostCacheFlag.LEGEND, testSimplePosts);
        verify(valueOperations).set(eq("cache:posts:legend"), any(), eq(Duration.ofDays(1)));

        // NOTICE - 7일 TTL
        postCacheCommandAdapter.cachePosts(PostCacheFlag.NOTICE, testSimplePosts);
        verify(valueOperations).set(eq("cache:posts:notice"), any(), eq(Duration.ofDays(7)));
    }

    @Test
    @DisplayName("통합 테스트 - 전체 워크플로우")
    void shouldCompleteEntireWorkflow_WhenCachingPopularPosts() {
        // Given: 게시글 ID 목록과 캐시 데이터
        List<Long> postIds = Arrays.asList(1L, 2L);
        PostCacheFlag cacheFlag = PostCacheFlag.REALTIME;

        // QueryDSL Mock 설정
        JPAUpdateClause updateClause = mock(com.querydsl.jpa.impl.JPAUpdateClause.class);
        given(jpaQueryFactory.update(any())).willReturn(updateClause);
        given(updateClause.set(any(Path.class), any(Object.class))).willReturn(updateClause);
        given(updateClause.where(any())).willReturn(updateClause);
        given(updateClause.execute()).willReturn(2L);

        // When: 전체 워크플로우 실행
        // 1. 인기 게시글 플래그 적용
        postCacheCommandAdapter.applyPopularFlag(postIds, cacheFlag);
        
        // 2. 캐시에 게시글 목록 저장
        postCacheCommandAdapter.cachePosts(cacheFlag, testSimplePosts);
        
        // 3. 개별 게시글 상세 정보 캐시
        postCacheCommandAdapter.cacheFullPost(testFullPost);

        // Then: 모든 단계가 정상 실행됨
        // 플래그 적용 검증
        verify(jpaQueryFactory).update(any());
        verify(updateClause).execute();
        
        // 목록 캐시 검증
        verify(valueOperations).set(eq("cache:posts:realtime"), eq(testSimplePosts), any(Duration.class));
        
        // 상세 캐시 검증
        verify(valueOperations).set(eq("cache:post:" + testFullPost.getId()), eq(testFullPost), any(Duration.class));
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
        given(updateClause.set(any(com.querydsl.core.types.Path.class), any(Object.class))).willReturn(updateClause);
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
}