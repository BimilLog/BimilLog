package jaeik.bimillog.domain.post.event;

import jaeik.bimillog.domain.notification.entity.NotificationType;

/**
 * <h2>인기글 등극 이벤트</h2>
 * <p>게시글이 주간 인기글, 전설의 게시글, 실시간 인기글로 등극했을 때 발생</p>
 * <p>NotificationSaveListener에서 수신하여 알림을 저장하고 SSE와 FCM 알림을 비동기로 발송합니다.</p>
 * <p>FCM 푸시 알림의 title과 body는 FcmCommandService에서 NotificationType에 따라 작성됩니다.</p>
 *
 * @param memberId 게시글 작성자 ID (알림을 받을 대상 사용자)
 * @param sseMessage SSE 알림 메시지 (브라우저 실시간 알림용)
 * @param postId 등극한 게시글 ID
 * @param notificationType 인기글 유형 (WEEKLY/LEGEND/REALTIME)
 * @param postTitle 게시글 제목 (FCM 알림 본문에 사용)
 * @author Jaeik
 * @version 2.0.0
 */
public record PostFeaturedEvent(
        Long memberId,
        String sseMessage,
        Long postId,
        NotificationType notificationType,
        String postTitle
) {}