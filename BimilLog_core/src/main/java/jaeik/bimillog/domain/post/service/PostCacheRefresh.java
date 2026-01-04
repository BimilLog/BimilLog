package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.out.PostQueryRepository;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostStoreAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisTier1PostStoreAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisTier2PostStoreAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostCacheRefresh {
    private final PostQueryRepository postQueryRepository;
    private final RedisTier1PostStoreAdapter redisTier1PostStoreAdapter;
    private final RedisTier2PostStoreAdapter redisTier2PostStoreAdapter;
    private final RedisRealTimePostStoreAdapter redisRealTimePostStoreAdapter;

    /**
     * <h3>비동기 캐시 갱신 (PER 기반)</h3>
     * <p>확률적 선계산 기법에서 TTL 임계값 이하일 때 호출되는 비동기 캐시 갱신 메서드입니다.</p>
     * <p>백그라운드에서 실행되므로 사용자 요청은 블로킹되지 않습니다.</p>
     *
     * @param type 갱신할 캐시 유형 (REALTIME, WEEKLY, LEGEND, NOTICE)
     */
    @Async("cacheRefreshExecutor")
    public void asyncRefreshCache(PostCacheFlag type) {
        try {
            List<Long> storedPostIds;
            log.info("캐시 갱신 시작: 타입={}, 스레드={}", type, Thread.currentThread().getName());

            // Step 1: Tier 2 PostIds로부터 복구 (실시간은 실시간 점수 Redis 저장소에서 복구)
            if (type == PostCacheFlag.REALTIME) {
                storedPostIds = redisRealTimePostStoreAdapter.getRealtimePopularPostIds();
            } else {
                storedPostIds = redisTier2PostStoreAdapter.getStoredPostIds(type);
            }

            if (storedPostIds.isEmpty()) {
                log.warn("캐시 갱신 실패 - 타입={}, 이유=2티어 저장소 비어있음", type);
                return;
            }

            // Step 2: DB에서 PostDetail 조회 후 PostSimpleDetail 변환
            List<PostSimpleDetail> refreshed = storedPostIds.stream()
                    .map(postId -> postQueryRepository.findPostDetailWithCounts(postId, null).orElse(null))
                    .filter(Objects::nonNull)
                    .map(PostDetail::toSimpleDetail)
                    .toList();

            log.info("DB로부터 응답 반환 - 타입={}, 결과 사이즈={}, 스레드={}", type, refreshed.size(), Thread.currentThread().getName());

            if (refreshed.isEmpty()) {
                log.warn("캐시 갱신 실패: 타입={}, 이유=DB 조회 결과 없음", type);
                return;
            }

            // Step 3: 캐시 갱신
            redisTier1PostStoreAdapter.cachePostList(type, refreshed);
            log.info("캐시 갱신 완료: 타입={}, count={}", type, refreshed.size());

        } catch (Exception e) {
            log.error("캐시 갱신 에러: 타입={}", type, e);
        }
    }
}
