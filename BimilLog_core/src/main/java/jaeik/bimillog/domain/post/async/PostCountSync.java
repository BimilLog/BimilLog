package jaeik.bimillog.domain.post.async;

import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostCounterAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostUpdateAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * <h2>게시글 조회수 증가 리스너</h2>
 * <p>게시글 조회 이벤트를 수신하여 중복 조회 방지 후 조회수를 증가시킵니다.</p>
 * <p>Redis SET으로 24시간 중복 조회를 방지하고, Redis Hash에 조회수를 버퍼링합니다.</p>
 * <p>글 단위 Hash 캐시는 1분 플러시 스케줄러에서 일괄 반영합니다.</p>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@Log(logResult = false, level = Log.LogLevel.DEBUG, message = "조회수 증가")
@Component
@RequiredArgsConstructor
@Slf4j
public class PostCountSync {
    private final RedisPostUpdateAdapter redisPostUpdateAdapter;
    private final RedisPostCounterAdapter redisPostCounterAdapter;

    /**
     * <h3>게시글 조회 이벤트 처리 (원자적)</h3>
     * <p>Lua 스크립트로 중복 확인 + 마킹 + 조회수 증가를 원자적으로 처리합니다.</p>
     * <p>Hash 캐시 반영은 1분 플러시 스케줄러에서 일괄 처리합니다.</p>
     * <p>동시 요청 시 Check-Then-Act 레이스 컨디션을 방지합니다.</p>
     *
     * @param postId    조회된 게시글 ID
     * @param viewerKey 조회자 식별 키 (중복 조회 방지용)
     */
    @Async("cacheCountUpdateExecutor")
    public void handlePostViewed(Long postId, String viewerKey) {
        try {
            redisPostUpdateAdapter.markViewedAndIncrement(postId, viewerKey);
        } catch (Exception e) {
            log.warn("조회수 처리 실패: postId={}, error={}", postId, e.getMessage());
        }
    }

    /**
     * <h3>좋아요 카운터 캐시 증감</h3>
     * <p>캐시글인 경우에만 post:counters Hash의 {postId}:like 필드를 HINCRBY로 증감합니다.</p>
     * <p>Lua 스크립트로 5개 카테고리 SET을 한 번에 확인하여 캐시글 여부를 판단합니다.</p>
     *
     * @param postId 게시글 ID
     * @param delta  증감값 (1: 좋아요 추가, -1: 좋아요 취소)
     */
    @Async("cacheCountUpdateExecutor")
    public void incrementLikeCounter(Long postId, long delta) {
        try {
            if (redisPostCounterAdapter.isCachedPost(postId)) {
                redisPostCounterAdapter.incrementCounter(postId, RedisKey.COUNTER_SUFFIX_LIKE, delta);
            }
        } catch (Exception e) {
            log.warn("좋아요 카운터 캐시 증감 실패: postId={}, delta={}, error={}", postId, delta, e.getMessage());
        }
    }

    /**
     * <h3>댓글 카운터 캐시 증감</h3>
     * <p>캐시글인 경우에만 post:counters Hash의 {postId}:comment 필드를 HINCRBY로 증감합니다.</p>
     * <p>Lua 스크립트로 5개 카테고리 SET을 한 번에 확인하여 캐시글 여부를 판단합니다.</p>
     *
     * @param postId 게시글 ID
     * @param delta  증감값 (1: 댓글 작성, -1: 댓글 삭제)
     */
    @Async("cacheCountUpdateExecutor")
    public void incrementCommentCounter(Long postId, long delta) {
        try {
            if (redisPostCounterAdapter.isCachedPost(postId)) {
                redisPostCounterAdapter.incrementCounter(postId, RedisKey.COUNTER_SUFFIX_COMMENT, delta);
            }
        } catch (Exception e) {
            log.warn("댓글 카운터 캐시 증감 실패: postId={}, delta={}, error={}", postId, delta, e.getMessage());
        }
    }
}
