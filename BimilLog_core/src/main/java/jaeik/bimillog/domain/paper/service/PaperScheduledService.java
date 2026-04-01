package jaeik.bimillog.domain.paper.service;

import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.infrastructure.redis.paper.RedisPaperUpdateAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaperScheduledService {
    private final RedisPaperUpdateAdapter redisPaperUpdateAdapter;

    @Scheduled(fixedRate = 60000 * 10) // 10분
    public void applyRealtimeScoreDecay() {
        try {
            redisPaperUpdateAdapter.applyRealtimePopularPaperScoreDecay();
        } catch (Exception e) {
            throw new CustomException(ErrorCode.PAPER_REDIS_WRITE_ERROR, e);
        }
    }
}
