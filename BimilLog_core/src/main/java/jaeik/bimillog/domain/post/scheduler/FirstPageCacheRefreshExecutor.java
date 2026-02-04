package jaeik.bimillog.domain.post.scheduler;

import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostReadModelQueryRepository;
import jaeik.bimillog.infrastructure.redis.post.RedisFirstPagePostAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.List;

import static jaeik.bimillog.infrastructure.redis.post.RedisPostKeys.FIRST_PAGE_SIZE;

/**
 * <h2>첫 페이지 캐시 갱신 실행기</h2>
 * <p>DB에서 첫 페이지 게시글을 조회하여 Redis 캐시를 갱신합니다.</p>
 * <p>실패 시 지수 백오프(1s -> 3s -> 9s -> 27s -> 81s -> 243s)로 최대 7회 재시도합니다.</p>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FirstPageCacheRefreshExecutor {

    private final PostReadModelQueryRepository postReadModelQueryRepository;
    private final RedisFirstPagePostAdapter redisFirstPagePostAdapter;

    /**
     * <h3>첫 페이지 캐시 갱신</h3>
     * <p>DB에서 최신 20개 게시글을 조회하여 Redis 캐시를 갱신합니다.</p>
     * <p>DB/Redis 실패 시 지수 백오프(1s -> 3s -> 9s -> 27s -> 81s -> 243s)로 최대 7회 시도합니다.</p>
     */
    @Retryable(
            retryFor = {
                    TransientDataAccessException.class,
                    DataAccessResourceFailureException.class,
                    QueryTimeoutException.class,
                    RedisConnectionFailureException.class
            },
            maxAttempts = 7,
            backoff = @Backoff(delay = 1000, multiplier = 3)
    )
    public void refreshFirstPage() {
        log.debug("[FIRST_PAGE_CACHE] 캐시 갱신 시작");

        // DB에서 최신 20개 조회 (+1은 hasNext 판단용이므로 정확히 20개만)
        List<PostSimpleDetail> posts = postReadModelQueryRepository.findBoardPostsByCursor(null, FIRST_PAGE_SIZE);

        // 21개 이상이면 20개로 자르기 (findBoardPostsByCursor가 size+1 반환)
        if (posts.size() > FIRST_PAGE_SIZE) {
            posts = posts.subList(0, FIRST_PAGE_SIZE);
        }

        redisFirstPagePostAdapter.refreshCache(posts);
        log.debug("[FIRST_PAGE_CACHE] 캐시 갱신 완료: {}개", posts.size());
    }

    /**
     * <h3>캐시 갱신 최종 실패 복구</h3>
     * <p>7회 재시도 후에도 실패 시 로그를 남기고 종료합니다.</p>
     * <p>다음 스케줄러 실행 시 다시 시도됩니다.</p>
     */
    @Recover
    public void recoverRefreshFirstPage(Exception e) {
        log.error("[FIRST_PAGE_CACHE] 캐시 갱신 최종 실패 (7회 시도): {}", e.getMessage(), e);
    }
}
