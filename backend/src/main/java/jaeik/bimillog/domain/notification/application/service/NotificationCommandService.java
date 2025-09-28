package jaeik.bimillog.domain.notification.application.service;

import jaeik.bimillog.domain.notification.application.port.in.NotificationCommandUseCase;
import jaeik.bimillog.domain.notification.application.port.out.NotificationCommandPort;
import jaeik.bimillog.domain.notification.entity.NotificationUpdateVO;
import jaeik.bimillog.domain.notification.exception.NotificationCustomException;
import jaeik.bimillog.infrastructure.adapter.in.global.listener.UserWithdrawListener;
import jaeik.bimillog.infrastructure.adapter.in.notification.web.NotificationCommandController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>알림 명령 서비스</h2>
 * <p>알림 도메인의 명령 작업을 담당하는 서비스입니다.</p>
 * <p>알림 읽음 처리, 알림 삭제</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationCommandService implements NotificationCommandUseCase {

    private final NotificationCommandPort notificationCommandPort;

    /**
     * <h3>알림 일괄 업데이트</h3>
     * <p>여러 알림에 대해 읽음 처리 또는 삭제를 일괄 수행합니다.</p>
     * <p>읽음 처리: isRead 상태를 true로 변경</p>
     * <p>삭제: 알림을 완전 제거</p>
     * <p>{@link NotificationCommandController}에서 사용자의 알림 관리 API 요청 시 호출됩니다.</p>
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param updateCommand 알림 업데이트 명령 객체
     * @throws NotificationCustomException 사용자 정보가 유효하지 않은 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public void batchUpdate(Long userId, NotificationUpdateVO updateCommand) {
        
        notificationCommandPort.batchUpdate(userId, updateCommand);

        log.info("사용자 {}의 알림 업데이트 완료: {} 개 읽음 처리, {} 개 삭제",
                userId, 
                updateCommand.readIds().size(),
                updateCommand.deletedIds().size());
    }

    /**
     * <h3>사용자의 모든 알림 삭제</h3>
     * <p>특정 사용자의 모든 알림을 삭제합니다.</p>
     * <p>주로 사용자 탈퇴 시 호출되어 해당 사용자의 모든 알림 데이터를 정리합니다.</p>
     * <p>{@link UserWithdrawListener}에서 회원 탈퇴 이벤트 처리 시 호출됩니다.</p>
     *
     * @param userId 알림을 삭제할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public void deleteAllNotification(Long userId) {
        notificationCommandPort.deleteAllByUserId(userId);
    }

}
