package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.jpa.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.domain.post.scheduler.PostCacheRefreshScheduler;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisSimplePostAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;


/**
 * <h2>글 캐시 갱신 클래스</h2>
 * <p>인기글(주간/레전드) 목록 캐시의 동기 갱신을 담당합니다.</p>
 * <p>Post.featuredType 필드를 기반으로 단일 쿼리로 조회합니다.</p>
 * <p>스케줄러({@link PostCacheRefreshScheduler})에서 호출됩니다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Log(logResult = false, logExecutionTime = true, message = "캐시 갱신")
@Service
@RequiredArgsConstructor
@Slf4j
public class PostCacheRefresh {
    private final RedisSimplePostAdapter redisSimplePostAdapter;
    private final PostQueryRepository postQueryRepository;


    public static final Duration POST_CACHE_TTL_WEEKLY_LEGEND = RedisKey.POST_CACHE_TTL_WEEKLY_LEGEND;


    /**
     * <h3>주간/레전드/공지 캐시 동기 갱신</h3>
     * <p>Post 테이블에서 featuredType 기반으로 단일 쿼리 조회 후 캐시에 저장합니다.</p>
     * <p>실패 시 최대 3회 재시도합니다 (2s → 4s 지수 백오프).</p>
     *
     * @param type 캐시 유형 (WEEKLY, LEGEND, NOTICE)
     */
    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void refreshFeatured(PostCacheFlag type) {
        List<PostSimpleDetail> posts = postQueryRepository.findPostsByFeaturedType(type);
        if (posts.isEmpty()) {
            return;
        }
        Duration ttl = getTtlForType(type);
        redisSimplePostAdapter.cachePostsWithTtl(type, posts, ttl);
    }

    @Recover
    public void recoverRefreshRealtime(Exception e) {
        log.error("REALTIME 캐시 갱신 최종 실패 (3회 시도): {}", e.getMessage());
    }

    @Recover
    public void recoverRefreshFeatured(Exception e, PostCacheFlag type) {
        log.error("{} 캐시 갱신 최종 실패 (3회 시도): {}", type, e.getMessage());
    }

    /**
     * <h3>타입별 캐시 TTL 반환</h3>
     * <p>REALTIME/NOTICE: null (영구 저장), WEEKLY/LEGEND: 24시간 30분</p>
     *
     * @param type 게시글 캐시 유형
     * @return 해당 타입의 TTL (NOTICE는 null 반환 → 영구 저장)
     */
    public static Duration getTtlForType(PostCacheFlag type) {
        return switch (type) {
            case REALTIME, NOTICE -> null;
            case WEEKLY, LEGEND -> POST_CACHE_TTL_WEEKLY_LEGEND;
        };
    }
}
