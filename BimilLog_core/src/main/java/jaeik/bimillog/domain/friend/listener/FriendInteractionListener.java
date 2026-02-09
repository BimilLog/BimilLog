package jaeik.bimillog.domain.friend.listener;

import io.lettuce.core.RedisCommandTimeoutException;
import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.comment.event.CommentLikeEvent;
import jaeik.bimillog.domain.friend.service.FriendEventDlqService;
import jaeik.bimillog.domain.global.event.FriendInteractionEvent;
import jaeik.bimillog.domain.post.event.PostLikeEvent;
import jaeik.bimillog.infrastructure.redis.friend.RedisInteractionScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import jaeik.bimillog.infrastructure.log.Log;


/**
 * <h2>친구 상호작용 점수 관리 리스너</h2>
 * <p>게시글 좋아요, 댓글 작성, 댓글 좋아요 이벤트를 수신하여 Redis에 상호작용 점수를 기록합니다.</p>
 * <p>친구 추천 알고리즘에서 사용되는 상호작용 점수를 실시간으로 업데이트합니다.</p>
 * <p>익명 사용자의 상호작용은 점수에 반영되지 않습니다.</p>
 * <p>각 상호작용당 +0.5점, 최대 10점까지 증가합니다.</p>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@Log(logResult = false, level = Log.LogLevel.DEBUG, message = "친구 상호작용 점수")
@Component
@RequiredArgsConstructor
@Slf4j
public class FriendInteractionListener {
    private final RedisInteractionScoreRepository redisInteractionScoreRepository;
    private final FriendEventDlqService friendEventDlqService;

    public static final Double INTERACTION_SCORE_DEFAULT = 0.5; // 상호 작용 점수 증가 기본 값

    /**
     * <h3>상호작용 점수 증가</h3>
     * <p>댓글 추천, 글 추천, 댓글 작성의 경우 FriendInteractionEvent로 추상화</p>
     * <p>자기자신이나 익명 게시글, 댓글의 경우 점수가 증가하지 않습니다.</p>
     *
     * @param event 게시글 좋아요 이벤트
     */
    @EventListener
    @Async("friendUpdateExecutor")
    @Retryable(
            retryFor = {
                    RedisConnectionFailureException.class,
                    RedisSystemException.class,
                    RedisCommandTimeoutException.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 1.5))
    public void handlePostLiked(FriendInteractionEvent event) {
        boolean processed = redisInteractionScoreRepository.addInteractionScore(event.getMemberId(), event.getTargetMemberId(), event.getIdempotencyKey());
        if (!processed) {
            event.getAlreadyProcess();
        }
    }

    @Recover
    public void recoverPostLiked(Exception e, FriendInteractionEvent event) {
        event.getDlqMessage(e);
        friendEventDlqService.saveScoreUp(event.getIdempotencyKey(), event.getMemberId(), event.getTargetMemberId(), INTERACTION_SCORE_DEFAULT);
    }
}
