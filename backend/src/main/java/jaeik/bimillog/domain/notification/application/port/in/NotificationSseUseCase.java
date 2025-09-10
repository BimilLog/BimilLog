package jaeik.bimillog.domain.notification.application.port.in;

import jaeik.bimillog.domain.auth.event.UserLoggedOutEvent;
import jaeik.bimillog.domain.auth.event.UserWithdrawnEvent;
import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.paper.event.RollingPaperEvent;
import jaeik.bimillog.domain.post.event.PostFeaturedEvent;
import jaeik.bimillog.infrastructure.adapter.notification.in.web.NotificationSseController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * <h2>SSE 실시간 알림 유스케이스</h2>
 * <p>Server-Sent Events를 통한 실시간 알림을 담당하는 유스케이스입니다.</p>
 * <p>SSE 구독 관리, 댓글 알림, 롤링페이퍼 알림, 인기글 알림</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface NotificationSseUseCase {

    /**
     * <h3>SSE 구독</h3>
     * <p>클라이언트의 SSE 연결 요청을 처리하여 SseEmitter를 생성합니다.</p>
     * <p>사용자 ID와 토큰 ID 조합으로 다중 기기 연결을 관리합니다.</p>
     * <p>{@link NotificationSseController}에서 클라이언트의 SSE 구독 API 요청 시 호출됩니다.</p>
     *
     * @param userId  사용자 ID
     * @param tokenId 토큰 ID (다중 기기 구분용)
     * @return SSE Emitter (30분 타임아웃)
     * @author Jaeik
     * @since 2.0.0
     */
    SseEmitter subscribe(Long userId, Long tokenId);

    /**
     * <h3>사용자 SSE 연결 정리</h3>
     * <p>사용자의 모든 SSE Emitter 연결을 정리하고 메모리에서 제거합니다.</p>
     * <p>다중 기기에서 연결된 모든 SSE 연결을 한 번에 해제합니다.</p>
     * <p>{@link UserWithdrawnEvent} 이벤트 발생시 회원 탈퇴 처리 흐름에서 호출됩니다.</p>
     *
     * @param userId 사용자 ID
     * @since 2.0.0
     * @author Jaeik
     */
    void deleteAllEmitterByUserId(Long userId);

    /**
     * <h3>특정 기기 SSE 연결 정리</h3>
     * <p>사용자의 특정 기기(토큰) SSE Emitter 연결만 선택적으로 정리합니다.</p>
     * <p>다중 기기 로그인 환경에서 개별 기기 로그아웃 시 해당 기기만 연결 해제합니다.</p>
     * <p>{@link UserLoggedOutEvent} 이벤트 발생시 특정 기기 로그아웃 처리 흐름에서 호출됩니다.</p>
     *
     * @param userId 사용자 ID
     * @param tokenId 토큰 ID
     * @since 2.0.0
     * @author Jaeik
     */
    void deleteEmitterByUserIdAndTokenId(Long userId, Long tokenId);

    /**
     * <h3>댓글 알림 SSE 전송</h3>
     * <p>댓글 작성 완료 시 게시글 작성자에게 SSE 실시간 알림을 전송합니다.</p>
     * <p>댓글 작성자와 게시글 작성자가 동일한 경우 알림을 전송하지 않습니다.</p>
     * <p>{@link CommentCreatedEvent} 이벤트 발생시 댓글 작성 알림 전송 흐름에서 호출됩니다.</p>
     *
     * @param postUserId    게시글 작성자 ID
     * @param commenterName 댓글 작성자 이름
     * @param postId        게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void sendCommentNotification(Long postUserId, String commenterName, Long postId);

    /**
     * <h3>롤링페이퍼 메시지 알림 SSE 전송</h3>
     * <p>롤링페이퍼 메시지 작성 완료 시 소유자에게 SSE 실시간 알림을 전송합니다.</p>
     * <p>메시지 작성자와 소유자가 동일한 경우 알림을 전송하지 않습니다.</p>
     * <p>{@link RollingPaperEvent} 이벤트 발생시 롤링페이퍼 메시지 작성 알림 전송 흐름에서 호출됩니다.</p>
     *
     * @param farmOwnerId 롤링페이퍼 주인 ID
     * @param userName    사용자 이름
     * @author Jaeik
     * @since 2.0.0
     */
    void sendPaperPlantNotification(Long farmOwnerId, String userName);

    /**
     * <h3>인기글 등극 알림 SSE 전송</h3>
     * <p>게시글 인기글 선정 시 작성자에게 SSE 실시간 알림을 전송합니다.</p>
     * <p>조회수, 댓글 수, 좋아요 수 등의 기준을 만족한 게시글에 대해 발송됩니다.</p>
     * <p>{@link PostFeaturedEvent} 이벤트 발생시 인기글 등극 알림 전송 흐름에서 호출됩니다.</p>
     *
     * @param userId  사용자 ID
     * @param message 알림 메시지
     * @param postId  게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void sendPostFeaturedNotification(Long userId, String message, Long postId);
}