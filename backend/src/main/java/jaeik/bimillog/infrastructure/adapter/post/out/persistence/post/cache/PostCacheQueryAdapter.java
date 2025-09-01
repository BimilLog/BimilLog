package jaeik.bimillog.infrastructure.adapter.post.out.persistence.post.cache;

import jaeik.bimillog.domain.post.application.port.out.PostCacheQueryPort;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSearchResult;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * <h2>Redis 캐시 조회 어댑터</h2>
 * <p>Redis를 사용하여 게시글 캐시 데이터를 관리하는 영속성 어댑터입니다.</p>
 * <p>PostCacheQueryPort 인터페이스를 구현합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
public class PostCacheQueryAdapter implements PostCacheQueryPort {

    private final RedisTemplate<String, Object> redisTemplate;
    private final Map<PostCacheFlag, CacheMetadata> cacheMetadataMap;
    private static final String FULL_POST_CACHE_PREFIX = "cache:post:";

    /**
     * <h3>RedisCacheAdapter 생성자</h3>
     * <p>RedisTemplate을 주입받아 캐시 메타데이터를 초기화합니다.</p>
     *
     * @param redisTemplate Redis 작업을 위한 템플릿
     * @author Jaeik
     * @since 2.0.0
     */
    public PostCacheQueryAdapter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.cacheMetadataMap = new EnumMap<>(PostCacheFlag.class);
        cacheMetadataMap.put(PostCacheFlag.REALTIME, new CacheMetadata("cache:posts:realtime", Duration.ofMinutes(30)));
        cacheMetadataMap.put(PostCacheFlag.WEEKLY, new CacheMetadata("cache:posts:weekly", Duration.ofDays(1)));
        cacheMetadataMap.put(PostCacheFlag.LEGEND, new CacheMetadata("cache:posts:legend", Duration.ofDays(1)));
        cacheMetadataMap.put(PostCacheFlag.NOTICE, new CacheMetadata("cache:posts:notice", Duration.ofDays(7)));
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
            throw new CustomException(ErrorCode.REDIS_READ_ERROR.getStatus(), "Unknown PostCacheFlag type: " + type);
        }
        return metadata;
    }

    @Override
    public boolean hasPopularPostsCache(PostCacheFlag type) {
        CacheMetadata metadata = getCacheMetadata(type);
        try {
            return redisTemplate.hasKey(metadata.key());
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_READ_ERROR, e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PostSearchResult> getCachedPostList(PostCacheFlag type) {
        CacheMetadata metadata = getCacheMetadata(type);
        try {
            // 1. Sorted Set에서 ID 목록 조회 (score 높은 순)
            Set<Object> postIds = redisTemplate.opsForZSet().reverseRange(metadata.key(), 0, -1);
            if (postIds == null || postIds.isEmpty()) {
                return Collections.emptyList();
            }

            // 2. 상세 캐시에서 PostDetail 조회 후 PostSearchResult로 변환
            return postIds.stream()
                    .map(Object::toString)
                    .map(Long::valueOf)
                    .map(this::getCachedPostIfExists)
                    .filter(java.util.Objects::nonNull)
                    .map(PostDetail::toSearchResult)
                    .toList();
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_READ_ERROR, e);
        }
    }

    @Override
    public PostDetail getCachedPostIfExists(Long postId) {
        String key = FULL_POST_CACHE_PREFIX + postId;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof PostDetail postDetail) {
                // Redis에서 직접 PostDetail로 저장하므로 캐스팅만 필요
                return postDetail;
            }
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_READ_ERROR, e);
        }
        return null;
    }

    @Override
    public Page<PostSearchResult> getCachedPostListPaged(PostCacheFlag type, Pageable pageable) {
        CacheMetadata metadata = getCacheMetadata(type);
        try {
            // 1. 전체 크기 조회
            Long totalElements = redisTemplate.opsForZSet().count(metadata.key(), Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
            if (totalElements == null || totalElements == 0) {
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }
            
            // 2. Sorted Set에서 페이징된 ID 목록 조회 (score 높은 순)
            int page = pageable.getPageNumber();
            int size = pageable.getPageSize();
            long start = (long) page * size;
            long end = start + size - 1;
            
            Set<Object> postIds = redisTemplate.opsForZSet().reverseRange(metadata.key(), start, end);
            if (postIds == null || postIds.isEmpty()) {
                return new PageImpl<>(Collections.emptyList(), pageable, totalElements);
            }
            
            // 3. 상세 캐시에서 PostDetail 조회 후 PostSearchResult로 변환
            List<PostSearchResult> pagedPosts = postIds.stream()
                    .map(Object::toString)
                    .map(Long::valueOf)
                    .map(this::getCachedPostIfExists)
                    .filter(java.util.Objects::nonNull)
                    .map(PostDetail::toSearchResult)
                    .toList();
            
            return new PageImpl<>(pagedPosts, pageable, totalElements);
            
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_READ_ERROR, e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean isPopularPost(Long postId) {
        try {
            // 모든 인기글 타입에 대해 확인
            for (PostCacheFlag flag : PostCacheFlag.getPopularPostTypes()) {
                // 해당 타입의 캐시가 있는지 확인
                if (hasPopularPostsCache(flag)) {
                    // Sorted Set에서 해당 ID가 있는지 확인
                    Double score = redisTemplate.opsForZSet().score(getCacheMetadata(flag).key(), postId.toString());
                    if (score != null) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            // 캐시 조회 실패 시 false 반환 (일반 게시글로 처리)
            return false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean existsInNoticeCache(Long postId) {
        CacheMetadata metadata = getCacheMetadata(PostCacheFlag.NOTICE);
        try {
            // Sorted Set에서 해당 ID가 있는지 확인
            Double score = redisTemplate.opsForZSet().score(metadata.key(), postId.toString());
            return score != null;
        } catch (Exception e) {
            // 캐시 읽기 실패 시 false 반환
            return false;
        }
    }

    @Override
    public List<PostSearchResult> findAllNotices() {
        return getCachedPostList(PostCacheFlag.NOTICE);
    }
}
