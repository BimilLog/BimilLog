package jaeik.growfarm.domain.notification.application.port.in;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * <h2>알림 구독 유스케이스</h2>
 * <p>SSE 실시간 알림 구독 관련 비즈니스 로직을 정의하는 Primary Port</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface NotificationSseUseCase {

    /**
     * <h3>SSE 구독</h3>
     * <p>사용자의 실시간 알림 구독을 처리합니다.</p>
     *
     * @param userId  사용자 ID
     * @param tokenId 토큰 ID
     * @return SSE Emitter
     */
    SseEmitter subscribe(Long userId, Long tokenId);

    /**
     * <h3>사용자 SSE 연결 정리</h3>
     * <p>사용자와 관련된 모든 SSE Emitter 연결을 정리합니다.</p>
     * <p>회원탈퇴 시 호출됩니다.</p>
     *
     * @param userId 사용자 ID
     * @since 2.0.0
     * @author Jaeik
     */
    void deleteAllEmitterByUserId(Long userId);

    /**
     * <h3>특정 기기 SSE 연결 정리</h3>
     * <p>사용자의 특정 기기(토큰)에 해당하는 SSE Emitter 연결을 정리합니다.</p>
     * <p>다중 기기 로그인 환경에서 특정 기기만 로그아웃 처리할 때 호출됩니다.</p>
     *
     * @param userId 사용자 ID
     * @param tokenId 토큰 ID
     * @since 2.0.0
     * @author Jaeik
     */
    void deleteEmitterByUserIdAndTokenId(Long userId, Long tokenId);

    /**
     * <h3>댓글 알림 SSE 전송</h3>
     * <p>댓글이 작성되었을 때 게시글 작성자에게 SSE 알림을 전송합니다.</p>
     *
     * @param postUserId    게시글 작성자 ID
     * @param commenterName 댓글 작성자 이름
     * @param postId        게시글 ID
     */
    void sendCommentNotification(Long postUserId, String commenterName, Long postId);

    /**
     * <h3>롤링페이퍼 메시지 알림 SSE 전송</h3>
     * <p>롤링페이퍼에 새 메시지가 작성되었을 때 주인에게 SSE 알림을 전송합니다.</p>
     *
     * @param farmOwnerId 롤링페이퍼 주인 ID
     * @param userName    사용자 이름
     */
    void sendPaperPlantNotification(Long farmOwnerId, String userName);

    /**
     * <h3>인기글 등극 알림 SSE 전송</h3>
     * <p>게시글이 인기글로 등극했을 때 작성자에게 SSE 알림을 전송합니다.</p>
     *
     * @param userId  사용자 ID
     * @param message 알림 메시지
     * @param postId  게시글 ID
     */
    void sendPostFeaturedNotification(Long userId, String message, Long postId);
}