package jaeik.growfarm.service.notification;

import jaeik.growfarm.dto.notification.EventDTO;
import jaeik.growfarm.dto.notification.NotificationDTO;
import jaeik.growfarm.dto.notification.UpdateNotificationDTO;
import jaeik.growfarm.entity.notification.NotificationType;
import jaeik.growfarm.global.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * <h2>알림 퍼사드 서비스</h2>
 * <p>
 * 모든 알림 관련 기능을 조율하는 퍼사드 서비스
 * SRP: 알림 서비스들 간의 조율과 통합 인터페이스 제공
 * OCP: 새로운 알림 방식 추가 시 기존 코드 변경 없이 확장 가능
 * DIP: 구체적인 구현이 아닌 인터페이스에 의존
 * </p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationFacadeService {

    private final List<NotificationSender> notificationSenders;
    private final NotificationService notificationService;
    private final NotificationUtil notificationUtil;

    @Value("${url}")
    private String url;

    /**
     * <h3>SSE 구독</h3>
     *
     * @param userId  사용자 ID
     * @param tokenId 토큰 ID
     * @return SSE Emitter
     * @author Jaeik
     * @since 1.0.0
     */
    public SseEmitter subscribe(Long userId, Long tokenId) {
        return notificationService.subscribe(userId, tokenId);
    }

    /**
     * <h3>알림 리스트 조회</h3>
     *
     * @param userDetails 사용자 정보
     * @return 알림 목록
     * @author Jaeik
     * @since 1.0.0
     */
    public List<NotificationDTO> getNotificationList(CustomUserDetails userDetails) {
        return notificationService.getNotificationList(userDetails);
    }

    /**
     * <h3>알림 일괄 업데이트</h3>
     *
     * @param userDetails           사용자 정보
     * @param updateNotificationDTO 업데이트 정보
     * @author Jaeik
     * @since 1.0.0
     */
    public void batchUpdate(CustomUserDetails userDetails, UpdateNotificationDTO updateNotificationDTO) {
        notificationService.batchUpdate(userDetails, updateNotificationDTO);
    }

    /**
     * <h3>댓글 알림 전송</h3>
     *
     * @param postUserId    게시글 작성자 ID
     * @param commenterName 댓글 작성자 이름
     * @param postId        게시글 ID
     * @author Jaeik
     * @since 1.0.0
     */
    public void sendCommentNotification(Long postUserId, String commenterName, Long postId) {
        EventDTO eventDTO = notificationUtil.createEventDTO(
                NotificationType.COMMENT,
                commenterName + "님이 댓글을 남겼습니다!",
                url + "/board/post/" + postId
        );

        sendNotificationToAll(postUserId, eventDTO);
    }

    /**
     * <h3>롤링페이퍼 메시지 알림 전송</h3>
     *
     * @param farmOwnerId 롤링페이퍼 주인 ID
     * @param userName    사용자 이름
     * @author Jaeik
     * @since 1.0.0
     */
    public void sendPaperPlantNotification(Long farmOwnerId, String userName) {
        EventDTO eventDTO = notificationUtil.createEventDTO(
                NotificationType.FARM,
                "롤링페이퍼에 메시지가 작성되었어요!",
                url + "/rolling-paper/" + userName
        );

        sendNotificationToAll(farmOwnerId, eventDTO);
    }

    /**
     * <h3>인기글 등극 알림 전송</h3>
     *
     * @param userId  사용자 ID
     * @param message 알림 메시지
     * @param postId  게시글 ID
     * @author Jaeik
     * @since 1.0.0
     */
    public void sendPostFeaturedNotification(Long userId, String message, Long postId) {
        EventDTO eventDTO = notificationUtil.createEventDTO(
                NotificationType.POST_FEATURED,
                message,
                url + "/board/post/" + postId
        );

        sendNotificationToAll(userId, eventDTO);
    }

    /**
     * <h3>모든 알림 방식으로 전송</h3>
     * <p>
     * 등록된 모든 NotificationSender를 통해 알림을 전송합니다.
     * Strategy Pattern 적용으로 확장성 확보
     * </p>
     *
     * @param userId   사용자 ID
     * @param eventDTO 이벤트 정보
     * @author Jaeik
     * @since 1.0.0
     */
    private void sendNotificationToAll(Long userId, EventDTO eventDTO) {
        for (NotificationSender sender : notificationSenders) {
            try {
                sender.send(userId, eventDTO);
                log.debug("알림 전송 성공 - Type: {}, UserId: {}", sender.getType(), userId);
            } catch (Exception e) {
                log.error("알림 전송 실패 - Type: {}, UserId: {}, Error: {}", 
                         sender.getType(), userId, e.getMessage(), e);
            }
        }
    }
}