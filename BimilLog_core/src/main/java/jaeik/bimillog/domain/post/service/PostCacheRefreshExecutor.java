package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.jpa.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.post.RedisPostKeys;
import jaeik.bimillog.infrastructure.redis.post.RedisSimplePostAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * <h2>캐시 갱신 실행기</h2>
 * <p>순수 Redis 캐시 저장만 담당합니다.</p>
 * <p>DB 조회는 {@link PostCacheRefresh}에서 수행하고, 조회된 데이터를 전달받아 캐시에 저장합니다.</p>
 * <p>재시도는 호출자인 {@link PostCacheRefresh}의 {@code @Retryable}이 담당합니다.</p>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@Log(logResult = false, logExecutionTime = true, message = "캐시 저장")
@Component
@RequiredArgsConstructor
@Slf4j
public class PostCacheRefreshExecutor {
    private final RedisSimplePostAdapter redisSimplePostAdapter;

    /**
     * <h3>타입별 TTL 적용 캐시 저장 (스케줄러용)</h3>
     * <p>RedisPostKeys.getTtlForType()으로 TTL을 결정하여 cachePostsWithTtl()로 저장합니다.</p>
     *
     * @param type  캐시 유형
     * @param posts 캐시에 저장할 게시글 목록
     */
    public void cachePostsWithType(PostCacheFlag type, List<PostSimpleDetail> posts) {
        if (posts.isEmpty()) {
            return;
        }
        Duration ttl = RedisPostKeys.getTtlForType(type);
        redisSimplePostAdapter.cachePostsWithTtl(type, posts, ttl);
    }
}
