package jaeik.bimillog.infrastructure.redis.post;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.entity.jpa.PostCacheFlag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * <h2>실시간 인기글 Hash 캐시 조회 (서킷브레이커 적용)</h2>
 * <p>RedisSimplePostAdapter의 실시간 인기글 조회를 서킷브레이커로 감쌉니다.</p>
 * <p>서킷 OPEN 시 빈 리스트를 반환하여 Redis 타임아웃을 방지합니다.</p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RedisRealtimePostCacheReader {
    private final RedisSimplePostAdapter redisSimplePostAdapter;

    /**
     * <h3>실시간 인기글 Hash 캐시 조회</h3>
     * <p>realtimeRedis 서킷이 OPEN이면 Redis 호출 없이 빈 리스트를 반환합니다.</p>
     *
     * @return 실시간 인기글 목록 (서킷 OPEN 시 빈 리스트)
     */
    @CircuitBreaker(name = "realtimeRedis", fallbackMethod = "getRealtimeCachedPostsFallback")
    public List<PostSimpleDetail> getRealtimeCachedPosts() {
        return redisSimplePostAdapter.getAllCachedPostsList(PostCacheFlag.REALTIME);
    }

    private List<PostSimpleDetail> getRealtimeCachedPostsFallback(Throwable t) {
        log.warn("[CIRCUIT_FALLBACK] 실시간 인기글 Hash 캐시 조회 실패: {}", t.getMessage());
        return Collections.emptyList();
    }
}
