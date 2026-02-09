package jaeik.bimillog.domain.post.async;

import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.post.RedisPostViewAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>게시글 조회수 증가 리스너</h2>
 * <p>게시글 조회 이벤트를 수신하여 중복 조회 방지 후 조회수를 증가시킵니다.</p>
 * <p>Redis SET으로 24시간 중복 조회를 방지하고, Redis Hash에 조회수를 버퍼링합니다.</p>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@Log(logResult = false, level = Log.LogLevel.DEBUG, message = "조회수 증가")
@Component
@RequiredArgsConstructor
@Slf4j
public class PostViewCountSync {
    private final RedisPostViewAdapter redisPostViewAdapter;

    /**
     * <h3>게시글 조회 이벤트 처리 (원자적)</h3>
     * <p>Lua 스크립트로 중복 확인 + 마킹 + 조회수 증가를 원자적으로 처리합니다.</p>
     * <p>동시 요청 시 Check-Then-Act 레이스 컨디션을 방지합니다.</p>
     *
     * @param postId    조회된 게시글 ID
     * @param viewerKey 조회자 식별 키 (중복 조회 방지용)
     */
    @Async("realtimeEventExecutor")
    @Transactional
    public void handlePostViewed(Long postId, String viewerKey) {
        try {
            redisPostViewAdapter.markViewedAndIncrement(postId, viewerKey);
        } catch (Exception e) {
            log.warn("조회수 처리 실패: postId={}, error={}", postId, e.getMessage());
        }
    }
}
