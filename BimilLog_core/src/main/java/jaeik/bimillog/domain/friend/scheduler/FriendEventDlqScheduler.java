package jaeik.bimillog.domain.friend.scheduler;

import jaeik.bimillog.domain.friend.entity.jpa.FriendEventDlq;
import jaeik.bimillog.domain.friend.repository.FriendEventDlqRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static jaeik.bimillog.infrastructure.redis.friend.RedisFriendKeys.*;

/**
 * <h2>친구 이벤트 DLQ 재처리 스케줄러</h2>
 * <p>5분마다 DLQ에 저장된 이벤트를 Redis 파이프라인으로 일괄 재처리합니다.</p>
 * <p>Redis 상태를 Actuator 헬스체크로 확인 후 처리합니다.</p>
 *
 * @author Jaeik
 * @version 2.5.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FriendEventDlqScheduler {

    private final FriendEventDlqRepository repository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRY = 3;

    /**
     * 5분마다 DLQ 이벤트를 재처리합니다.
     * <p>Redis가 정상 상태일 때만 실행되며, 파이프라인으로 일괄 처리합니다.</p>
     */
    @Scheduled(fixedRate = 300000)  // 5분마다
    @Transactional
    public void processDlq() {

        List<FriendEventDlq> events = repository.findPendingEvents(BATCH_SIZE);

        if (events.isEmpty()) {
            return;
        }

        if (!isRedisHealthy()) {
            log.warn("[DLQ] Redis 비정상 상태, 재처리 건너뜀");
            return;
        }

        log.info("[DLQ] 재처리 시작: {}건", events.size());

        List<FriendEventDlq> processedEvents = new ArrayList<>();
        List<FriendEventDlq> failedEvents = new ArrayList<>();

        try {
            redisTemplate.executePipelined((RedisConnection connection) -> {
                for (FriendEventDlq event : events) {
                    processEvent(connection, event);
                }
                return null;
            });

            // 파이프라인 성공 시 모두 PROCESSED 처리
            for (FriendEventDlq event : events) {
                event.markAsProcessed();
                processedEvents.add(event);
            }
        } catch (Exception e) {
            log.error("[DLQ] 파이프라인 처리 실패, 개별 재시도 진행", e);

            // 파이프라인 실패 시 개별 처리
            for (FriendEventDlq event : events) {
                try {
                    processSingleEvent(event);
                    event.markAsProcessed();
                    processedEvents.add(event);
                } catch (Exception ex) {
                    event.incrementRetryCount();
                    if (event.getRetryCount() >= MAX_RETRY) {
                        event.markAsFailed();
                        log.error("[DLQ] 최대 재시도 초과, FAILED 처리: id={}, type={}", event.getId(), event.getType());
                    }
                    failedEvents.add(event);
                }
            }
        }

        repository.saveAll(processedEvents);
        repository.saveAll(failedEvents);

        log.info("[DLQ] 재처리 완료: 성공={}건, 실패={}건", processedEvents.size(), failedEvents.size());
    }

    private void processEvent(RedisConnection connection, FriendEventDlq event) {
        switch (event.getType()) {
            case FRIEND_ADD -> processFriendAdd(connection, event);
            case FRIEND_REMOVE -> processFriendRemove(connection, event);
            case SCORE_UP -> processScoreUp(connection, event);
        }
    }

    private void processFriendAdd(RedisConnection connection, FriendEventDlq event) {
        String key1 = FRIEND_SHIP_PREFIX + event.getMemberId();
        String key2 = FRIEND_SHIP_PREFIX + event.getTargetId();

        connection.setCommands().sAdd(
                key1.getBytes(StandardCharsets.UTF_8),
                String.valueOf(event.getTargetId()).getBytes(StandardCharsets.UTF_8));
        connection.setCommands().sAdd(
                key2.getBytes(StandardCharsets.UTF_8),
                String.valueOf(event.getMemberId()).getBytes(StandardCharsets.UTF_8));
    }

    private void processFriendRemove(RedisConnection connection, FriendEventDlq event) {
        String key1 = FRIEND_SHIP_PREFIX + event.getMemberId();
        String key2 = FRIEND_SHIP_PREFIX + event.getTargetId();

        connection.setCommands().sRem(
                key1.getBytes(StandardCharsets.UTF_8),
                String.valueOf(event.getTargetId()).getBytes(StandardCharsets.UTF_8));
        connection.setCommands().sRem(
                key2.getBytes(StandardCharsets.UTF_8),
                String.valueOf(event.getMemberId()).getBytes(StandardCharsets.UTF_8));
    }

    private void processScoreUp(RedisConnection connection, FriendEventDlq event) {
        String key1 = INTERACTION_PREFIX + event.getMemberId();
        String key2 = INTERACTION_PREFIX + event.getTargetId();
        double increment = event.getScore() != null ? event.getScore() : INTERACTION_SCORE_DEFAULT;

        connection.zSetCommands().zIncrBy(
                key1.getBytes(StandardCharsets.UTF_8),
                increment,
                event.getTargetId().toString().getBytes(StandardCharsets.UTF_8));
        connection.zSetCommands().zIncrBy(
                key2.getBytes(StandardCharsets.UTF_8),
                increment,
                event.getMemberId().toString().getBytes(StandardCharsets.UTF_8));
    }

    private void processSingleEvent(FriendEventDlq event) {
        switch (event.getType()) {
            case FRIEND_ADD -> {
                String key1 = FRIEND_SHIP_PREFIX + event.getMemberId();
                String key2 = FRIEND_SHIP_PREFIX + event.getTargetId();
                redisTemplate.opsForSet().add(key1, event.getTargetId());
                redisTemplate.opsForSet().add(key2, event.getMemberId());
            }
            case FRIEND_REMOVE -> {
                String key1 = FRIEND_SHIP_PREFIX + event.getMemberId();
                String key2 = FRIEND_SHIP_PREFIX + event.getTargetId();
                redisTemplate.opsForSet().remove(key1, event.getTargetId());
                redisTemplate.opsForSet().remove(key2, event.getMemberId());
            }
            case SCORE_UP -> {
                String key1 = INTERACTION_PREFIX + event.getMemberId();
                String key2 = INTERACTION_PREFIX + event.getTargetId();
                double score = event.getScore() != null ? event.getScore() : INTERACTION_SCORE_DEFAULT;
                redisTemplate.opsForZSet().incrementScore(key1, event.getTargetId(), score);
                redisTemplate.opsForZSet().incrementScore(key2, event.getMemberId(), score);
            }
        }
    }

    private boolean isRedisHealthy() {
        try {
            RedisConnectionFactory connectionFactory = redisTemplate.getConnectionFactory();
            if (connectionFactory == null) {
                return false;
            }
            String pong = connectionFactory.getConnection().ping();
            return "PONG".equals(pong);
        } catch (Exception e) {
            log.warn("[DLQ] Redis ping 실패", e);
            return false;
        }
    }
}
