package jaeik.growfarm.infrastructure.adapter.auth.in.listener;

import jaeik.growfarm.domain.admin.event.AdminWithdrawRequestedEvent;
import jaeik.growfarm.domain.auth.application.port.in.WithdrawUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * <h2>관리자 강제 탈퇴 이벤트 리스너</h2>
 * <p>관리자가 발행한 강제 탈퇴 요청 이벤트를 처리하는 리스너입니다.</p>
 * <p>이벤트 드리븐 아키텍처를 통해 Admin 도메인과 Auth 도메인의 느슨한 결합을 유지합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminWithdrawEventListener {

    private final WithdrawUseCase withdrawUseCase;

    /**
     * <h3>관리자 강제 탈퇴 요청 처리</h3>
     * <p>AdminWithdrawRequestedEvent를 수신하여 실제 탈퇴 프로세스를 수행합니다.</p>
     * <p>비동기 처리로 이벤트 발행자의 트랜잭션과 분리됩니다.</p>
     *
     * @param event 관리자 강제 탈퇴 요청 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @EventListener
    @Async
    public void handleAdminWithdrawRequest(AdminWithdrawRequestedEvent event) {
        log.info("관리자 강제 탈퇴 요청 처리 시작 - userId: {}, reason: {}", 
                event.userId(), event.reason());
        
        try {
            withdrawUseCase.forceWithdraw(event.userId());
            log.info("관리자 강제 탈퇴 완료 - userId: {}", event.userId());
        } catch (Exception e) {
            log.error("관리자 강제 탈퇴 처리 실패 - userId: {}, error: {}", 
                    event.userId(), e.getMessage(), e);
            // 실패 시 재처리 로직이나 보상 트랜잭션 고려
            throw e;
        }
    }
}