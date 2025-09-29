package jaeik.bimillog.domain.notification.application.port.in;

import jaeik.bimillog.domain.auth.event.UserLoggedOutEvent;
import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.paper.event.RollingPaperEvent;
import jaeik.bimillog.domain.post.event.PostFeaturedEvent;
import jaeik.bimillog.domain.user.entity.user.User;
import jaeik.bimillog.domain.user.event.UserWithdrawnEvent;
import jaeik.bimillog.infrastructure.adapter.in.notification.web.NotificationSseController;

/**
 * <h2>FCM 푸시 알림 유스케이스</h2>
 * <p>Firebase Cloud Messaging을 통한 모바일 푸시 알림을 담당하는 유스케이스입니다.</p>
 * <p>FCM 토큰 관리, 댓글 알림, 롤링페이퍼 알림, 인기글 알림</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface FcmUseCase {

    /**
     * <h3>FCM 토큰 등록</h3>
     * <p>클라이언트 앱의 FCM 토큰을 서버에 등록합니다.</p>
     * <p>중복 토큰 검사, 다중 기기 지원</p>
     * <p>{@link NotificationSseController}에서 클라이언트의 토큰 등록 API 요청 시 호출됩니다.</p>
     *
     * @param user   사용자
     * @param fcmToken FCM 토큰 문자열 (Firebase SDK에서 생성)
     * @return 저장된 FCM 토큰 엔티티의 ID (토큰이 없거나 빈 값인 경우 null)
     * @author Jaeik
     * @since 2.0.0
     */
    Long registerFcmToken(User user, String fcmToken);

    /**
     * <h3>FCM 토큰 삭제</h3>
     * <p>로그아웃시 특정 토큰만 삭제하거나 회원탈퇴시 모든 토큰을 삭제합니다.</p>
     * <p>fcmTokenId가 null인 경우 모든 토큰 삭제, 값이 있는 경우 특정 토큰만 삭제합니다.</p>
     * <p>{@link UserLoggedOutEvent}, {@link UserWithdrawnEvent} 이벤트 발생시 호출됩니다.</p>
     *
     * @param userId 사용자 ID
     * @param fcmTokenId 삭제할 토큰 ID (null인 경우 모든 토큰 삭제)
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteFcmTokens(Long userId, Long fcmTokenId);

    /**
     * <h3>댓글 알림 FCM 전송</h3>
     * <p>댓글 작성 완료 시 게시글 작성자에게 FCM 푸시 알림을 전송합니다.</p>
     * <p>댓글 작성자와 게시글 작성자가 동일한 경우 알림을 전송하지 않습니다.</p>
     * <p>{@link CommentCreatedEvent} 이벤트 발생시 댓글 작성 알림 전송 흐름에서 호출됩니다.</p>
     *
     * @param postUserId    게시글 작성자 ID
     * @param commenterName 댓글 작성자 이름
     * @author Jaeik
     * @since 2.0.0
     */
    void sendCommentNotification(Long postUserId, String commenterName);

    /**
     * <h3>롤링페이퍼 메시지 알림 FCM 전송</h3>
     * <p>롤링페이퍼 메시지 작성 완료 시 소유자에게 FCM 푸시 알림을 전송합니다.</p>
     * <p>메시지 작성자와 소유자가 동일한 경우 알림을 전송하지 않습니다.</p>
     * <p>{@link RollingPaperEvent} 이벤트 발생시 롤링페이퍼 메시지 작성 알림 전송 흐름에서 호출됩니다.</p>
     *
     * @param farmOwnerId 롤링페이퍼 주인 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void sendPaperPlantNotification(Long farmOwnerId);

    /**
     * <h3>인기글 등극 알림 FCM 전송</h3>
     * <p>게시글 인기글 선정 시 작성자에게 FCM 푸시 알림을 전송합니다.</p>
     * <p>조회수, 댓글 수, 좋아요 수 등의 기준을 만족한 게시글에 대해 발송됩니다.</p>
     * <p>{@link PostFeaturedEvent} 이벤트 발생시 인기글 등극 알림 전송 흐름에서 호출됩니다.</p>
     *
     * @param userId  사용자 ID
     * @param title   알림 제목
     * @param body    알림 내용
     * @author Jaeik
     * @since 2.0.0
     */
    void sendPostFeaturedNotification(Long userId, String title, String body);

}
