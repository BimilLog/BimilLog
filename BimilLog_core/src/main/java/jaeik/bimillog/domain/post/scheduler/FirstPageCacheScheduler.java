package jaeik.bimillog.domain.post.scheduler;

import jaeik.bimillog.infrastructure.redis.post.RedisFirstPagePostAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * <h2>첫 페이지 캐시 스케줄러</h2>
 * <p>30분마다 게시판 첫 페이지 캐시를 갱신합니다.</p>
 * <p>분산 환경에서 하나의 인스턴스만 갱신하도록 분산 락을 사용합니다.</p>
 *
 * <h3>동작 방식:</h3>
 * <ul>
 *     <li>fixedRate: 서버 시작 즉시 첫 실행 -> 이후 30분 간격</li>
 *     <li>서버 시작 시 캐시가 즉시 채워짐 (캐시 미스 없음)</li>
 *     <li>분산 락으로 다중 인스턴스 중복 갱신 방지</li>
 * </ul>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FirstPageCacheScheduler {

    private final RedisFirstPagePostAdapter redisFirstPagePostAdapter;
    private final FirstPageCacheRefreshExecutor firstPageCacheRefreshExecutor;

    /**
     * <h3>30분마다 첫 페이지 캐시 갱신</h3>
     * <p>fixedRate: 서버 시작 즉시 첫 실행 -> 이후 30분 간격</p>
     * <p>분산 락 획득 실패 시 다른 인스턴스가 갱신 중이므로 스킵</p>
     */
    @Scheduled(fixedRate = 1800000)  // 30분 = 1,800,000ms
    public void refreshFirstPageCache() {
        // 분산 락 획득 시도
        if (!redisFirstPagePostAdapter.tryAcquireRefreshLock()) {
            log.debug("[FIRST_PAGE_CACHE] 다른 인스턴스가 갱신 중이므로 스킵");
            return;
        }

        try {
            firstPageCacheRefreshExecutor.refreshFirstPage();
        } finally {
            redisFirstPagePostAdapter.releaseRefreshLock();
        }
    }
}
