package jaeik.bimillog.domain.friend.service;

import io.lettuce.core.RedisCommandTimeoutException;
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

/**
 * <h2>친구관계 레디스 업데이트 클래스</h2>
 * <P>SADD 명령어는 어차피 멱등성이 있다.</P>
 * @author Jaeik
 * @version 2.7.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FriendshipRedisUpdate {
    private final RedisFriendshipRepository redisFriendshipRepository;
    private final FriendEventDlqService friendEventDlqService;

    /**
     * <h3>레디스에 친구 관계 추가</h3>
     */
    @Async("friendUpdateExecutor")
    @Retryable(
            retryFor = {
                    RedisConnectionFailureException.class,
                    RedisSystemException.class,
                    RedisCommandTimeoutException.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 1.5),
            recover = "recoverAddFriend"
    )
    public void addFriendToRedis(Long memberId, Long friendId) {
        redisFriendshipRepository.addFriend(memberId, friendId);
    }

    @Recover
    public void recoverAddFriend(Exception e, Long memberId, Long friendId) {
        log.error("Redis 친구 관계 추가 재시도 실패 DLQ 진입: memberId={}, friendId={}", memberId, friendId, e);
        friendEventDlqService.saveFriendAdd(memberId, friendId);
    }

    /**
     * <h3>레디스에 친구 관계 삭제</h3>
     */
    @Async("friendUpdateExecutor")
    @Retryable(
            retryFor = {
                    RedisConnectionFailureException.class,
                    RedisSystemException.class,
                    RedisCommandTimeoutException.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 1.5),
            recover = "recoverDeleteFriend"
    )
    public void deleteFriendToRedis(Long memberId1, Long memberId2) {
        redisFriendshipRepository.deleteFriend(memberId1, memberId2);
    }

    @Recover
    public void recoverDeleteFriend(Exception e, Long memberId1, Long memberId2) {
        log.error("Redis 친구 관계 삭제 재시도 실패 DLQ 진입: memberId={}, friendId={}", memberId1, memberId2, e);
        friendEventDlqService.saveFriendRemove(memberId1, memberId2);
    }
}
