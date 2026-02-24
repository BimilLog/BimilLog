package jaeik.bimillog.domain.friend.scheduler;

import jaeik.bimillog.domain.friend.entity.jpa.FriendDlqStatus;
import jaeik.bimillog.domain.friend.entity.jpa.FriendEventDlq;
import jaeik.bimillog.domain.friend.repository.FriendEventDlqRepository;
import jaeik.bimillog.infrastructure.redis.RedisCheck;
import jaeik.bimillog.infrastructure.redis.friend.RedisFriendshipRepository;
import jaeik.bimillog.infrastructure.redis.friend.RedisInteractionScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * <h2>친구 이벤트 DLQ 재처리 스케줄러</h2>
 * <p>10분마다 DLQ에 저장된 이벤트를 Redis 파이프라인으로 일괄 재처리합니다.</p>
 * <p>친구 추가, 삭제의 유실 친구 상호작용 점수의 유실이 모임</p>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FriendEventDlqScheduler {
    private final FriendEventDlqRepository repository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisCheck redisCheck;
    private final RedisFriendshipRepository redisFriendshipRepository;
    private final RedisInteractionScoreRepository redisInteractionScoreRepository;

    private static final int BATCH_SIZE = 500;
    private static final int MAX_RETRY = 3;

    /**
     * 10분마다 DLQ 이벤트를 재처리합니다.
     * <p>Redis가 정상 상태일 때만 실행되며, PENDING 이벤트가 모두 소진될 때까지 배치 반복 처리합니다.</p>
     */
    @Scheduled(fixedRate = 600000)  // 10분마다
    public void processDlq() {
        if (!redisCheck.isRedisHealthy()) return;

        int totalProcessed = 0;
        int totalFailed = 0;

        while (true) {
            List<FriendEventDlq> events = repository.findPendingEvents(FriendDlqStatus.PENDING, MAX_RETRY, BATCH_SIZE);
            if (events.isEmpty()) break;

            List<FriendEventDlq> processedEvents = new ArrayList<>();
            List<FriendEventDlq> failedEvents = new ArrayList<>();

            try {
                pipelineRestore(events, processedEvents);
            } catch (Exception e) {
                log.error("[친구 DLQ] 파이프라인 처리 실패, 개별 재시도 진행", e);
                singleRestore(events, processedEvents, failedEvents);
            }

            repository.saveAll(processedEvents);
            repository.saveAll(failedEvents);
            totalProcessed += processedEvents.size();
            totalFailed += failedEvents.size();
        }

        if (totalProcessed > 0 || totalFailed > 0) {
            log.info("[친구 DLQ] 전체 재처리 완료: 성공={}건, 실패={}건", totalProcessed, totalFailed);
        }
    }

    private void pipelineRestore(List<FriendEventDlq> events, List<FriendEventDlq> processedEvents) {
        redisTemplate.executePipelined((RedisConnection connection) -> {
            for (FriendEventDlq event : events) {
                switch (event.getType()) {
                    case FRIEND_ADD -> redisFriendshipRepository.processFriendAdd(connection, event);
                    case FRIEND_REMOVE -> redisFriendshipRepository.processFriendRemove(connection, event);
                    case SCORE_UP -> redisInteractionScoreRepository.processScoreUp(connection, event);
                }
            }
            return null;
        });

        // 파이프라인 성공 시 모두 PROCESSED 처리
        for (FriendEventDlq event : events) {
            event.markAsProcessed();
            processedEvents.add(event);
        }
    }

    private void singleRestore(List<FriendEventDlq> events, List<FriendEventDlq> processedEvents, List<FriendEventDlq> failedEvents) {
        // 파이프라인 실패 시 개별 처리
        for (FriendEventDlq event : events) {
            try {
                switch (event.getType()) {
                    case FRIEND_ADD -> redisFriendshipRepository.addFriend(event.getMemberId(), event.getTargetId());
                    case FRIEND_REMOVE -> redisFriendshipRepository.deleteFriend(event.getMemberId(), event.getTargetId());
                    case SCORE_UP -> redisInteractionScoreRepository.addInteractionScore(event.getMemberId(), event.getTargetId(), event.getEventId());
                }
                event.markAsProcessed();
                processedEvents.add(event);
            } catch (Exception ex) {
                event.incrementRetryCount();
                if (event.getRetryCount() >= MAX_RETRY) {
                    event.markAsFailed();
                    log.error("[친구 DLQ] 최대 재시도 초과, FAILED 처리: id={}, type={}", event.getId(), event.getType());
                }
                failedEvents.add(event);
            }
        }
    }
}
