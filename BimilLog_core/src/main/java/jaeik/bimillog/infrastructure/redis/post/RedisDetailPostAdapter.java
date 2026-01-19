package jaeik.bimillog.infrastructure.redis.post;

import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.infrastructure.log.CacheMetricsLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static jaeik.bimillog.infrastructure.redis.post.RedisPostKeys.POST_CACHE_TTL;
import static jaeik.bimillog.infrastructure.redis.post.RedisPostKeys.getPostDetailKey;

/**
 * <h2>레디스 게시글 상세 저장소 어댑터</h2>
 * <p>Tier1 상세 캐시(PostDetail)를 관리합니다.</p>
 * <p>캐시 미스 시 PostQueryService에서 저장합니다.</p>
 *
 * @author Jaeik
 * @version 2.5.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RedisDetailPostAdapter {
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * PER의 expiry gap (초 단위)
     * <p>TTL 마지막 60초 동안 확률적으로 캐시를 갱신합니다.</p>
     */
    private static final long PER_EXPIRY_GAP_SECONDS = 60;

    /**
     * <h3>캐시 조회</h3>
     * <p>Redis에서 게시글 상세 정보를 조회합니다. 캐시가 없으면 null을 반환합니다.</p>
     *
     * @param postId 조회할 게시글 ID
     * @return 캐시된 게시글 상세 정보 (없으면 null)
     */
    public PostDetail getCachedPostIfExists(Long postId) {
        String key = getPostDetailKey(postId);
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof PostDetail postDetail) {
            CacheMetricsLogger.hit(log, "post:detail", postId);
            return postDetail;
        }
        CacheMetricsLogger.miss(log, "post:detail", postId, "value_not_found");
        return null;
    }

    /**
     * <h3>캐시 저장</h3>
     * <p>게시글 상세 정보를 Redis 캐시에 저장합니다 (캐시 어사이드 패턴).</p>
     *
     * @param postDetail 캐시할 게시글 상세 정보
     */
    public void saveCachePost(PostDetail postDetail) {
        String key = getPostDetailKey(postDetail.getId());
        redisTemplate.opsForValue().set(key, postDetail, POST_CACHE_TTL);
    }

    /**
     * <h3>캐시 삭제</h3>
     * <p>특정 게시글의 캐시 데이터를 Redis에서 삭제합니다 (라이트 어라운드 패턴).</p>
     *
     * @param postId 캐시를 삭제할 게시글 ID
     */
    public void deleteCachePost(Long postId) {
        String detailKey = getPostDetailKey(postId);
        redisTemplate.delete(detailKey);
    }

    /**
     * <h3>PER 기반 갱신 필요 여부 확인</h3>
     * <p>상세 캐시의 TTL을 확인하여 확률적으로 갱신이 필요한지 판단합니다.</p>
     * <p>TTL 마지막 60초 동안 확률적으로 true를 반환합니다.</p>
     *
     * @param postId 확인할 게시글 ID
     * @return 갱신이 필요하면 true
     */
    public boolean shouldRefresh(Long postId) {
        String key = getPostDetailKey(postId);
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);

        if (ttl == null || ttl <= 0) {
            return true;
        }

        if (ttl < PER_EXPIRY_GAP_SECONDS) {
            double randomFactor = ThreadLocalRandom.current().nextDouble();
            return ttl - (randomFactor * PER_EXPIRY_GAP_SECONDS) <= 0;
        }

        return false;
    }
}
