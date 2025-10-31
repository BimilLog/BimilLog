package jaeik.bimillog.domain.paper.application.service;

import jaeik.bimillog.domain.paper.application.port.out.RedisPaperUpdatePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaperScheduledService {

    private final RedisPaperUpdatePort redisPaperUpdatePort;

    @Scheduled(fixedRate = 60000 * 5) // 5분마다
    public void applyRealtimeScoreDecay() {
        try {
            redisPaperUpdatePort.applyRealtimePopularPaperScoreDecay();
            log.info("실시간 인기글 점수 지수감쇠 적용 완료 (0.97 곱하기, 1점 이하 제거)");
        } catch (Exception e) {
            log.error("실시간 인기글 점수 지수감쇠 적용 실패", e);
        }
    }
}
