package jaeik.bimillog.infrastructure.adapter.in.paper.listener;

import jaeik.bimillog.domain.paper.application.port.out.RedisPaperUpdatePort;
import jaeik.bimillog.domain.paper.event.MessageDeletedEvent;
import jaeik.bimillog.domain.paper.event.PaperViewedEvent;
import jaeik.bimillog.domain.paper.event.RollingPaperEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * <h2>실시간 인기 롤링페이퍼 점수 업데이트 리스너</h2>
 * <p>롤링페이퍼 조회, 메시지 작성, 메시지 삭제 이벤트를 수신하여 실시간 인기 롤링페이퍼 점수를 업데이트합니다.</p>
 * <p>조회: +2점, 메시지 작성: +5점, 메시지 삭제: -5점</p>
 * <p>비동기 처리를 통해 이벤트 발행자와 독립적으로 실행됩니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RealtimePaperPopularScoreListener {

    private final RedisPaperUpdatePort redisPaperUpdatePort;

    private static final double VIEW_SCORE = 2.0;
    private static final double MESSAGE_SCORE = 5.0;

    /**
     * <h3>롤링페이퍼 조회 이벤트 처리</h3>
     * <p>롤링페이퍼 조회 시 실시간 인기 롤링페이퍼 점수를 2점 증가시킵니다.</p>
     * <p>PaperViewedEvent 발행 시 비동기로 호출됩니다.</p>
     *
     * @param event 롤링페이퍼 조회 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @EventListener
    @Async
    public void handlePaperViewed(PaperViewedEvent event) {
        try {
            redisPaperUpdatePort.incrementRealtimePopularPaperScore(event.memberId(), VIEW_SCORE);
            log.debug("실시간 인기 롤링페이퍼 점수 증가 (조회): memberId={}, score=+{}", event.memberId(), VIEW_SCORE);
        } catch (Exception e) {
            log.error("실시간 인기 롤링페이퍼 점수 증가 실패 (조회): memberId={}", event.memberId(), e);
        }
    }

    /**
     * <h3>메시지 작성 이벤트 처리</h3>
     * <p>메시지 작성 시 해당 롤링페이퍼의 실시간 인기 점수를 5점 증가시킵니다.</p>
     * <p>RollingPaperEvent 발행 시 비동기로 호출됩니다.</p>
     *
     * @param event 메시지 작성 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @EventListener
    @Async
    public void handleMessageCreated(RollingPaperEvent event) {
        try {
            redisPaperUpdatePort.incrementRealtimePopularPaperScore(event.paperOwnerId(), MESSAGE_SCORE);
            log.debug("실시간 인기 롤링페이퍼 점수 증가 (메시지 작성): memberId={}, score=+{}", event.paperOwnerId(), MESSAGE_SCORE);
        } catch (Exception e) {
            log.error("실시간 인기 롤링페이퍼 점수 증가 실패 (메시지 작성): memberId={}", event.paperOwnerId(), e);
        }
    }

    /**
     * <h3>메시지 삭제 이벤트 처리</h3>
     * <p>메시지 삭제 시 해당 롤링페이퍼의 실시간 인기 점수를 5점 감소시킵니다.</p>
     * <p>MessageDeletedEvent 발행 시 비동기로 호출됩니다.</p>
     *
     * @param event 메시지 삭제 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @EventListener
    @Async
    public void handleMessageDeleted(MessageDeletedEvent event) {
        try {
            redisPaperUpdatePort.incrementRealtimePopularPaperScore(event.paperOwnerId(), -MESSAGE_SCORE);
            log.debug("실시간 인기 롤링페이퍼 점수 감소 (메시지 삭제): memberId={}, score=-{}", event.paperOwnerId(), MESSAGE_SCORE);
        } catch (Exception e) {
            log.error("실시간 인기 롤링페이퍼 점수 감소 실패 (메시지 삭제): memberId={}", event.paperOwnerId(), e);
        }
    }
}
