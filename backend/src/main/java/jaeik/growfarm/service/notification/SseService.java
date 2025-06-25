package jaeik.growfarm.service.notification;

import jaeik.growfarm.dto.notification.EventDTO;
import jaeik.growfarm.entity.notification.NotificationType;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.util.NotificationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * <h2>SSE 알림 서비스</h2>
 * <p>
 * SSE 알림을 처리하는 전용 서비스
 * </p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SseService {

    private final NotificationService notificationService;
    private final NotificationUtil notificationUtil;

    @Value("${url}")
    private String url;

    /**
     * <h3>댓글 달림 SSE 알림</h3>
     *
     * @param postUserId    게시글 작성자 ID
     * @param commenterName 댓글 작성자 이름
     * @param postId        게시글 ID
     * @author Jaeik
     * @since 1.0.0
     */
    @Async("sseNotificationExecutor")
    public void sendCommentNotificationAsync(Long postUserId, String commenterName, Long postId) {
        try {
            EventDTO eventDTO = notificationUtil.createEventDTO(
                    NotificationType.COMMENT,
                    commenterName + "님이 댓글을 남겼습니다!",
                    url + "/board/post/" + postId);
            notificationService.send(postUserId, eventDTO);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.SSE_SEND_ERROR);
        }
    }

    /**
     * <h3>롤링페이퍼에 메시지 수신 SSE 알림</h3>
     *
     * @param farmOwnerId 롤링페이퍼 주인 ID
     * @param userName    닉네임
     * @author Jaeik
     * @since 1.0.0
     */
    @Async("sseNotificationExecutor")
    public void sendPaperPlantNotificationAsync(Long farmOwnerId, String userName) {
        try {
            EventDTO eventDTO = notificationUtil.createEventDTO(
                    NotificationType.FARM,
                    "롤링페이퍼에 메시지가 작성되었어요!",
                    url + "/rolling-paper/" + userName);
            notificationService.send(farmOwnerId, eventDTO);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.SSE_SEND_ERROR);
        }
    }

    /**
     * <h3>인기글 등극 SSE 알림</h3>
     *
     * @param userId  사용자 ID
     * @param message 알림 메시지
     * @param postId  게시글 ID
     */
    @Async("sseNotificationExecutor")
    public void sendPostFeaturedNotificationAsync(Long userId, String message, Long postId) {
        try {
            EventDTO eventDTO = notificationUtil.createEventDTO(
                    NotificationType.POST_FEATURED,
                    message,
                    url + "/board/post/" + postId);
            notificationService.send(userId, eventDTO);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.SSE_SEND_ERROR);

        }
    }
}