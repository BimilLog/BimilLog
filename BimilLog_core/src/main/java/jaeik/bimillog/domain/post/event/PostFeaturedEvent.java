package jaeik.bimillog.domain.post.event;

import jaeik.bimillog.in.notification.listener.NotificationGenerateListener;

/**
 * <h2>인기글 등극 이벤트</h2>
 * <p>게시글이 주간 인기글, 전설의 게시글, 공지사항으로 등극했을 때 발생</p>
 * <p>NotificationEventListener에서 수신하여 SSE와 FCM 알림을 비동기로 발송합니다.</p>
 *
 * @param memberId 게시글 작성자 ID (알림을 받을 대상 사용자)
 * @param sseMessage SSE 알림 메시지 (브라우저 실시간 알림용)
 * @param postId 등극한 게시글 ID
 * @param fcmTitle FCM 푸시 알림 제목
 * @param fcmBody FCM 푸시 알림 내용
 * @author Jaeik
 * @version 2.0.0
 * {@link NotificationGenerateListener} 인기글 등극 알림 발송
 */
public record PostFeaturedEvent(
        Long memberId,
        String sseMessage,
        Long postId,
        String fcmTitle,
        String fcmBody
) {}