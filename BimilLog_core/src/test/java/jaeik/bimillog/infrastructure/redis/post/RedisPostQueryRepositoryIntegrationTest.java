package jaeik.bimillog.infrastructure.redis.post;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.testutil.RedisTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>RedisTier1PostStoreAdapter 통합 테스트</h2>
 * <p>로컬 Redis 환경에서 게시글 캐시 조회 어댑터의 핵심 기능을 검증합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("local-integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("local-integration")
class RedisPostQueryRepositoryIntegrationTest {

    @Autowired
    private RedisTier1PostStoreAdapter redisTier1PostStoreAdapter;

    @Autowired
    private RedisDetailPostStoreAdapter redisDetailPostStoreAdapter;

    @Autowired
    private RedisRealTimePostStoreAdapter redisRealTimePostStoreAdapter;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private PostDetail testPostDetail1;
    private PostDetail testPostDetail2;
    private PostDetail testPostDetail3;

    /**
     * <h3>PostDetail을 PostSimpleDetail로 변환</h3>
     */
    private PostSimpleDetail toSimpleDetail(PostDetail detail) {
        return PostSimpleDetail.builder()
                .id(detail.getId())
                .title(detail.getTitle())
                .viewCount(detail.getViewCount())
                .likeCount(detail.getLikeCount())
                .commentCount(detail.getCommentCount())
                .createdAt(detail.getCreatedAt())
                .memberId(detail.getMemberId())
                .memberName(detail.getMemberName())
                .build();
    }



    @BeforeEach
    void setUp() {
        // Redis 초기화
        RedisTestHelper.flushRedis(redisTemplate);

        // 테스트 데이터 준비
        testPostDetail1 = PostDetail.builder()
                .id(1L)
                .title("첫 번째 게시글")
                .content("첫 번째 내용")
                .viewCount(100)
                .likeCount(50)
                .commentCount(10)
                .isLiked(false)
                .createdAt(Instant.now())
                .memberId(1L)
                .memberName("member1")
                .build();

        testPostDetail2 = PostDetail.builder()
                .id(2L)
                .title("두 번째 게시글")
                .content("두 번째 내용")
                .viewCount(200)
                .likeCount(100)
                .commentCount(20)
                .isLiked(false)
                .createdAt(Instant.now())
                .memberId(2L)
                .memberName("member2")
                .build();

        testPostDetail3 = PostDetail.builder()
                .id(3L)
                .title("세 번째 게시글")
                .content("세 번째 내용")
                .viewCount(150)
                .likeCount(75)
                .commentCount(15)
                .isLiked(false)
                .createdAt(Instant.now())
                .memberId(3L)
                .memberName("member3")
                .build();
    }

    @Test
    @DisplayName("정상 케이스 - 캐시된 게시글 상세 조회")
    void shouldReturnPostDetail_WhenCachedPostExists() {
        // Given: RedisTemplate로 직접 저장 (CommandAdapter 의존성 제거)
        String cacheKey = RedisTestHelper.RedisKeys.postDetail(1L);
        redisTemplate.opsForValue().set(cacheKey, testPostDetail1, Duration.ofMinutes(5));

        // When: QueryAdapter로 조회
        PostDetail result = redisDetailPostStoreAdapter.getCachedPostIfExists(1L);

        // Then: 저장한 데이터와 동일한 데이터 조회됨
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("첫 번째 게시글");
        assertThat(result.getContent()).isEqualTo("첫 번째 내용");
        assertThat(result.getViewCount()).isEqualTo(100);
        assertThat(result.getLikeCount()).isEqualTo(50);
        assertThat(result.getCommentCount()).isEqualTo(10);
        assertThat(result.getMemberName()).isEqualTo("member1");
    }

    @Test
    @DisplayName("정상 케이스 - 캐시된 게시글 목록 조회")
    void shouldReturnPostList_WhenCachedListExists() {
        // Given: RedisTemplate로 직접 저장 (CommandAdapter 의존성 제거)
        PostCacheFlag cacheType = PostCacheFlag.REALTIME;
        String hashKey = RedisPostKeys.CACHE_METADATA_MAP.get(cacheType).key();
        String postIdsKey = RedisPostKeys.getPostIdsStorageKey(cacheType);

        // PostSimpleDetail 생성 (PostDetail에서 변환)
        PostSimpleDetail simple1 = toSimpleDetail(testPostDetail1);
        PostSimpleDetail simple2 = toSimpleDetail(testPostDetail2);
        PostSimpleDetail simple3 = toSimpleDetail(testPostDetail3);

        // Hash에 PostSimpleDetail 저장 (Field: postId, Value: PostSimpleDetail)
        redisTemplate.opsForHash().put(hashKey, "1", simple1);
        redisTemplate.opsForHash().put(hashKey, "2", simple2);
        redisTemplate.opsForHash().put(hashKey, "3", simple3);
        redisTemplate.expire(hashKey, Duration.ofMinutes(5));

        // postIds 저장소에 순서 저장 (REALTIME은 Sorted Set 사용 - NOTICE가 아니므로)
        redisTemplate.opsForZSet().add(postIdsKey, "1", 1.0);
        redisTemplate.opsForZSet().add(postIdsKey, "2", 2.0);
        redisTemplate.opsForZSet().add(postIdsKey, "3", 3.0);

        // When: 목록 조회
        List<PostSimpleDetail> result = redisTier1PostStoreAdapter.getCachedPostList(cacheType);

        // Then: 3개의 게시글이 순서대로 조회됨
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(2).getId()).isEqualTo(3L);

        // PostSimpleDetail로 변환되었는지 확인
        assertThat(result.get(0).getTitle()).isEqualTo("첫 번째 게시글");
        assertThat(result.get(0).getViewCount()).isEqualTo(100);
    }

    @Test
    @DisplayName("정상 케이스 - 빈 캐시 목록 조회")
    void shouldReturnEmptyList_WhenNoCachedPostsExist() {
        // Given: 캐시가 없는 상태
        PostCacheFlag cacheType = PostCacheFlag.REALTIME;

        // When: 목록 조회
        List<PostSimpleDetail> result = redisTier1PostStoreAdapter.getCachedPostList(cacheType);

        // Then: 빈 목록 반환
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("경계값 - 캐시되지 않은 게시글 조회 시 null 반환")
    void shouldReturnNull_WhenPostNotCached() {
        // Given: 캐시되지 않은 게시글 ID
        Long nonExistentPostId = 999L;

        // When: 조회 시도
        PostDetail result = redisDetailPostStoreAdapter.getCachedPostIfExists(nonExistentPostId);

        // Then: null 반환
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("정상 케이스 - 일부 게시글만 캐시된 경우 존재하는 것만 조회")
    void shouldReturnOnlyCachedPosts_WhenSomePostsAreMissing() {
        // Given: RedisTemplate로 직접 저장 (postIds는 3개, Hash에는 2개만 저장)
        PostCacheFlag cacheType = PostCacheFlag.WEEKLY;
        String hashKey = RedisPostKeys.CACHE_METADATA_MAP.get(cacheType).key();
        String postIdsKey = RedisPostKeys.getPostIdsStorageKey(cacheType);

        // Hash에 PostSimpleDetail 2개만 저장 (3번은 저장하지 않음)
        redisTemplate.opsForHash().put(hashKey, "1", toSimpleDetail(testPostDetail1));
        redisTemplate.opsForHash().put(hashKey, "2", toSimpleDetail(testPostDetail2));
        redisTemplate.expire(hashKey, Duration.ofMinutes(5));

        // postIds 저장소에는 3개 모두 저장 (WEEKLY는 Sorted Set)
        redisTemplate.opsForZSet().add(postIdsKey, "1", 1.0);
        redisTemplate.opsForZSet().add(postIdsKey, "2", 2.0);
        redisTemplate.opsForZSet().add(postIdsKey, "3", 3.0);

        // When: 목록 조회
        List<PostSimpleDetail> result = redisTier1PostStoreAdapter.getCachedPostList(cacheType);

        // Then: Hash에 있는 2개만 반환 (필터링됨)
        assertThat(result).hasSize(2);
        assertThat(result).extracting("id").containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("정상 케이스 - 레전드 게시글 목록 페이지네이션 조회 (첫 페이지와 두 번째 페이지)")
    void shouldReturnPagedLists_FirstAndSecondPage() {
        RedisTestHelper.flushRedis(redisTemplate);

        // Given: RedisTemplate로 직접 20개의 레전드 게시글 저장 (CommandAdapter 의존성 제거)
        String hashKey = RedisPostKeys.CACHE_METADATA_MAP.get(PostCacheFlag.LEGEND).key();
        String postIdsKey = RedisPostKeys.getPostIdsStorageKey(PostCacheFlag.LEGEND);

        for (long i = 1; i <= 20; i++) {
            // PostDetail 생성
            PostDetail detail = PostDetail.builder()
                    .id(i)
                    .title("게시글 " + i)
                    .content("내용 " + i)
                    .viewCount((int) (i * 10))
                    .likeCount((int) (i * 5))
                    .commentCount((int) i)
                    .isLiked(false)
                    .createdAt(Instant.now())
                    .memberId(i)
                    .memberName("member" + i)
                    .build();

            // Hash에 PostSimpleDetail 저장
            redisTemplate.opsForHash().put(hashKey, String.valueOf(i), toSimpleDetail(detail));

            // postIds 저장소에 순서 저장 (LEGEND는 Sorted Set)
            redisTemplate.opsForZSet().add(postIdsKey, String.valueOf(i), (double) i);
        }
        redisTemplate.expire(hashKey, Duration.ofMinutes(5));

        // When & Then - 케이스 1: 첫 페이지 조회 (페이지 0, 사이즈 10)
        Pageable firstPage = PageRequest.of(0, 10);
        Page<PostSimpleDetail> firstResult = redisTier1PostStoreAdapter.getCachedPostListPaged(firstPage);

        assertThat(firstResult.getContent()).hasSize(10);
        assertThat(firstResult.getTotalElements()).isEqualTo(20);
        assertThat(firstResult.getTotalPages()).isEqualTo(2);
        assertThat(firstResult.getNumber()).isEqualTo(0); // 현재 페이지 번호
        assertThat(firstResult.getContent().get(0).getId()).isEqualTo(1L);
        assertThat(firstResult.getContent().get(9).getId()).isEqualTo(10L);

        // When & Then - 케이스 2: 두 번째 페이지 조회 (페이지 1, 사이즈 10)
        Pageable secondPage = PageRequest.of(1, 10);
        Page<PostSimpleDetail> secondResult = redisTier1PostStoreAdapter.getCachedPostListPaged(secondPage);

        assertThat(secondResult.getContent()).hasSize(10);
        assertThat(secondResult.getTotalElements()).isEqualTo(20);
        assertThat(secondResult.getNumber()).isEqualTo(1); // 두 번째 페이지
        assertThat(secondResult.getContent().get(0).getId()).isEqualTo(11L);
        assertThat(secondResult.getContent().get(9).getId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("경계값 - 빈 레전드 목록 페이지네이션 조회")
    void shouldReturnEmptyPage_WhenNoLegendPostsExist() {
        // Given: 레전드 게시글이 없는 상태
        Pageable pageable = PageRequest.of(0, 10);

        // When: 페이지네이션 조회
        Page<PostSimpleDetail> result = redisTier1PostStoreAdapter.getCachedPostListPaged(pageable);

        // Then: 빈 페이지 반환
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getTotalPages()).isEqualTo(0);
    }

    @Test
    @DisplayName("정상 케이스 - 실시간 인기글 ID 목록 조회 (상위 5개)")
    void shouldReturnTop5PostIds_WhenRealtimeScoresExist() {
        // Given: 10개의 게시글에 점수 설정 (높은 점수부터)
        String scoreKey = RedisPostKeys.REALTIME_POST_SCORE_KEY;
        for (long i = 1; i <= 10; i++) {
            double score = 11.0 - i; // 10, 9, 8, 7, 6, 5, 4, 3, 2, 1
            redisTemplate.opsForZSet().add(scoreKey, String.valueOf(i), score);
        }

        // When: 실시간 인기글 ID 조회 (상위 5개)
        List<Long> result = redisRealTimePostStoreAdapter.getRealtimePopularPostIds();

        // Then: 점수가 높은 상위 5개만 반환
        assertThat(result).hasSize(5);
        assertThat(result).containsExactly(1L, 2L, 3L, 4L, 5L); // 점수: 10, 9, 8, 7, 6
    }

    @Test
    @DisplayName("정상 케이스 - 실시간 인기글 ID 목록 내림차순 정렬 확인")
    void shouldReturnInDescendingOrder_ByScore() {
        // Given: 랜덤 순서로 점수 설정
        String scoreKey = RedisPostKeys.REALTIME_POST_SCORE_KEY;
        redisTemplate.opsForZSet().add(scoreKey, "100", 15.0);
        redisTemplate.opsForZSet().add(scoreKey, "200", 25.0);
        redisTemplate.opsForZSet().add(scoreKey, "300", 10.0);
        redisTemplate.opsForZSet().add(scoreKey, "400", 30.0);
        redisTemplate.opsForZSet().add(scoreKey, "500", 20.0);

        // When: 실시간 인기글 ID 조회
        List<Long> result = redisRealTimePostStoreAdapter.getRealtimePopularPostIds();

        // Then: 점수 내림차순으로 정렬됨
        assertThat(result).containsExactly(400L, 200L, 500L, 100L, 300L); // 30, 25, 20, 15, 10
    }

    @Test
    @DisplayName("경계값 - 실시간 인기글이 5개 미만인 경우")
    void shouldReturnLessThan5_WhenFewerPostsExist() {
        // Given: 3개의 게시글만 점수 설정
        String scoreKey = RedisPostKeys.REALTIME_POST_SCORE_KEY;
        redisTemplate.opsForZSet().add(scoreKey, "1", 10.0);
        redisTemplate.opsForZSet().add(scoreKey, "2", 8.0);
        redisTemplate.opsForZSet().add(scoreKey, "3", 6.0);

        // When: 실시간 인기글 ID 조회
        List<Long> result = redisRealTimePostStoreAdapter.getRealtimePopularPostIds();

        // Then: 존재하는 3개만 반환
        assertThat(result).hasSize(3);
        assertThat(result).containsExactly(1L, 2L, 3L);
    }

    @Test
    @DisplayName("경계값 - 실시간 인기글이 없는 경우")
    void shouldReturnEmptyList_WhenNoRealtimePostsExist() {
        // Given: 실시간 인기글 점수가 없는 상태

        // When: 실시간 인기글 ID 조회
        List<Long> result = redisRealTimePostStoreAdapter.getRealtimePopularPostIds();

        // Then: 빈 목록 반환
        assertThat(result).isEmpty();
    }
}
