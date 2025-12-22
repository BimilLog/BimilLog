package jaeik.bimillog.domain.comment.event;

import jaeik.bimillog.domain.notification.listener.NotificationSendListener;

/**
 * <h2>댓글 작성 이벤트</h2>
 * <p>댓글이 작성되었을 때 발생하는 이벤트</p>
 * <p>SSE와 FCM 알림을 트리거하고 상호작용 점수를 증가시킵니다</p>
 * <p>이때 게시글 작성자는 익명이면 안됨</p>
 *
 * @param postUserId 게시글 작성자 ID (알림을 받을 사용자)
 * @param commenterName 댓글 작성자 이름
 * @param commenterId 댓글 작성자 ID (익명인 경우 null, 상호작용 점수 증가에 사용)
 * @param postId 게시글 ID
 * @author Jaeik
 * @version 2.0.0
 * {@link NotificationSendListener} SSE/FCM 알림 발송
 */
public record CommentCreatedEvent(Long postUserId, String commenterName, Long commenterId, Long postId) {}