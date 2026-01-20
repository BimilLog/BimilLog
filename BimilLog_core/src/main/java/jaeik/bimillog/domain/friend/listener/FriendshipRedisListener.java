package jaeik.bimillog.domain.friend.listener;

import jaeik.bimillog.domain.friend.event.FriendshipCreatedEvent;
import jaeik.bimillog.domain.friend.event.FriendshipDeletedEvent;
import jaeik.bimillog.infrastructure.redis.friend.RedisFriendshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import jaeik.bimillog.infrastructure.log.Log;

/**
 * <h2>친구 관계 Redis 동기화 리스너</h2>
 * <p>친구 요청 수락, 친구 삭제 이벤트를 수신하여 Redis 친구 관계 테이블을 동기화합니다.</p>
 * <p>Redis에는 양방향 친구 관계가 Set으로 저장됩니다.</p>
 * <p>Key 패턴: {@code friendship:{memberId}}</p>
 * <p>Value: Set&lt;friendId&gt;</p>
 *
 * @author Jaeik
 * @version 2.5.0
 */
@Log(logResult = false, message = "친구 관계 Redis 동기화")
@Component
@RequiredArgsConstructor
@Slf4j
public class FriendshipRedisListener {

    private final RedisFriendshipRepository redisFriendshipRepository;

    /**
     * <h3>친구 관계 생성 이벤트 처리</h3>
     * <p>친구 요청 수락 시 Redis 친구 관계 테이블에 양방향으로 추가합니다.</p>
     * <p>{@link FriendshipCreatedEvent} 발행 시 비동기로 호출됩니다.</p>
     *
     * @param event 친구 관계 생성 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @EventListener
    @Async
    @Retryable(
            retryFor = RedisConnectionFailureException.class,
            maxAttemptsExpression = "${retry.max-attempts}",
            backoff = @Backoff(delayExpression = "${retry.backoff.delay}", multiplierExpression = "${retry.backoff.multiplier}")
    )
    public void handleFriendshipCreated(FriendshipCreatedEvent event) {
        redisFriendshipRepository.addFriend(event.memberId(), event.friendId());
        log.debug("Redis 친구 관계 추가 완료: memberId={}, friendId={}", event.memberId(), event.friendId());
    }

    @Recover
    public void recoverFriendshipCreated(Exception e, FriendshipCreatedEvent event) {
        log.error("Redis 친구 관계 추가 최종 실패: memberId={}, friendId={}", event.memberId(), event.friendId(), e);
    }

    /**
     * <h3>친구 관계 삭제 이벤트 처리</h3>
     * <p>친구 삭제 시 Redis 친구 관계 테이블에서 양방향으로 삭제합니다.</p>
     * <p>{@link FriendshipDeletedEvent} 발행 시 비동기로 호출됩니다.</p>
     *
     * @param event 친구 관계 삭제 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @EventListener
    @Async
    @Retryable(
            retryFor = RedisConnectionFailureException.class,
            maxAttemptsExpression = "${retry.max-attempts}",
            backoff = @Backoff(delayExpression = "${retry.backoff.delay}", multiplierExpression = "${retry.backoff.multiplier}")
    )
    public void handleFriendshipDeleted(FriendshipDeletedEvent event) {
        redisFriendshipRepository.deleteFriend(event.memberId(), event.friendId());
        log.debug("Redis 친구 관계 삭제 완료: memberId={}, friendId={}", event.memberId(), event.friendId());
    }

    @Recover
    public void recoverFriendshipDeleted(Exception e, FriendshipDeletedEvent event) {
        log.error("Redis 친구 관계 삭제 최종 실패: memberId={}, friendId={}", event.memberId(), event.friendId(), e);
    }
}
