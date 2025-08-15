package jaeik.growfarm.domain.notification.application.service;

import jaeik.growfarm.domain.notification.application.port.in.NotificationEventUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>알림 이벤트 서비스</h2>
 * <p>알림 관련 이벤트를 처리하는 서비스 클래스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class NotificationEventService implements NotificationEventUseCase {

    @Override
    public void sendCommentNotification(Long postUserId, String commenterName, Long postId) {
        // TODO: 댓글 알림 전송 구현
        log.info("댓글 알림 전송: postUserId={}, commenterName={}, postId={}", postUserId, commenterName, postId);
    }

    @Override
    public void sendPaperPlantNotification(Long farmOwnerId, String userName) {
        // TODO: 롤링페이퍼 메시지 알림 전송 구현
        log.info("롤링페이퍼 메시지 알림 전송: farmOwnerId={}, userName={}", farmOwnerId, userName);
    }

    @Override
    public void sendPostFeaturedNotification(Long userId, String message, Long postId) {
        // TODO: 인기글 등극 알림 전송 구현
        log.info("인기글 등극 알림 전송: userId={}, message={}, postId={}", userId, message, postId);
    }


}