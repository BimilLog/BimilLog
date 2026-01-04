package jaeik.bimillog.infrastructure.redis.post;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.infrastructure.log.CacheMetricsLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static jaeik.bimillog.infrastructure.redis.post.RedisPostKeys.CACHE_METADATA_MAP;
import static jaeik.bimillog.infrastructure.redis.post.RedisPostKeys.CacheMetadata;

/**
 * <h2>레디스 게시글 캐시 티어1 저장소 어댑터</h2>
 * <p>게시글 상세를 제외한 실시간, 주간, 전설인기글과 공지사항의 글 목록 리스트를 관리한다.</p>
 *
 * @author Jaeik
 * @version 2.4.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RedisTier1PostStoreAdapter {
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisTier2PostStoreAdapter redisTier2PostStoreAdapter;
    private final Map<PostCacheFlag, CacheMetadata> cacheMetadataMap = CACHE_METADATA_MAP;

    /**
     * <h3>TTL 조회</h3>
     * <p>특정 캐시 유형의 남은 TTL을 초 단위로 조회합니다.</p>
     *
     * @param type 조회할 인기글 캐시 유형 (REALTIME, WEEKLY, LEGEND, NOTICE)
     * @return Long 남은 TTL (초 단위), 키가 없으면 -2, 만료 시간이 없으면 -1
     */
    public Long getPostListCacheTTL(PostCacheFlag type) {
        CacheMetadata metadata = getCacheMetadata(type);
        return redisTemplate.getExpire(metadata.key(), TimeUnit.SECONDS);
    }

    /**
     * <h3>캐시 목록 맵 조회</h3>
     * <p>Redis Hash에서 PostSimpleDetail을 Map으로 조회합니다.</p>
     * <p>순서 정보 없이 순수하게 캐시 데이터만 반환합니다.</p>
     *
     * @param type 조회할 캐시 유형
     * @return 캐시된 게시글 Map (postId -> PostSimpleDetail)
     */
    @SuppressWarnings("unchecked")
    public Map<Long, PostSimpleDetail> getCachedPostMap(PostCacheFlag type) {
        CacheMetadata metadata = getCacheMetadata(type);
        Map<Object, Object> hashEntries = redisTemplate.opsForHash().entries(metadata.key());

        if (hashEntries.isEmpty()) {
            CacheMetricsLogger.miss(log, "post:map:" + type.name().toLowerCase(), metadata.key(), "hash_empty");
            return Collections.emptyMap();
        }

        Map<Long, PostSimpleDetail> cachedMap = hashEntries.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> Long.valueOf(e.getKey().toString()),
                        e -> (PostSimpleDetail) e.getValue()
                ));

        CacheMetricsLogger.hit(log, "post:map:" + type.name().toLowerCase(), metadata.key(), cachedMap.size());
        return cachedMap;
    }

    /**
     * <h3>캐시 목록 리스트 조회</h3>
     * <p>Redis Hash에서 PostSimpleDetail 목록을 조회합니다.</p>
     * <p>postIds 저장소의 순서를 사용하여 정렬합니다.</p>
     *
     * @param type 조회할 캐시 유형 (WEEKLY, LEGEND, NOTICE)
     * @return 캐시된 게시글 목록
     */
    @SuppressWarnings("unchecked")
    public List<PostSimpleDetail> getCachedPostList(PostCacheFlag type) {
        Map<Long, PostSimpleDetail> cachedMap = getCachedPostMap(type);
        if (cachedMap.isEmpty()) {
            return Collections.emptyList();
        }

        // postIds 저장소에서 순서 가져오기
        List<Long> orderedIds = redisTier2PostStoreAdapter.getStoredPostIds(type);
        if (orderedIds.isEmpty()) {
            CacheMetricsLogger.miss(log, "post:list:" + type.name().toLowerCase(),
                    getCacheMetadata(type).key(), "ordered_ids_empty");
            return Collections.emptyList();
        }

        List<PostSimpleDetail> cachedPosts = orderedIds.stream()
                .map(cachedMap::get)
                .filter(Objects::nonNull)
                .toList();

        if (cachedPosts.isEmpty()) {
            CacheMetricsLogger.miss(log, "post:list:" + type.name().toLowerCase(),
                    getCacheMetadata(type).key(), "resolved_entries_empty");
        } else {
            CacheMetricsLogger.hit(log, "post:list:" + type.name().toLowerCase(),
                    getCacheMetadata(type).key(), cachedPosts.size());
        }
        return cachedPosts;
    }

    /**
     * <h3>전설 인기글 목록 조회</h3>
     * <p>레전드 게시글 목록을 페이지네이션으로 조회합니다.</p>
     * <p>postIds 저장소의 순서를 사용하여 페이징 및 정렬합니다.</p>
     *
     * @param pageable 페이지 정보
     * @return 캐시된 레전드 게시글 목록 페이지
     */
    public Page<PostSimpleDetail> getCachedPostListPaged(Pageable pageable) {
        CacheMetadata metadata = getCacheMetadata(PostCacheFlag.LEGEND);
        // 1. Hash에서 모든 PostSimpleDetail 조회
        Map<Object, Object> hashEntries = redisTemplate.opsForHash().entries(metadata.key());
        if (hashEntries.isEmpty()) {
            CacheMetricsLogger.miss(log, "post:legend:list", metadata.key(), "hash_empty");
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        // 2. postIds 저장소에서 전체 순서 가져오기
        List<Long> orderedIds = redisTier2PostStoreAdapter.getStoredPostIds(PostCacheFlag.LEGEND);
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
                .filter(Objects::nonNull)
                .toList();

        if (pagedPosts.isEmpty()) {
            CacheMetricsLogger.miss(log, "post:legend:list", metadata.key(), "resolved_entries_empty");
        } else {
            CacheMetricsLogger.hit(log, "post:legend:list", metadata.key(), pagedPosts.size());
        }

        return new PageImpl<>(pagedPosts, pageable, orderedIds.size());
    }

    /**
     * <h3>캐시 목록 저장</h3>
     * <p>인기글 목록을 Redis Hash에 저장합니다 (TTL 5분)</p>
     * <p>Hash 구조: Field는 postId, Value는 PostSimpleDetail 객체</p>
     * <p>조회 시 postIds 저장소의 순서를 사용하여 정렬합니다.</p>
     *
     * @param type  캐시할 게시글 유형 (REALTIME, WEEKLY, LEGEND, NOTICE)
     * @param posts 캐시할 게시글 목록 (PostSimpleDetail)
     */
    public void cachePostList(PostCacheFlag type, List<PostSimpleDetail> posts) {
        if (posts == null || posts.isEmpty()) {
            return;
        }

        RedisPostKeys.CacheMetadata metadata = getCacheMetadata(type);
        log.warn("[CACHE_WRITE] START - type={}, count={}, key={}, thread={}",
                type, posts.size(), metadata.key(), Thread.currentThread().getName());

        // Hash에 PostSimpleDetail 저장 (HSET)
        String hashKey = metadata.key();
        for (PostSimpleDetail post : posts) {
            redisTemplate.opsForHash().put(hashKey, post.getId().toString(), post);
        }
        // TTL 설정
        redisTemplate.expire(hashKey, metadata.ttl());

        log.warn("[CACHE_WRITE] SUCCESS - type={}, key={}, ttl={}min",
                type, metadata.key(), metadata.ttl().toMinutes());
    }

    /**
     * <h3>단일 캐시 삭제</h3>
     * <p>모든 Redis Hash에서 특정 postId의 PostSimpleDetail을 삭제합니다.</p>
     * <p>모든 PostCacheFlag를 순회하며 각 타입의 목록 캐시에서 제거합니다.</p>
     * <p>게시글 수정/삭제 시 목록 캐시 무효화를 위해 호출됩니다.</p>
     *
     * @param postId 제거할 게시글 ID
     */
    public void removePostFromListCache(Long postId) {
        for (PostCacheFlag type : PostCacheFlag.values()) {
            String hashKey = RedisPostKeys.CACHE_METADATA_MAP.get(type).key();
            redisTemplate.opsForHash().delete(hashKey, postId.toString());
        }
    }

    /**
     * <h3>캐시 삭제</h3>
     * <p>특정 캐시 유형의 posts:{type} Hash 전체를 삭제합니다.</p>
     * <p>스케줄러 재실행 시 기존 목록 캐시를 초기화하기 위해 호출됩니다.</p>
     *
     * @param type 삭제할 캐시 유형 (REALTIME, WEEKLY, LEGEND, NOTICE)
     */
    public void clearPostListCache(PostCacheFlag type) {
        String hashKey = RedisPostKeys.CACHE_METADATA_MAP.get(type).key();
        redisTemplate.delete(hashKey);
    }

    /**
     * <h3>캐시 메타데이터 조회</h3>
     * <p>주어진 캐시 유형에 해당하는 메타데이터를 조회합니다.</p>
     *
     * @param type 게시글 캐시 유형
     * @return 캐시 메타데이터
     * @throws CustomException 알 수 없는 PostCacheFlag 유형인 경우
     */
    private CacheMetadata getCacheMetadata(PostCacheFlag type) {
        CacheMetadata metadata = cacheMetadataMap.get(type);
        if (metadata == null) {
            throw new CustomException(ErrorCode.POST_REDIS_READ_ERROR);
        }
        return metadata;
    }
}
