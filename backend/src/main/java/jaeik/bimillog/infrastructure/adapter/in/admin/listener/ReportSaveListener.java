package jaeik.bimillog.infrastructure.adapter.in.admin.listener;

import jaeik.bimillog.domain.admin.application.port.in.AdminCommandUseCase;
import jaeik.bimillog.domain.user.event.ReportSubmittedEvent;
import jaeik.bimillog.infrastructure.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * <h2>신고 저장 리스너</h2>
 * <p>신고 제출 이벤트를 처리하는 리스너입니다.</p>
 * <p>ReportSubmittedEvent 구독하여 신고 데이터 저장</p>
 * <p>비동기 처리로 응답 시간 단축</p>
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
     * <h3>신고 제출 이벤트 비동기 처리</h3>
     * <p>User 도메인에서 발행된 ReportSubmittedEvent를 구독하여 Admin 도메인의 신고 접수 로직을 실행합니다.</p>
     * <p>사용자가 프론트엔드에서 신고 폼을 제출할 때 User 도메인의 서비스에서 이벤트를 발행하면 이 메서드가 호출됩니다.</p>
     * <p>@Async 어노테이션을 통해 별도 스레드에서 비동기적으로 실행되어 사용자의 요청 응답 시간을 단축합니다.</p>
     * <p>AdminCommandUseCase를 호출하여 실제 신고 데이터를 생성하고 저장하는 비즈니스 로직을 수행합니다.</p>
     * <p>비즈니스 검증 실패(CustomException)와 시스템 오류를 구분하여 적절한 로그 레벨로 기록합니다.</p>
     * <p>이벤트 기반 설계를 통해 User 도메인과 Admin 도메인 간의 직접적인 의존성을 제거합니다.</p>
     *
     * @param event 사용자 신고 제출 이벤트 (신고자 정보, 신고 유형, 대상 ID, 신고 내용 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    @Async
    @EventListener
    public void handleReportSubmitted(ReportSubmittedEvent event) {
        log.info("신고/건의사항 접수 시작 - 신고자: {}, 유형: {}, targetId: {}", 
                event.reporterName(), event.reportType(), event.targetId());
        
        try {
            adminCommandUseCase.createReport(event.reporterId(), event.reportType(), event.targetId(), event.content());
            log.info("신고/건의사항 처리 완료 - 신고자: {}, 유형: {}, targetId: {}", 
                    event.reporterName(), event.reportType(), event.targetId());
            
        } catch (CustomException e) {
            log.warn("신고/건의사항 비즈니스 검증 실패 - 신고자: {}, 유형: {}, targetId: {}, 오류: {}",
                    event.reporterName(), event.reportType(), 
                    event.targetId(), e.getMessage());
        } catch (Exception e) {
            log.error("신고/건의사항 처리 중 시스템 오류 발생 - 신고자: {}, 유형: {}, targetId: {}, 오류: {}", 
                    event.reporterName(), event.reportType(), 
                    event.targetId(), e.getMessage(), e);
        }
    }
}