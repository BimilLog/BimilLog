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
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>RedisTier1PostStoreAdapter 통합 테스트</h2>
 * <p>로컬 Redis 환경에서 게시글 목록 캐시 어댑터의 핵심 기능을 검증합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("local-integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("local-integration")
class RedisTier1PostStoreAdapterIntegrationTest {

    @Autowired
    private RedisTier1PostStoreAdapter redisTier1PostStoreAdapter;

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
    @DisplayName("정상 케이스 - 캐시 TTL 조회")
    void shouldReturnCacheTTL_WhenCacheExists() {
        // Given: WEEKLY 캐시 저장 (TTL 5분)
        PostCacheFlag type = PostCacheFlag.WEEKLY;
        String hashKey = RedisPostKeys.CACHE_METADATA_MAP.get(type).key();

        PostSimpleDetail post = toSimpleDetail(testPostDetail1);
        redisTemplate.opsForHash().put(hashKey, "1", post);
        redisTemplate.expire(hashKey, Duration.ofMinutes(5));

        // When: TTL 조회
        Long ttl = redisTier1PostStoreAdapter.getPostListCacheTTL(type);

        // Then: 290~300초 사이
        assertThat(ttl).isBetween(290L, 300L);
    }

    @Test
    @DisplayName("정상 케이스 - 캐시 Map 조회")
    void shouldReturnCachedPostMap_WhenHashExists() {
        // Given: Hash에 PostSimpleDetail 저장
        PostCacheFlag type = PostCacheFlag.WEEKLY;
        String hashKey = RedisPostKeys.CACHE_METADATA_MAP.get(type).key();

        PostSimpleDetail post1 = toSimpleDetail(testPostDetail1);
        PostSimpleDetail post2 = toSimpleDetail(testPostDetail2);

        redisTemplate.opsForHash().put(hashKey, "1", post1);
        redisTemplate.opsForHash().put(hashKey, "2", post2);

        // When: Map 조회
        Map<Long, PostSimpleDetail> result = redisTier1PostStoreAdapter.getCachedPostMap(type);

        // Then: Map 반환 확인
        assertThat(result).hasSize(2);
        assertThat(result.get(1L).getTitle()).isEqualTo("첫 번째 게시글");
        assertThat(result.get(2L).getTitle()).isEqualTo("두 번째 게시글");
    }

    @Test
    @DisplayName("정상 케이스 - 캐시된 게시글 개수 조회")
    void shouldReturnCachedPostCount_WhenHashExists() {
        // Given: RedisTemplate로 직접 저장
        PostCacheFlag cacheType = PostCacheFlag.REALTIME;
        String hashKey = RedisPostKeys.CACHE_METADATA_MAP.get(cacheType).key();

        // PostSimpleDetail 생성 (PostDetail에서 변환)
        PostSimpleDetail simple1 = toSimpleDetail(testPostDetail1);
        PostSimpleDetail simple2 = toSimpleDetail(testPostDetail2);
        PostSimpleDetail simple3 = toSimpleDetail(testPostDetail3);

        // Hash에 PostSimpleDetail 저장 (Field: postId, Value: PostSimpleDetail)
        redisTemplate.opsForHash().put(hashKey, "1", simple1);
        redisTemplate.opsForHash().put(hashKey, "2", simple2);
        redisTemplate.opsForHash().put(hashKey, "3", simple3);
        redisTemplate.expire(hashKey, Duration.ofMinutes(5));

        // When: 개수 조회
        long count = redisTier1PostStoreAdapter.getCachedPostCount(cacheType);

        // Then: 3개의 게시글 개수 확인
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("정상 케이스 - 빈 캐시 개수 조회")
    void shouldReturnZeroCount_WhenNoCachedPostsExist() {
        // Given: 캐시가 없는 상태
        PostCacheFlag cacheType = PostCacheFlag.REALTIME;

        // When: 개수 조회
        long count = redisTier1PostStoreAdapter.getCachedPostCount(cacheType);

        // Then: 0 반환
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("정상 케이스 - 페이징 조회 시 일부 게시글만 캐시된 경우 존재하는 것만 조회")
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

        // When: 페이징 목록 조회
        Pageable pageable = PageRequest.of(0, 10);
        Page<PostSimpleDetail> result = redisTier1PostStoreAdapter.getCachedPostListPaged(cacheType, pageable);

        // Then: Hash에 있는 2개만 반환 (필터링됨)
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting("id").containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("정상 케이스 - 레전드 게시글 목록 페이지네이션 조회 (첫 페이지와 두 번째 페이지)")
    void shouldReturnPagedLists_FirstAndSecondPage() {
        RedisTestHelper.flushRedis(redisTemplate);

        // Given: RedisTemplate로 직접 20개의 레전드 게시글 저장
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
        Page<PostSimpleDetail> firstResult = redisTier1PostStoreAdapter.getCachedPostListPaged(PostCacheFlag.LEGEND, firstPage);

        assertThat(firstResult.getContent()).hasSize(10);
        assertThat(firstResult.getTotalElements()).isEqualTo(20);
        assertThat(firstResult.getTotalPages()).isEqualTo(2);
        assertThat(firstResult.getNumber()).isEqualTo(0); // 현재 페이지 번호
        assertThat(firstResult.getContent().get(0).getId()).isEqualTo(1L);
        assertThat(firstResult.getContent().get(9).getId()).isEqualTo(10L);

        // When & Then - 케이스 2: 두 번째 페이지 조회 (페이지 1, 사이즈 10)
        Pageable secondPage = PageRequest.of(1, 10);
        Page<PostSimpleDetail> secondResult = redisTier1PostStoreAdapter.getCachedPostListPaged(PostCacheFlag.LEGEND, secondPage);

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
        Page<PostSimpleDetail> result = redisTier1PostStoreAdapter.getCachedPostListPaged(PostCacheFlag.LEGEND, pageable);

        // Then: 빈 페이지 반환
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getTotalPages()).isEqualTo(0);
    }

    @Test
    @DisplayName("정상 케이스 - 게시글 목록 캐시 저장")
    void shouldCachePostList_WhenValidPostsProvided() {
        // Given
        PostCacheFlag type = PostCacheFlag.WEEKLY;
        List<PostSimpleDetail> posts = List.of(
                toSimpleDetail(testPostDetail1),
                toSimpleDetail(testPostDetail2)
        );

        // When: 목록 저장
        redisTier1PostStoreAdapter.cachePostList(type, posts);

        // Then: Hash에 저장 확인
        String hashKey = RedisPostKeys.CACHE_METADATA_MAP.get(type).key();
        assertThat(redisTemplate.hasKey(hashKey)).isTrue();
        assertThat(redisTemplate.opsForHash().size(hashKey)).isEqualTo(2);

        // TTL 확인 (5분)
        Long ttl = redisTemplate.getExpire(hashKey, TimeUnit.SECONDS);
        assertThat(ttl).isBetween(290L, 300L);
    }

    @Test
    @DisplayName("정상 케이스 - 게시글 목록 캐시 전체 삭제")
    void shouldClearPostListCache() {
        // Given: post:weekly:list Hash에 여러 게시글 추가
        PostCacheFlag type = PostCacheFlag.WEEKLY;
        String hashKey = RedisPostKeys.CACHE_METADATA_MAP.get(type).key();

        redisTemplate.opsForHash().put(hashKey, "1", testPostDetail1);
        redisTemplate.opsForHash().put(hashKey, "2", testPostDetail2);

        // 저장 확인
        assertThat(redisTemplate.hasKey(hashKey)).isTrue();
        assertThat(redisTemplate.opsForHash().size(hashKey)).isEqualTo(2);

        // When: clearPostListCache() 호출
        redisTier1PostStoreAdapter.clearPostListCache(type);

        // Then: Hash 전체가 삭제됨 확인
        assertThat(redisTemplate.hasKey(hashKey)).isFalse();
    }

    @Test
    @DisplayName("정상 케이스 - 목록 캐시에서 단일 게시글 제거 (모든 Hash 필드 삭제)")
    void shouldRemovePostFromListCache() {
        // Given: 모든 타입의 Hash에 게시글 추가
        Long postId = 1L;

        // 모든 캐시 타입에 게시글 추가
        String realtimeKey = RedisPostKeys.CACHE_METADATA_MAP.get(PostCacheFlag.REALTIME).key();
        String weeklyKey = RedisPostKeys.CACHE_METADATA_MAP.get(PostCacheFlag.WEEKLY).key();
        String legendKey = RedisPostKeys.CACHE_METADATA_MAP.get(PostCacheFlag.LEGEND).key();
        String noticeKey = RedisPostKeys.CACHE_METADATA_MAP.get(PostCacheFlag.NOTICE).key();

        redisTemplate.opsForHash().put(realtimeKey, postId.toString(), testPostDetail1);
        redisTemplate.opsForHash().put(weeklyKey, postId.toString(), testPostDetail1);
        redisTemplate.opsForHash().put(legendKey, postId.toString(), testPostDetail1);
        redisTemplate.opsForHash().put(noticeKey, postId.toString(), testPostDetail1);

        // 저장 확인
        assertThat(redisTemplate.opsForHash().hasKey(realtimeKey, postId.toString())).isTrue();
        assertThat(redisTemplate.opsForHash().hasKey(weeklyKey, postId.toString())).isTrue();

        // When: removePostFromListCache() 호출 (모든 타입에서 제거)
        redisTier1PostStoreAdapter.removePostFromListCache(postId);

        // Then: 모든 Hash에서 필드가 삭제됨 확인
        assertThat(redisTemplate.opsForHash().hasKey(realtimeKey, postId.toString())).isFalse();
        assertThat(redisTemplate.opsForHash().hasKey(weeklyKey, postId.toString())).isFalse();
        assertThat(redisTemplate.opsForHash().hasKey(legendKey, postId.toString())).isFalse();
        assertThat(redisTemplate.opsForHash().hasKey(noticeKey, postId.toString())).isFalse();
    }
}
