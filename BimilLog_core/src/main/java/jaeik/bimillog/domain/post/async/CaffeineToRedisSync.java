package jaeik.bimillog.domain.post.async;

import jaeik.bimillog.domain.post.repository.RealtimeScoreFallbackStore;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.post.RedisPostRealTimeAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * <h2>실시간 인기글 점수 업데이트</h2>
 * <p>게시글 조회, 댓글 작성, 추천 이벤트를 수신하여 실시간 인기글 점수를 업데이트합니다.</p>
 * <p>조회: +2점, 댓글: +3점/-3점, 추천: +4점/-4점</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Log(logResult = false, level = Log.LogLevel.DEBUG, message = "실시간 인기글 점수")
@Component
@RequiredArgsConstructor
@Slf4j
public class CaffeineToRedisSync {
    private final RedisPostRealTimeAdapter redisPostRealTimeAdapter;
    private final RealtimeScoreFallbackStore realtimeScoreFallbackStore;

    /**
     * <h3>서킷 CLOSED 전환 시 Caffeine → Redis 동기화</h3>
     * <p>OPEN 구간에 Caffeine에 쌓인 증분 점수를 Redis에 반영하고,
     * 삭제된 게시글을 Redis에서 제거합니다.</p>
     */
    @Async("circuitSyncExecutor")
    public void syncCaffeineToRedis() {
        try {
            Map<Long, Double> deltaScores = realtimeScoreFallbackStore.getDeltaScores();
            if (!deltaScores.isEmpty()) {
                redisPostRealTimeAdapter.syncCaffeineScoresToRedis(deltaScores);
            }

            Set<Long> deletedIds = realtimeScoreFallbackStore.getDeletedPostIds();
            if (!deletedIds.isEmpty()) {
                redisPostRealTimeAdapter.replayDeletionsToRedis(deletedIds);
            }

            log.info("[CIRCUIT] CLOSED 전환: Caffeine 점수 동기화 및 삭제 재처리 완료 (점수: {}건, 삭제: {}건)",
                    deltaScores.size(), deletedIds.size());
        } catch (Exception e) {
            log.error("[CIRCUIT] Caffeine → Redis 동기화 실패 (다음 웜업 주기에 자연 보정)", e);
        }
    }
}
