package jaeik.growfarm.service.notification;

import jaeik.growfarm.dto.notification.EventDTO;
import jaeik.growfarm.entity.notification.NotificationType;
import jaeik.growfarm.util.NotificationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * <h2>비동기 SSE 알림 서비스</h2>
 * <p>
 * SSE 알림을 비동기로 처리하는 전용 서비스
 * </p>
 *
 * @author Jaeik
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SseService {

    private final NotificationService notificationService;
    private final NotificationUtil notificationUtil;

    /**
     * <h3>댓글 작성 SSE 알림 (비동기)</h3>
     *
     * @param postUserId    게시글 작성자 ID
     * @param commenterName 댓글 작성자 이름
     * @param postId        게시글 ID
     */
    @Async("sseNotificationExecutor")
    public void sendCommentNotificationAsync(Long postUserId, String commenterName, Long postId) {
        try {
            log.info("댓글 SSE 알림 비동기 처리 시작: postUserId={}, 스레드={}",
                    postUserId, Thread.currentThread().getName());

            EventDTO eventDTO = notificationUtil.createEventDTO(
                    NotificationType.COMMENT,
                    commenterName + "님이 댓글을 남겼습니다!",
                    "http://localhost:3000/board/" + postId);

            notificationService.send(postUserId, eventDTO);

            log.info("댓글 SSE 알림 비동기 처리 완료: postUserId={}", postUserId);

        } catch (Exception e) {
            log.error("댓글 SSE 알림 비동기 처리 실패: postUserId={}, error={}",
                    postUserId, e.getMessage());
        }
    }

    /**
     * <h3>농작물 심기 SSE 알림 (비동기)</h3>
     *
     * @param farmOwnerId 농장 주인 ID
     * @param userName    닉네임
     */
    @Async("sseNotificationExecutor")
    public void sendPaperPlantNotificationAsync(Long farmOwnerId, String userName) {
        try {
            log.info("농작물 심기 SSE 알림 비동기 처리 시작: farmOwnerId={}, 스레드={}",
                    farmOwnerId, Thread.currentThread().getName());

            EventDTO eventDTO = notificationUtil.createEventDTO(
                    NotificationType.FARM,
                    "누군가가 농장에 농작물을 심었습니다!",
                    "http://localhost:3000/farm/" + userName);

            notificationService.send(farmOwnerId, eventDTO);

            log.info("농작물 심기 SSE 알림 비동기 처리 완료: farmOwnerId={}", farmOwnerId);

        } catch (Exception e) {
            log.error("농작물 심기 SSE 알림 비동기 처리 실패: farmOwnerId={}, error={}",
                    farmOwnerId, e.getMessage());
        }
    }

    /**
     * <h3>인기글 등극 SSE 알림 (비동기)</h3>
     *
     * @param userId  사용자 ID
     * @param message 알림 메시지
     * @param postId  게시글 ID
     */
    @Async("sseNotificationExecutor")
    public void sendPostFeaturedNotificationAsync(Long userId, String message, Long postId) {
        try {
            log.info("인기글 SSE 알림 비동기 처리 시작: userId={}, 스레드={}",
                    userId, Thread.currentThread().getName());

            EventDTO eventDTO = notificationUtil.createEventDTO(
                    NotificationType.POST_FEATURED,
                    message,
                    "http://localhost:3000/board/" + postId);

            notificationService.send(userId, eventDTO);

            log.info("인기글 SSE 알림 비동기 처리 완료: userId={}", userId);

        } catch (Exception e) {
            log.error("인기글 SSE 알림 비동기 처리 실패: userId={}, error={}",
                    userId, e.getMessage());
        }
    }
}