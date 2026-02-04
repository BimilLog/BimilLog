package jaeik.bimillog.domain.post.scheduler;

import jaeik.bimillog.domain.post.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * <h2>처리된 이벤트 정리 스케줄러</h2>
 * <p>매일 새벽 3시에 1일 이상 지난 처리된 이벤트를 삭제합니다.</p>
 *
 * @author Jaeik
 * @version 2.6.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessedEventCleanupScheduler {

    private final ProcessedEventRepository processedEventRepository;

    /**
     * 매일 새벽 3시에 오래된 이벤트를 정리합니다.
     */
    @Scheduled(cron = "0 0 3 * * *")  // 매일 새벽 3시
    @Transactional
    public void cleanupOldEvents() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(1);

        int deletedCount = processedEventRepository.deleteByProcessedAtBefore(cutoffDate);

        if (deletedCount > 0) {
            log.info("[ProcessedEvent] 오래된 이벤트 정리 완료: {}건 삭제 (기준: {} 이전)", deletedCount, cutoffDate);
        }
    }
}
