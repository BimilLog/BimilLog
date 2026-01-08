package jaeik.bimillog.domain.friend.service;

import jaeik.bimillog.domain.friend.entity.jpa.Friendship;
import jaeik.bimillog.domain.friend.repository.FriendshipRepository;
import jaeik.bimillog.infrastructure.redis.event.RedisFailureEvent;
import jaeik.bimillog.infrastructure.redis.friend.RedisFriendshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <h2>FriendshipRedisRebuildService</h2>
 * <p>Redis 친구 관계 캐시를 DB 기준으로 전체 재구성합니다.</p>
 *
 * @author Jaeik
 * @version 2.4.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FriendshipRedisRebuildService {
    private static final int PAGE_SIZE = 500;
    private static final int REBUILD_COOLDOWN_MINUTES = 5; // 재구성 후 5분간 재시도 방지

    private final FriendshipRepository friendshipRepository;
    private final RedisFriendshipRepository redisFriendshipRepository;

    // 재구성 진행 중 플래그
    private final AtomicBoolean isRebuilding = new AtomicBoolean(false);
    // 마지막 재구성 시간
    private final AtomicReference<LocalDateTime> lastRebuildTime = new AtomicReference<>(null);

    /**
     * <h3>Redis 친구 관계 캐시 재구성</h3>
     * <p>DB의 Friendship 전체를 순회하며 Redis를 재생성합니다.</p>
     */
    @Transactional(readOnly = true)
    public void rebuildRedisFriendshipCache() {
        log.info("Redis 친구 관계 캐시 재구성 시작");
        redisFriendshipRepository.clearAllFriendshipKeys();

        long processed = 0L;
        int page = 0;
        Page<Friendship> friendshipPage;
        do {
            friendshipPage = friendshipRepository.findAll(PageRequest.of(page, PAGE_SIZE));
            for (Friendship friendship : friendshipPage.getContent()) {
                redisFriendshipRepository.addFriend(
                        friendship.getMember().getId(),
                        friendship.getFriend().getId()
                );
                processed++;
            }
            page++;
        } while (friendshipPage.hasNext());

        log.info("Redis 친구 관계 캐시 재구성 완료: processed={}", processed);
    }

    /**
     * <h3>Redis 오류 발생 시 자동 재구성</h3>
     * <p>Redis 조회 실패 이벤트 발생 시 자동으로 캐시를 재구성합니다.</p>
     *
     * <h4>중복 실행 방지</h4>
     * <ul>
     *   <li>재구성이 이미 진행 중이면 스킵</li>
     *   <li>마지막 재구성 후 5분 이내면 스킵</li>
     * </ul>
     *
     * @param event Redis 장애 이벤트
     */
    @Async
    @EventListener
    public void handleRedisFailure(RedisFailureEvent event) {
        log.warn("Redis 오류 감지: source={}, type={}, message={}",
                event.source(), event.errorType(), event.message());

        // 1. 재구성 진행 중이면 스킵
        if (isRebuilding.get()) {
            log.info("Redis 재구성이 이미 진행 중입니다. 스킵합니다.");
            return;
        }

        // 2. 마지막 재구성 후 5분 이내면 스킵 (과도한 재시도 방지)
        LocalDateTime lastTime = lastRebuildTime.get();
        if (lastTime != null && lastTime.plusMinutes(REBUILD_COOLDOWN_MINUTES).isAfter(LocalDateTime.now())) {
            log.info("마지막 재구성 후 {}분 이내입니다. 스킵합니다. (lastRebuild={})",
                    REBUILD_COOLDOWN_MINUTES, lastTime);
            return;
        }

        // 3. 재구성 시작
        if (!isRebuilding.compareAndSet(false, true)) {
            log.info("Redis 재구성이 다른 스레드에서 시작되었습니다. 스킵합니다.");
            return;
        }

        try {
            log.info("Redis 오류 감지로 인한 자동 재구성 시작");
            rebuildRedisFriendshipCache();
            lastRebuildTime.set(LocalDateTime.now());
            log.info("Redis 자동 재구성 성공 (재구성 시간: {})", lastRebuildTime.get());
        } catch (Exception e) {
            log.error("Redis 자동 재구성 실패", e);
            // 재구성 실패해도 애플리케이션은 계속 동작 (DB 조회로 fallback)
        } finally {
            isRebuilding.set(false);
        }
    }
}
