package jaeik.bimillog.domain.notification.application.port.in;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * <h2>SSE 실시간 알림 요구사항</h2>
 * <p>
 * 헥사고날 아키텍처에서 Server-Sent Events(SSE)를 통한 실시간 알림을 정의하는 Primary Port입니다.
 * 브라우저에서 실시간으로 알림을 수신할 수 있도록 SSE 연결 관리와 알림 전송에 대한 비즈니스 인터페이스를 제공합니다.
 * </p>
 * <p>
 * FCM 푸시 알림과 달리 웹 브라우저에서 실시간으로 알림을 수신할 수 있으며, 연결이 활성화된 상태에서만 동작합니다.
 * 댓글 작성, 롤링페이퍼 메시지 작성, 인기글 등극 등의 이벤트 발생 시 즉시 실시간 알림을 전송합니다.
 * </p>
 * <p>NotificationSseController와 각종 이벤트 리스너에서 호출합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface NotificationSseUseCase {

    /**
     * <h3>SSE 구독</h3>
     * <p>클라이언트의 SSE 연결 요청을 처리하여 실시간 알림 수신을 위한 SseEmitter를 생성합니다.</p>
     * <p>다중 기기 지원을 위해 사용자 ID와 토큰 ID 조합으로 각각의 연결을 관리합니다.</p>
     * <p>연결 유지 시간은 30분이며, 타임아웃 시 자동으로 연결이 해제됩니다.</p>
     * <p>NotificationSseController에서 클라이언트의 SSE 구독 API 요청을 처리하기 위해 호출합니다.</p>
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
     * <p>사용자와 관련된 모든 SSE Emitter 연결을 정리하고 메모리에서 제거합니다.</p>
     * <p>다중 기기에서 연결된 모든 SSE 연결을 한 번에 해제하며, 개인정보 보호를 위해 완전히 삭제합니다.</p>
     * <p>NotificationRemoveListener에서 회원탈퇴 이벤트 발생 시 호출합니다.</p>
     *
     * @param userId 사용자 ID
     * @since 2.0.0
     * @author Jaeik
     */
    void deleteAllEmitterByUserId(Long userId);

    /**
     * <h3>특정 기기 SSE 연결 정리</h3>
     * <p>사용자의 특정 기기(토큰)에 해당하는 SSE Emitter 연결만을 선택적으로 정리합니다.</p>
     * <p>다중 기기 로그인 환경에서 개별 기기의 로그아웃 시 다른 기기의 연결은 유지하면서 해당 기기만 연결 해제합니다.</p>
     * <p>AuthLogoutListener에서 특정 기기 로그아웃 이벤트 발생 시 호출합니다.</p>
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
     * <p>게시글 작성자의 모든 활성화된 SSE 연결에 알림 데이터를 브로드캐스트합니다.</p>
     * <p>CommentNotificationListener에서 댓글 작성 이벤트 발생 시 호출합니다.</p>
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
     * <p>롤링페이퍼에 새 메시지 작성 완료 시 롤링페이퍼 소유자에게 SSE 실시간 알림을 전송합니다.</p>
     * <p>메시지 작성자와 롤링페이퍼 소유자가 동일한 경우 알림을 전송하지 않습니다.</p>
     * <p>롤링페이퍼 소유자의 모든 활성화된 SSE 연결에 알림 데이터를 브로드캐스트합니다.</p>
     * <p>PaperNotificationListener에서 롤링페이퍼 메시지 작성 이벤트 발생 시 호출합니다.</p>
     *
     * @param farmOwnerId 롤링페이퍼 주인 ID
     * @param userName    사용자 이름
     * @author Jaeik
     * @since 2.0.0
     */
    void sendPaperPlantNotification(Long farmOwnerId, String userName);

    /**
     * <h3>인기글 등극 알림 SSE 전송</h3>
     * <p>게시글이 인기글로 선정되었을 때 게시글 작성자에게 SSE 실시간 알림을 전송합니다.</p>
     * <p>조회수, 댓글 수, 좋아요 수 등의 기준을 만족한 게시글에 대해 자동으로 발송됩니다.</p>
     * <p>게시글 작성자의 모든 활성화된 SSE 연결에 알림 데이터를 브로드캐스트합니다.</p>
     * <p>PostFeaturedListener에서 인기글 등극 이벤트 발생 시 호출합니다.</p>
     *
     * @param userId  사용자 ID
     * @param message 알림 메시지
     * @param postId  게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void sendPostFeaturedNotification(Long userId, String message, Long postId);
}