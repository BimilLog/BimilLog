package jaeik.bimillog.infrastructure.adapter.admin.in.listener;

import jaeik.bimillog.domain.admin.application.port.in.AdminCommandUseCase;
import jaeik.bimillog.domain.user.event.ReportSubmittedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import jaeik.bimillog.infrastructure.exception.CustomException;

/**
 * <h2>신고 이벤트 리스너</h2>
 * <p>사용자 도메인에서 발행한 신고 이벤트를 리스닝하여 관리자 도메인에서 처리하는 Secondary Adapter</p>
 * <p>비동기로 처리되어 사용자의 요청 응답 시간에 영향을 주지 않습니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportSaveListener {

    private final AdminCommandUseCase adminCommandUseCase;

    /**
     * <h3>신고 제출 이벤트 처리</h3>
     * <p>사용자가 제출한 신고/건의사항을 관리자 도메인에서 처리합니다.</p>
     * <p>비동기로 실행되어 사용자 요청의 응답 시간을 단축합니다.</p>
     * <p>비즈니스 검증 실패와 시스템 오류를 구분하여 처리합니다.</p>
     *
     * @param event 신고 제출 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @Async
    @EventListener
    public void handleReportSubmitted(ReportSubmittedEvent event) {
        log.info("신고/건의사항 접수 시작 - 신고자: {}, 유형: {}, targetId: {}", 
                event.reporterName(), event.reportVO().reportType(), event.reportVO().targetId());
        
        try {
            adminCommandUseCase.createReport(event.reporterId(), event.reportVO());
            log.info("신고/건의사항 처리 완료 - 신고자: {}, 유형: {}, targetId: {}", 
                    event.reporterName(), event.reportVO().reportType(), event.reportVO().targetId());
            
        } catch (CustomException e) {
            log.warn("신고/건의사항 비즈니스 검증 실패 - 신고자: {}, 유형: {}, targetId: {}, 오류: {}",
                    event.reporterName(), event.reportVO().reportType(), 
                    event.reportVO().targetId(), e.getMessage());
        } catch (Exception e) {
            log.error("신고/건의사항 처리 중 시스템 오류 발생 - 신고자: {}, 유형: {}, targetId: {}, 오류: {}", 
                    event.reporterName(), event.reportVO().reportType(), 
                    event.reportVO().targetId(), e.getMessage(), e);
        }
    }
}