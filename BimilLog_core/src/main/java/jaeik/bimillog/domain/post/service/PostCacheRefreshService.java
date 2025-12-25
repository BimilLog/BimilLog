package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.out.PostQueryRepository;
import jaeik.bimillog.infrastructure.redis.post.RedisPostQueryAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostSaveAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostUpdateAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * <h2>PostCacheRefreshService</h2>
 * <p>캐시 갱신 전용 비동기 서비스</p>
 * <p>Self-invocation 문제 해결을 위해 PostCacheService에서 분리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostCacheRefreshService {

    private final RedisPostSaveAdapter redisPostSaveAdapter;
    private final PostQueryRepository postQueryRepository;
    private final RedisPostQueryAdapter redisPostQueryAdapter;
    private final RedisPostUpdateAdapter redisPostUpdateAdapter;

    /**
     * <h3>비동기 캐시 갱신 (Redisson RLock 기반)</h3>
     * <p>Redisson RLock을 사용하여 여러 요청이 동시에 갱신을 시도해도 1회만 실행되도록 보장합니다.</p>
     * <p>백그라운드 스레드(cacheRefreshExecutor)에서 실행되므로 사용자 요청은 블로킹되지 않습니다.</p>
     * <p>락 획득 설정: waitTime=3초, leaseTime=5초</p>
     *
     * @param type 갱신할 캐시 유형 (REALTIME, WEEKLY, LEGEND, NOTICE)
     * @author Jaeik
     * @since 2.0.0
     */
    @Async("cacheRefreshExecutor")
    public void asyncRefreshCache(PostCacheFlag type) {
        // Step 1: 분산 락 획득 시도 (waitTime=3초, leaseTime=5초)

        boolean acquired = redisPostUpdateAdapter.tryAcquireCacheRefreshLock(PostCacheFlag.WEEKLY);

        if (!acquired) {
            log.info("다른 스레드가 캐시 갱신 중: type={}", PostCacheFlag.WEEKLY);
            return;
        }
        try {
            List<Long> storedPostIds;
            log.info("캐시 갱신 시작: 타입={}, 스레드={}", type, Thread.currentThread().getName());

            // Step 2: Tier 2 PostIds로부터 복구 실시간은 실시간 점수 Redis 저장소에서 복구
            if (type == PostCacheFlag.REALTIME) {
                storedPostIds = redisPostQueryAdapter.getRealtimePopularPostIds();
            } else {
                storedPostIds = redisPostQueryAdapter.getStoredPostIds(type);
            }

            if (storedPostIds.isEmpty()) {
                log.warn("캐시 갱신 실패 - 타입={}, 이유 = 2티어 저장소 비어있음", type);
                return;
            }

            // DB에서 PostDetail 조회 후 PostSimpleDetail 변환
            List<PostSimpleDetail> refreshed = storedPostIds.stream()
                    .map(postId -> postQueryRepository.findPostDetailWithCounts(postId, null).orElse(null))
                    .filter(Objects::nonNull)
                    .map(PostDetail::toSimpleDetail)
                    .toList();

            log.warn("DB로부터 응답반환 - 타입={}, 결과 사이즈={}, 스레드={}", type, refreshed.size(), Thread.currentThread().getName());

            if (refreshed.isEmpty()) {
                log.warn("캐시 갱신 실패: 타입={} DB 조회 결과가 없음", type);
                return;
            }

            // Step 3: 캐시 갱신
            redisPostSaveAdapter.cachePostList(type, refreshed);
            log.info("캐시 갱신 완료: 타입={}, count={}", type, refreshed.size());

        } catch (Exception e) {
            log.error("캐시 갱신 에러: 타입={}", type, e);
        } finally {
            // Step 4: 락 해제
            redisPostUpdateAdapter.releaseCacheRefreshLock(type);
        }
    }
}
