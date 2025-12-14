package jaeik.bimillog.domain.notification.service;

import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.global.listener.MemberWithdrawListener;
import jaeik.bimillog.domain.global.out.GlobalMemberQueryAdapter;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.notification.controller.NotificationCommandController;
import jaeik.bimillog.domain.notification.entity.Notification;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.notification.entity.NotificationUpdateVO;
import jaeik.bimillog.domain.notification.entity.SseMessage;
import jaeik.bimillog.domain.notification.event.AlarmSendEvent;
import jaeik.bimillog.domain.notification.repository.NotificationRepository;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
public class NotificationCommandService {
    private final NotificationRepository notificationRepository;
    private final GlobalMemberQueryAdapter globalMemberQueryAdapter;
    private final UrlGenerator urlGenerator;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * <h3>알림 일괄 업데이트</h3>
     * <p>여러 알림에 대해 읽음 처리 또는 삭제를 일괄 수행합니다.</p>
     * <p>읽음 처리: isRead 상태를 true로 변경</p>
     * <p>삭제: 알림을 완전 제거</p>
     * <p>{@link NotificationCommandController}에서 사용자의 알림 관리 API 요청 시 호출됩니다.</p>
     *
     * @param memberId      현재 로그인한 사용자 ID
     * @param updateCommand 알림 업데이트 명령 객체
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional
    public void batchUpdate(Long memberId, NotificationUpdateVO updateCommand) {

        List<Long> deleteIds = updateCommand.deletedIds();
        List<Long> readIds = updateCommand.readIds();

        if (deleteIds != null && !deleteIds.isEmpty()) {
            notificationRepository.deleteAllByIdInAndMember_Id(deleteIds, memberId);
        }

        if (readIds != null && !readIds.isEmpty()) {
            List<Notification> notifications = notificationRepository.findAllByIdInAndMember_Id(readIds, memberId);
            notifications.forEach(Notification::markAsRead);
        }
    }

    /**
     * <h3>사용자의 모든 알림 삭제</h3>
     * <p>특정 사용자의 모든 알림을 삭제합니다.</p>
     * <p>주로 사용자 탈퇴 시 호출되어 해당 사용자의 모든 알림 데이터를 정리합니다.</p>
     * <p>{@link MemberWithdrawListener}에서 회원 탈퇴 이벤트 처리 시 호출됩니다.</p>
     *
     * @param memberId 알림을 삭제할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional
    public void deleteAllNotification(Long memberId) {
        notificationRepository.deleteAllByMemberId(memberId);
    }

    /**
     * <h3>댓글 작성 알림 저장</h3>
     * <p>댓글 작성 완료 시 DB에 알림을 저장하고 AlarmSendEvent를 발행합니다.</p>
     *
     * @param postOwnerId 게시글 작성자 ID
     * @param commenterName 댓글 작성자 이름
     * @param postId 게시글 ID
     */
    public void saveCommentNotification(Long postOwnerId, String commenterName, Long postId) {
        String message = commenterName + "님이 댓글을 남겼습니다!";
        String url = urlGenerator.generatePostUrl(postId);
        Member member = globalMemberQueryAdapter.findById(postOwnerId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_INVALID_USER_CONTEXT));

        Notification notification = Notification.create(member, NotificationType.COMMENT, message, url);
        notificationRepository.save(notification);

        eventPublisher.publishEvent(
                AlarmSendEvent.ofComment(member.getId(), message, url, commenterName)
        );
    }

    /**
     * <h3>롤링페이퍼 메시지 알림 저장</h3>
     * <p>롤링페이퍼 메시지 작성 완료 시 DB에 알림을 저장하고 AlarmSendEvent를 발행합니다.</p>
     *
     * @param paperOwnerId 롤링페이퍼 주인 ID
     * @param memberName 작성자 이름
     */
    public void saveMessageNotification(Long paperOwnerId, String memberName) {
        String message = "롤링페이퍼에 메시지가 작성되었어요!";
        String url = urlGenerator.generateRollingPaperUrl(memberName);
        Member member = globalMemberQueryAdapter.findById(paperOwnerId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_INVALID_USER_CONTEXT));

        Notification notification = Notification.create(member, NotificationType.MESSAGE, message, url);
        notificationRepository.save(notification);

        eventPublisher.publishEvent(
                AlarmSendEvent.of(member.getId(), NotificationType.MESSAGE, message, url)
        );
    }

    /**
     * <h3>인기글 선정 알림 저장</h3>
     * <p>게시글 인기글 선정 시 DB에 알림을 저장하고 AlarmSendEvent를 발행합니다.</p>
     *
     * @param memberId 사용자 ID
     * @param message SSE 메시지
     * @param postId 게시글 ID
     * @param notificationType 인기글 유형 (WEEKLY/LEGEND/REALTIME)
     * @param postTitle 게시글 제목 (FCM 알림 본문에 사용)
     */
    public void savePopularNotification(Long memberId, String message, Long postId, NotificationType notificationType, String postTitle) {
        String url = urlGenerator.generatePostUrl(postId);
        Member member = globalMemberQueryAdapter.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_INVALID_USER_CONTEXT));

        Notification notification = Notification.create(member, notificationType, message, url);
        notificationRepository.save(notification);

        eventPublisher.publishEvent(
                AlarmSendEvent.ofPostFeatured(member.getId(), notificationType, message, url, postTitle)
        );
    }

    /**
     * <h3>친구 요청 알림 저장</h3>
     * <p>친구 요청 수신 시 DB에 알림을 저장하고 AlarmSendEvent를 발행합니다.</p>
     *
     * @param receiveMemberId 수신자 ID
     * @param message SSE 메시지
     * @param senderName 친구 요청 보낸 사람 이름 (FCM 알림에 사용)
     */
    public void saveFriendNotification(Long receiveMemberId, String message, String senderName) {
        String url = urlGenerator.generateFriendRequestUrl();
        Member member = globalMemberQueryAdapter.findById(receiveMemberId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_INVALID_USER_CONTEXT));

        Notification notification = Notification.create(member, NotificationType.FRIEND, message, url);
        notificationRepository.save(notification);

        eventPublisher.publishEvent(
                AlarmSendEvent.ofFriend(member.getId(), message, url, senderName)
        );
    }
}
