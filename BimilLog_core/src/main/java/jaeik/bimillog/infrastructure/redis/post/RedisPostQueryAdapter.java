package jaeik.bimillog.infrastructure.redis.post;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.infrastructure.log.CacheMetricsLogger;
import jaeik.bimillog.infrastructure.redis.post.RedisPostKeys.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static jaeik.bimillog.infrastructure.redis.post.RedisPostKeys.*;

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
@Slf4j
public class RedisPostQueryAdapter {

    private final RedisTemplate<String, Object> redisTemplate;
    private final Map<PostCacheFlag, CacheMetadata> cacheMetadataMap;

    /**
     * <h3>RedisPostQueryAdapter 생성자</h3>
     * <p>RedisTemplate을 주입받아 PostRedisKeys의 캐시 메타데이터 맵을 설정합니다.</p>
     *
     * @param redisTemplate Redis 작업을 위한 템플릿
     * @author Jaeik
     * @since 2.0.0
     */
    public RedisPostQueryAdapter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.cacheMetadataMap = CACHE_METADATA_MAP;
    }

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
            throw new CustomException(ErrorCode.POST_REDIS_READ_ERROR);
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
    public boolean hasPopularPostsCache(PostCacheFlag type) {
        CacheMetadata metadata = getCacheMetadata(type);
        try {
            boolean exists = redisTemplate.hasKey(metadata.key());
            String cacheName = "post:popular:" + type.name().toLowerCase();
            if (exists) {
                CacheMetricsLogger.hit(log, cacheName, metadata.key());
            } else {
                CacheMetricsLogger.miss(log, cacheName, metadata.key(), "key_not_found");
            }
            return exists;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.POST_REDIS_READ_ERROR, e);
        }
    }

    /**
     * <h3>캐시된 게시글 목록 조회 (Hash 구조)</h3>
     * <p>Redis Hash에서 PostSimpleDetail 목록을 조회합니다.</p>
     * <p>postIds 저장소의 순서를 사용하여 정렬합니다.</p>
     *
     * @param type 조회할 캐시 유형
     * @return 캐시된 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @SuppressWarnings("unchecked")
    public List<PostSimpleDetail> getCachedPostList(PostCacheFlag type) {
        CacheMetadata metadata = getCacheMetadata(type);
        try {
            Map<Object, Object> hashEntries = redisTemplate.opsForHash().entries(metadata.key());

            if (hashEntries.isEmpty()) {
                CacheMetricsLogger.miss(log, "post:list:" + type.name().toLowerCase(),
                        metadata.key(), "hash_empty");
                return Collections.emptyList();
            }

            // 모든 타입에 대해 정렬된 순서 사용 (REALTIME 포함)
            List<Long> orderedIds = getStoredPostIds(type);
            if (orderedIds.isEmpty()) {
                CacheMetricsLogger.miss(log, "post:list:" + type.name().toLowerCase(),
                        metadata.key(), "ordered_ids_empty");
                return Collections.emptyList();
            }

            List<PostSimpleDetail> cachedPosts = orderedIds.stream()
                    .map(id -> (PostSimpleDetail) hashEntries.get(id.toString()))
                    .filter(java.util.Objects::nonNull)
                    .toList();
            if (cachedPosts.isEmpty()) {
                CacheMetricsLogger.miss(log, "post:list:" + type.name().toLowerCase(),
                        metadata.key(), "resolved_entries_empty");
            } else {
                CacheMetricsLogger.hit(log, "post:list:" + type.name().toLowerCase(),
                        metadata.key(), cachedPosts.size());
            }
            return cachedPosts;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.POST_REDIS_READ_ERROR, e);
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
    public List<Long> getStoredPostIds(PostCacheFlag type) {
        String postIdsKey = getPostIdsStorageKey(type);
        try {
            Set<Object> postIds;

            if (type == PostCacheFlag.NOTICE) {
                // 공지사항: Set에서 조회
                postIds = redisTemplate.opsForSet().members(postIdsKey);
            } else {
                // 주간/레전드: Sorted Set에서 조회 (점수 오름차순 = DB 추출 순서)
                postIds = redisTemplate.opsForZSet().range(postIdsKey, 0, -1);
            }

            if (postIds == null || postIds.isEmpty()) {
                CacheMetricsLogger.miss(log, "post:ids:" + type.name().toLowerCase(),
                        postIdsKey, "post_ids_empty");
                return Collections.emptyList();
            }

            List<Long> ids = postIds.stream()
                    .map(Object::toString)
                    .map(Long::valueOf)
                    .toList();
            CacheMetricsLogger.hit(log, "post:ids:" + type.name().toLowerCase(),
                    postIdsKey, ids.size());
            return ids;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.POST_REDIS_READ_ERROR, e);
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
    public PostDetail getCachedPostIfExists(Long postId) {
        String key = getPostDetailKey(postId);
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof PostDetail postDetail) {
                CacheMetricsLogger.hit(log, "post:detail", postId);
                return postDetail;
            }
            CacheMetricsLogger.miss(log, "post:detail", postId, "value_not_found");
        } catch (Exception e) {
            throw new CustomException(ErrorCode.POST_REDIS_READ_ERROR, e);
        }
        return null;
    }

    /**
     * <h3>레전드 게시글 목록 페이지네이션 조회 (Hash 구조)</h3>
     * <p>레전드 게시글 목록을 페이지네이션으로 조회합니다.</p>
     * <p>postIds 저장소의 순서를 사용하여 페이징 및 정렬합니다.</p>
     *
     * @param pageable 페이지 정보
     * @return 캐시된 레전드 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    public Page<PostSimpleDetail> getCachedPostListPaged(Pageable pageable) {
        CacheMetadata metadata = getCacheMetadata(PostCacheFlag.LEGEND);
        try {
            // 1. Hash에서 모든 PostSimpleDetail 조회
            Map<Object, Object> hashEntries = redisTemplate.opsForHash().entries(metadata.key());
            if (hashEntries.isEmpty()) {
                CacheMetricsLogger.miss(log, "post:legend:list", metadata.key(), "hash_empty");
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }

            // 2. postIds 저장소에서 전체 순서 가져오기
            List<Long> orderedIds = getStoredPostIds(PostCacheFlag.LEGEND);
            if (orderedIds.isEmpty()) {
                CacheMetricsLogger.miss(log, "post:legend:list", metadata.key(), "ordered_ids_empty");
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }

            // 3. 페이징 처리
            int page = pageable.getPageNumber();
            int size = pageable.getPageSize();
            int start = page * size;
            int end = Math.min(start + size, orderedIds.size());

            if (start >= orderedIds.size()) {
                CacheMetricsLogger.miss(log, "post:legend:list", metadata.key(), "page_out_of_range");
                return new PageImpl<>(Collections.emptyList(), pageable, orderedIds.size());
            }

            // 4. 페이징된 ID 목록으로 PostSimpleDetail 조회
            List<PostSimpleDetail> pagedPosts = orderedIds.subList(start, end).stream()
                    .map(id -> (PostSimpleDetail) hashEntries.get(id.toString()))
                    .filter(java.util.Objects::nonNull)
                    .toList();

            if (pagedPosts.isEmpty()) {
                CacheMetricsLogger.miss(log, "post:legend:list", metadata.key(), "resolved_entries_empty");
            } else {
                CacheMetricsLogger.hit(log, "post:legend:list", metadata.key(), pagedPosts.size());
            }

            return new PageImpl<>(pagedPosts, pageable, orderedIds.size());

        } catch (Exception e) {
            throw new CustomException(ErrorCode.POST_REDIS_READ_ERROR, e);
        }
    }

    /**
     * <h3>실시간 인기글 postId 목록 조회</h3>
     * <p>Redis Sorted Set에서 점수가 높은 상위 5개의 게시글 ID를 조회합니다.</p>
     * <p>PostQueryService에서 실시간 인기글 목록 조회 시 호출됩니다.</p>
     *
     * @return List<Long> 상위 5개 게시글 ID 목록 (점수 내림차순)
     * @author Jaeik
     * @since 2.0.0
     */
    public List<Long> getRealtimePopularPostIds() {
        try {
            // Sorted Set에서 점수 높은 순으로 상위 5개 조회
            Set<Object> postIds = redisTemplate.opsForZSet().reverseRange(REALTIME_POST_SCORE_KEY, 0, 4);
            if (postIds == null || postIds.isEmpty()) {
                CacheMetricsLogger.miss(log, "post:realtime", REALTIME_POST_SCORE_KEY, "sorted_set_empty");
                return Collections.emptyList();
            }

            List<Long> ids = postIds.stream()
                    .map(Object::toString)
                    .map(Long::valueOf)
                    .toList();
            CacheMetricsLogger.hit(log, "post:realtime", REALTIME_POST_SCORE_KEY, ids.size());
            return ids;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.POST_REDIS_READ_ERROR, e);
        }
    }

    /**
     * <h3>게시글 목록 캐시 TTL 조회</h3>
     * <p>특정 캐시 유형의 남은 TTL(Time To Live)을 초 단위로 조회합니다.</p>
     * <p>확률적 선계산(Probabilistic Early Expiration) 기법에 사용됩니다.</p>
     *
     * @param type 조회할 인기글 캐시 유형 (REALTIME, WEEKLY, LEGEND, NOTICE)
     * @return Long 남은 TTL (초 단위), 키가 없으면 -2, 만료 시간이 없으면 -1
     * @author Jaeik
     * @since 2.0.0
     */
    public Long getPostListCacheTTL(PostCacheFlag type) {
        CacheMetadata metadata = getCacheMetadata(type);
        try {
            return redisTemplate.getExpire(metadata.key(), TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.POST_REDIS_READ_ERROR, e);
        }
    }
}
