package jaeik.bimillog.domain.notification.service;

import jaeik.bimillog.domain.global.listener.MemberWithdrawListener;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.notification.entity.Notification;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.notification.repository.NotificationRepository;
import jaeik.bimillog.domain.notification.adapter.NotificationToMemberAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <h2>알림 명령 서비스</h2>
 * <p>알림 도메인의 명령 작업을 담당하는 서비스입니다.</p>
 * <p>알림 읽음 처리, 알림 삭제, 알림 저장(템플릿 콜백)</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationCommandService {
    private final NotificationRepository notificationRepository;
    private final NotificationToMemberAdapter notificationToMemberAdapter;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * <h3>알림 일괄 업데이트</h3>
     * <p>여러 알림에 대해 읽음 처리 또는 삭제를 일괄 수행합니다.</p>
     * <p>읽음 처리: isRead 상태를 true로 변경</p>
     * <p>삭제: 알림을 완전 제거</p>
     *
     * @param memberId 현재 로그인한 사용자 ID
     */
    @Transactional
    public void batchUpdate(Long memberId, List<Long> readIds, List<Long> deletedIds) {

        if (!deletedIds.isEmpty()) {
            notificationRepository.deleteAllByIdInAndMember_Id(deletedIds, memberId);
        }

        if (!readIds.isEmpty()) {
            List<Notification> notifications = notificationRepository.findAllByIdInAndMember_Id(readIds, memberId);
            notifications.forEach(Notification::markAsRead);
        }
    }

    /**
     * <h3>사용자의 모든 알림 삭제</h3>
     * <p>{@link MemberWithdrawListener}에서 회원 탈퇴 이벤트 처리 시 호출됩니다.</p>
     *
     * @param memberId 알림을 삭제할 사용자 ID
     */
    @Transactional
    public void deleteAllNotification(Long memberId) {
        notificationRepository.deleteAllByMemberId(memberId);
    }

    /**
     * <h3>알림 저장 (템플릿 콜백)</h3>
     * <p>공통 흐름: 회원 조회 → 알림 저장 → AlarmSendEvent 발행</p>
     * <p>가변부(타입별 이벤트 생성)는 {@link NotificationEventCallback}으로 위임합니다.</p>
     *
     * @param memberId 알림 수신자 ID
     * @param type     알림 타입
     * @param message  SSE 메시지
     * @param url      이동 URL
     * @param callback 타입별 AlarmSendEvent 생성 콜백
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveNotification(Long memberId, NotificationType type, String message, String url, NotificationEventCallback callback) {
        Member member = notificationToMemberAdapter.findById(memberId);

        Notification notification = Notification.create(member, type, message, url);
        notificationRepository.save(notification);

        eventPublisher.publishEvent(callback.createEvent(member));
    }
}
