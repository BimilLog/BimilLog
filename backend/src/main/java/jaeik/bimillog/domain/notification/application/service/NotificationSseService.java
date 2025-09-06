package jaeik.bimillog.domain.notification.application.service;

import jaeik.bimillog.domain.notification.application.port.in.NotificationSseUseCase;
import jaeik.bimillog.domain.notification.application.port.out.NotificationUrlPort;
import jaeik.bimillog.domain.notification.application.port.out.SsePort;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * <h2>알림 구독 서비스</h2>
 * <p>SSE 실시간 알림 구독 관련 비즈니스 로직을 처리하는 사용 사례 구현</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class NotificationSseService implements NotificationSseUseCase {

    private final SsePort ssePort;
    private final NotificationUrlPort notificationUrlPort;

    /**
     * <h3>알림 구독</h3>
     * <p>주어진 사용자 ID와 토큰 ID로 SSE 알림을 구독합니다.</p>
     *
     * @param userId 구독할 사용자의 ID
     * @param tokenId 구독 토큰 ID
     * @return SseEmitter 객체
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public SseEmitter subscribe(Long userId, Long tokenId) {
        return ssePort.subscribe(userId, tokenId);
    }

    /**
     * <h3>사용자 SSE 연결 정리</h3>
     * <p>사용자와 관련된 모든 SSE Emitter 연결을 정리합니다.</p>
     *
     * @param userId 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteAllEmitterByUserId(Long userId) {
        ssePort.deleteAllEmitterByUserId(userId);
    }

    /**
     * <h3>특정 기기 SSE 연결 정리</h3>
     * <p>사용자의 특정 기기(토큰)에 해당하는 SSE Emitter 연결을 정리합니다.</p>
     *
     * @param userId 사용자 ID
     * @param tokenId 토큰 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteEmitterByUserIdAndTokenId(Long userId, Long tokenId) {
        ssePort.deleteEmitterByUserIdAndTokenId(userId, tokenId);
    }

    /**
     * <h3>댓글 알림 SSE 전송</h3>
     *
     * @param postUserId    게시글 작성자 ID
     * @param commenterName 댓글 작성자 이름
     * @param postId        게시글 ID
     */
    @Override
    public void sendCommentNotification(Long postUserId, String commenterName, Long postId) {
        String message = commenterName + "님이 댓글을 남겼습니다!";
        String url = notificationUrlPort.generatePostUrl(postId);
        ssePort.send(postUserId, NotificationType.COMMENT, message, url);
    }

    /**
     * <h3>롤링페이퍼 메시지 알림 SSE 전송</h3>
     *
     * @param farmOwnerId 롤링페이퍼 주인 ID
     * @param userName    사용자 이름
     */
    @Override
    public void sendPaperPlantNotification(Long farmOwnerId, String userName) {
        String message = "롤링페이퍼에 메시지가 작성되었어요!";
        String url = notificationUrlPort.generateRollingPaperUrl(userName);
        ssePort.send(farmOwnerId, NotificationType.PAPER, message, url);
    }

    /**
     * <h3>인기글 등극 알림 SSE 전송</h3>
     *
     * @param userId  사용자 ID
     * @param message 알림 메시지
     * @param postId  게시글 ID
     */
    @Override
    public void sendPostFeaturedNotification(Long userId, String message, Long postId) {
        String url = notificationUrlPort.generatePostUrl(postId);
        ssePort.send(userId, NotificationType.POST_FEATURED, message, url);
    }
}