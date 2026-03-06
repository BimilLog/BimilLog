package jaeik.bimillog.domain.friend.listener;

import io.lettuce.core.RedisCommandTimeoutException;
import jaeik.bimillog.domain.friend.event.FriendEvent.FriendshipCreatedEvent;
import jaeik.bimillog.domain.friend.event.FriendEvent.FriendshipDeletedEvent;
import jaeik.bimillog.domain.friend.service.FriendEventDlqService;
import jaeik.bimillog.infrastructure.redis.friend.RedisFriendshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * <h2>친구관계 레디스 업데이트 클래스</h2>
 * <P>SADD 명령어는 어차피 멱등성이 있다.</P>
 * @author Jaeik
 * @version 2.8.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FriendshipListener {
    private final RedisFriendshipRepository redisFriendshipRepository;
    private final FriendEventDlqService friendEventDlqService;

    /**
     * <h3>친구 관계 생성 이벤트 처리</h3>
     */
    @TransactionalEventListener
    @Async("friendUpdateExecutor")
    @Retryable(
            retryFor = {
                    RedisConnectionFailureException.class,
                    RedisSystemException.class,
                    RedisCommandTimeoutException.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 100),
            recover = "recoverAddFriend"
    )
    public void handleFriendshipCreated(FriendshipCreatedEvent event) {
        redisFriendshipRepository.addFriend(event.memberId(), event.friendId());
    }

    @Recover
    public void recoverAddFriend(Exception e, FriendshipCreatedEvent event) {
        log.error("Redis 친구 관계 추가 재시도 실패 DLQ 진입: memberId={}, friendId={}", event.memberId(), event.friendId(), e);
        friendEventDlqService.saveFriendAdd(event.memberId(), event.friendId());
    }

    /**
     * <h3>친구 관계 삭제 이벤트 처리</h3>
     */
    @TransactionalEventListener
    @Async("friendUpdateExecutor")
    @Retryable(
            retryFor = {
                    RedisConnectionFailureException.class,
                    RedisSystemException.class,
                    RedisCommandTimeoutException.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 100),
            recover = "recoverDeleteFriend"
    )
    public void handleFriendshipDeleted(FriendshipDeletedEvent event) {
        redisFriendshipRepository.deleteFriend(event.memberId1(), event.memberId2());
    }

    @Recover
    public void recoverDeleteFriend(Exception e, FriendshipDeletedEvent event) {
        log.error("Redis 친구 관계 삭제 재시도 실패 DLQ 진입: memberId={}, friendId={}", event.memberId1(), event.memberId2(), e);
        friendEventDlqService.saveFriendRemove(event.memberId1(), event.memberId2());
    }
}
