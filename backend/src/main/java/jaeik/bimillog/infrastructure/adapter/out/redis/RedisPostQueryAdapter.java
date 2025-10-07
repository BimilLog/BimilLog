package jaeik.bimillog.infrastructure.adapter.out.redis;

import jaeik.bimillog.domain.post.application.port.out.RedisPostQueryPort;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.exception.PostErrorCode;
import jaeik.bimillog.infrastructure.exception.CustomException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;

/**
 * <h2>게시글 캐시 조회 어댑터</h2>
 * <p>게시글 캐시 조회 포트의 Redis 구현체입니다.</p>
 * <p>인기글 목록 캐시 조회, 게시글 상세 캐시 조회</p>
 * <p>Redis List과 개별 상세 캐시 활용</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
public class RedisPostQueryAdapter implements RedisPostQueryPort {

    private final RedisTemplate<String, Object> redisTemplate;
    private final Map<PostCacheFlag, CacheMetadata> cacheMetadataMap;
    private static final String FULL_POST_CACHE_PREFIX = "cache:post:";
    private static final String POSTIDS_PREFIX = "cache:postids:";
    private static final String REALTIME_POPULAR_SCORE_KEY = "cache:realtime:scores";

    /**
     * <h3>RedisCacheAdapter 생성자</h3>
     * <p>RedisTemplate을 주입받아 캐시 메타데이터를 초기화합니다.</p>
     *
     * @param redisTemplate Redis 작업을 위한 템플릿
     * @author Jaeik
     * @since 2.0.0
     */
    public RedisPostQueryAdapter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.cacheMetadataMap = new EnumMap<>(PostCacheFlag.class);
        cacheMetadataMap.put(PostCacheFlag.REALTIME, new CacheMetadata("cache:posts:realtime", Duration.ofMinutes(5)));
        cacheMetadataMap.put(PostCacheFlag.WEEKLY, new CacheMetadata("cache:posts:weekly", Duration.ofMinutes(5)));
        cacheMetadataMap.put(PostCacheFlag.LEGEND, new CacheMetadata("cache:posts:legend", Duration.ofMinutes(5)));
        cacheMetadataMap.put(PostCacheFlag.NOTICE, new CacheMetadata("cache:posts:notice", Duration.ofMinutes(5)));
    }

    private record CacheMetadata(String key, Duration ttl) {}

    /**
     * <h3>캐시 메타데이터 조회</h3>
     * <p>주어진 캐시 유형에 해당하는 메타데이터를 조회합니다.</p>
     *
     * @param type 게시글 캐시 유형
     * @return 캐시 메타데이터
     * @throws CustomException 알 수 없는 PostCacheFlag 유형인 경우
     * @author Jaeik
     * @since 2.0.0
     */
    private CacheMetadata getCacheMetadata(PostCacheFlag type) {
        CacheMetadata metadata = cacheMetadataMap.get(type);
        if (metadata == null) {
            throw new PostCustomException(PostErrorCode.REDIS_READ_ERROR, "Unknown PostCacheFlag type: " + type);
        }
        return metadata;
    }

    /**
     * <h3>인기 게시글 캐시 존재 여부 확인</h3>
     * <p>주어진 캐시 유형의 인기 게시글 캐시가 존재하는지 확인합니다.</p>
     *
     * @param type 확인할 캐시 유형
     * @return 캐시가 존재하면 true, 없으면 false
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public boolean hasPopularPostsCache(PostCacheFlag type) {
        CacheMetadata metadata = getCacheMetadata(type);
        try {
            return redisTemplate.hasKey(metadata.key());
        } catch (Exception e) {
            throw new PostCustomException(PostErrorCode.REDIS_READ_ERROR, e);
        }
    }

    /**
     * <h3>캐시된 게시글 목록 조회</h3>
     * <p>Redis List에서 게시글 ID 목록을 조회하고 상세 캐시에서 정보를 가져와 변환합니다.</p>
     * <p>목록 캐시 미스 시 postIds 저장소에서 ID를 가져와 DB 조회 후 목록을 재구성합니다.</p>
     *
     * @param type 조회할 캐시 유형
     * @return 캐시된 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<PostSimpleDetail> getCachedPostList(PostCacheFlag type) {
        CacheMetadata metadata = getCacheMetadata(type);
        try {
            // 1. 목록 캐시에서 ID 목록 조회 (순서대로)
            List<Object> postIds = redisTemplate.opsForList().range(metadata.key(), 0, -1);

            // 2. 목록 캐시 미스 시 빈 리스트 반환 (복구는 서비스 레이어에서 처리)
            if (postIds == null || postIds.isEmpty()) {
                return Collections.emptyList();
            }

            // 3. 상세 캐시에서 PostDetail 조회 후 PostSearchResult로 변환
            return postIds.stream()
                    .map(Object::toString)
                    .map(Long::valueOf)
                    .map(this::getCachedPostIfExists)
                    .filter(java.util.Objects::nonNull)
                    .map(PostDetail::toSearchResult)
                    .toList();
        } catch (Exception e) {
            throw new PostCustomException(PostErrorCode.REDIS_READ_ERROR, e);
        }
    }

    /**
     * <h3>postIds 영구 저장소에서 ID 목록 조회</h3>
     * <p>캐시 미스 발생 시 복구를 위해 영구 저장된 postId 목록을 조회합니다.</p>
     * <p>PostQueryService에서 목록 캐시 미스 시 DB 조회를 위한 ID 목록 획득에 사용됩니다.</p>
     *
     * @param type 조회할 인기글 캐시 유형 (WEEKLY, LEGEND, NOTICE)
     * @return 저장된 게시글 ID 목록 (없으면 빈 리스트)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<Long> getStoredPostIds(PostCacheFlag type) {
        String postIdsKey = POSTIDS_PREFIX + type.name().toLowerCase();
        try {
            List<Object> postIds = redisTemplate.opsForList().range(postIdsKey, 0, -1);
            if (postIds == null || postIds.isEmpty()) {
                return Collections.emptyList();
            }
            return postIds.stream()
                    .map(Object::toString)
                    .map(Long::valueOf)
                    .toList();
        } catch (Exception e) {
            throw new PostCustomException(PostErrorCode.REDIS_READ_ERROR, e);
        }
    }

    /**
     * <h3>게시글 상세 캐시 조회</h3>
     * <p>Redis에서 게시글 상세 정보를 조회합니다. 캐시가 없으면 null을 반환합니다.</p>
     *
     * @param postId 조회할 게시글 ID
     * @return 캐시된 게시글 상세 정보 (없으면 null)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public PostDetail getCachedPostIfExists(Long postId) {
        String key = FULL_POST_CACHE_PREFIX + postId;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof PostDetail postDetail) {
                return postDetail;
            }
        } catch (Exception e) {
            throw new PostCustomException(PostErrorCode.REDIS_READ_ERROR, e);
        }
        return null;
    }

    /**
     * <h3>레전드 게시글 목록 페이지네이션 조회</h3>
     * <p>레전드 게시글 목록을 페이지네이션으로 조회합니다. Redis List 구조를 활용합니다.</p>
     *
     * @param pageable 페이지 정보
     * @return 캐시된 레전드 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<PostSimpleDetail> getCachedPostListPaged(Pageable pageable) {
        CacheMetadata metadata = getCacheMetadata(PostCacheFlag.LEGEND);
        try {
            // 1. 전체 크기 조회
            Long totalElements = redisTemplate.opsForList().size(metadata.key());
            if (totalElements == null || totalElements == 0) {
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }

            // 2. List에서 페이징된 ID 목록 조회 (순서대로)
            int page = pageable.getPageNumber();
            int size = pageable.getPageSize();
            long start = (long) page * size;
            long end = start + size - 1;

            List<Object> postIds = redisTemplate.opsForList().range(metadata.key(), start, end);
            if (postIds == null || postIds.isEmpty()) {
                return new PageImpl<>(Collections.emptyList(), pageable, totalElements);
            }

            // 3. 상세 캐시에서 PostDetail 조회 후 PostSearchResult로 변환
            List<PostSimpleDetail> pagedPosts = postIds.stream()
                    .map(Object::toString)
                    .map(Long::valueOf)
                    .map(this::getCachedPostIfExists)
                    .filter(java.util.Objects::nonNull)
                    .map(PostDetail::toSearchResult)
                    .toList();

            return new PageImpl<>(pagedPosts, pageable, totalElements);

        } catch (Exception e) {
            throw new PostCustomException(PostErrorCode.REDIS_READ_ERROR, e);
        }
    }

    /**
     * <h3>실시간 인기글 postId 목록 조회</h3>
     * <p>Redis Sorted Set에서 점수가 높은 상위 5개의 게시글 ID를 조회합니다.</p>
     * <p>PostQueryService에서 실시간 인기글 목록 조회 시 호출됩니다.</p>
     *
     * @return List&lt;Long&gt; 상위 5개 게시글 ID 목록 (점수 내림차순)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<Long> getRealtimePopularPostIds() {
        try {
            // Sorted Set에서 점수 높은 순으로 상위 5개 조회
            Set<Object> postIds = redisTemplate.opsForZSet().reverseRange(REALTIME_POPULAR_SCORE_KEY, 0, 4);
            if (postIds == null || postIds.isEmpty()) {
                return Collections.emptyList();
            }

            return postIds.stream()
                    .map(Object::toString)
                    .map(Long::valueOf)
                    .toList();
        } catch (Exception e) {
            throw new PostCustomException(PostErrorCode.REDIS_READ_ERROR, e);
        }
    }
}
