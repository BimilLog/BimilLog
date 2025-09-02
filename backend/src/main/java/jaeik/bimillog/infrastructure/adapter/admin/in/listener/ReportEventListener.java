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
public class ReportEventListener {

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
            // 비즈니스 로직 오류는 재시도하지 않음
            log.warn("신고/건의사항 비즈니스 검증 실패 - 신고자: {}, 유형: {}, targetId: {}, 오류: {}", 
                    event.reporterName(), event.reportVO().reportType(), 
                    event.reportVO().targetId(), e.getMessage());
            
            // 향후 알림 서비스나 Dead Letter Queue로 관리자에게 알림 가능
            handleBusinessValidationFailure(event, e);
            
        } catch (Exception e) {
            log.error("신고/건의사항 처리 중 시스템 오류 발생 - 신고자: {}, 유형: {}, targetId: {}, 오류: {}", 
                    event.reporterName(), event.reportVO().reportType(), 
                    event.reportVO().targetId(), e.getMessage(), e);
            
            // 향후 Dead Letter Queue나 재시도 메커니즘 구현 시 확장 가능
            handleSystemError(event, e);
        }
    }
    
    /**
     * <h3>비즈니스 검증 실패 처리</h3>
     * <p>비즈니스 검증에 실패한 신고에 대한 후속 처리를 수행합니다.</p>
     * <p>향후 관리자 알림이나 Dead Letter Queue 구현 시 확장 가능한 포인트입니다.</p>
     *
     * @param event 실패한 신고 이벤트
     * @param exception 발생한 비즈니스 예외
     * @author Jaeik
     * @since 2.0.0
     */
    private void handleBusinessValidationFailure(ReportSubmittedEvent event, CustomException exception) {
        // TODO: 향후 구현 계획
        // 1. 관리자 알림 서비스 연동
        // 2. 실패한 신고에 대한 별도 저장소 저장
        // 3. 모니터링 메트릭 수집
        
        log.debug("비즈니스 검증 실패 후속처리 - 이벤트: {}, 예외: {}", 
                event, exception.getMessage());
    }
    
    /**
     * <h3>시스템 오류 처리</h3>
     * <p>시스템 오류로 인해 신고 처리에 실패한 경우의 후속 처리를 수행합니다.</p>
     * <p>향후 재시도 메커니즘이나 Dead Letter Queue 구현 시 확장 가능한 포인트입니다.</p>
     *
     * @param event 처리 실패한 신고 이벤트
     * @param exception 발생한 시스템 예외
     * @author Jaeik
     * @since 2.0.0
     */
    private void handleSystemError(ReportSubmittedEvent event, Exception exception) {
        // TODO: 향후 구현 계획
        // 1. Dead Letter Queue에 이벤트 저장
        // 2. 재시도 스케줄링
        // 3. 시스템 모니터링 알림
        // 4. 관리자 대시보드에 실패 이벤트 표시
        
        log.debug("시스템 오류 후속처리 - 이벤트: {}, 예외: {}", 
                event, exception.getClass().getSimpleName());
    }
}