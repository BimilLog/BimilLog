package jaeik.growfarm.infrastructure.adapter.auth.in.listener;

import jaeik.growfarm.domain.auth.application.port.out.DeleteUserPort;
import jaeik.growfarm.domain.auth.event.UserLoggedOutEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * <h2>토큰 정리 이벤트 리스너</h2>
 * <p>사용자 로그아웃 시 토큰 삭제를 담당하는 이벤트 핸들러</p>
 * <p>단일 책임 원칙(SRP)에 따라 토큰 정리 로직만을 담당</p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupEventListener {

    private final DeleteUserPort deleteUserPort;

    /**
     * <h3>사용자 로그아웃 이벤트 처리 - 토큰 정리</h3>
     * <p>사용자가 로그아웃할 때 발생하는 이벤트를 구독하여 특정 토큰만 삭제 작업을 수행</p>
     *
     * @param event 사용자 로그아웃 이벤트
     * @since 2.0.0
     * @author Jaeik
     */
    @EventListener
    @Async
    public void handleUserLoggedOutEvent(UserLoggedOutEvent event) {
        try {
            log.debug("토큰 정리 시작 - 사용자 ID: {}, 토큰 ID: {}", 
                     event.userId(), event.tokenId());
            
            // 다중 로그인 지원: 특정 토큰만 삭제 (다른 기기의 로그인 상태 유지)
            deleteUserPort.logoutUser(event.userId(), event.tokenId());
            
            log.info("토큰 정리 완료 - 사용자 ID: {}, 토큰 ID: {}", 
                     event.userId(), event.tokenId());
            
        } catch (Exception e) {
            log.error("토큰 정리 실패 - 사용자 ID: {}, 토큰 ID: {}, 오류: {}", 
                     event.userId(), event.tokenId(), e.getMessage(), e);
            // 토큰 정리 실패는 로그아웃 자체를 실패시키지 않음 (비동기 처리)
        }
    }
}
