package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostStoreAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisTier1PostStoreAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisTier2PostStoreAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

import jaeik.bimillog.infrastructure.log.Log;

/**
 * <h2>글 캐시 갱신 클래스</h2>
 * <p>개별 게시글 캐시의 비동기 갱신을 담당합니다.</p>
 * <p>PER(Probabilistic Early Refresh) 기반으로 호출됩니다.</p>
 *
 * @author Jaeik
 * @version 2.5.0
 */
@Log(logResult = false, message = "캐시 갱신")
@Service
@RequiredArgsConstructor
@Slf4j
public class PostCacheRefresh {
    private final PostQueryRepository postQueryRepository;
    private final RedisTier1PostStoreAdapter redisTier1PostStoreAdapter;
    private final RedisTier2PostStoreAdapter redisTier2PostStoreAdapter;
    private final RedisRealTimePostStoreAdapter redisRealTimePostStoreAdapter;

    /**
     * <h3>특정 게시글들의 캐시 비동기 갱신 (PER 기반)</h3>
     * <p>개별 게시글 캐시가 만료 임박 시 호출됩니다.</p>
     * <p>백그라운드에서 실행되므로 사용자 요청은 블로킹되지 않습니다.</p>
     *
     * @param type    캐시 유형
     * @param postIds 갱신할 게시글 ID 목록
     */
    @Async("cacheRefreshExecutor")
    public void asyncRefreshPosts(PostCacheFlag type, List<Long> postIds) {
        try {
            log.info("[PER_REFRESH] 시작 - type={}, count={}, thread={}",
                    type, postIds.size(), Thread.currentThread().getName());

            // DB에서 조회 후 개별 캐시 저장
            List<PostSimpleDetail> refreshed = postIds.stream()
                    .map(postId -> postQueryRepository.findPostDetail(postId, null).orElse(null))
                    .filter(Objects::nonNull)
                    .map(PostDetail::toSimpleDetail)
                    .toList();

            if (refreshed.isEmpty()) {
                log.warn("[PER_REFRESH] 실패 - type={}, 이유=DB 조회 결과 없음", type);
                return;
            }

            // Pipeline으로 캐시 저장
            redisTier1PostStoreAdapter.cachePosts(type, refreshed);

            log.info("[PER_REFRESH] 완료 - type={}, count={}", type, refreshed.size());

        } catch (Exception e) {
            log.error("[PER_REFRESH] 에러 - type={}", type, e);
        }
    }
}
