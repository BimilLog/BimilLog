package jaeik.bimillog.domain.notification.application.service;

import jaeik.bimillog.domain.notification.application.port.in.SseUseCase;
import jaeik.bimillog.domain.notification.application.port.out.SsePort;
import jaeik.bimillog.domain.notification.application.port.out.UrlGeneratorPort;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.notification.entity.SseMessage;
import jaeik.bimillog.in.notification.listener.NotificationGenerateListener;
import jaeik.bimillog.in.notification.web.NotificationSseController;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * <h2>SSE 실시간 알림 서비스</h2>
 * <p>Server-Sent Events를 통한 실시간 알림을 담당하는 서비스입니다.</p>
 * <p>SSE 구독 관리, 연결 관리, 이벤트 기반 알림 전송</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class SseService implements SseUseCase {

    private final SsePort ssePort;
    private final UrlGeneratorPort urlGeneratorPort;

    /**
     * <h3>SSE 구독</h3>
     * <p>클라이언트의 SSE 구독 요청을 처리하여 실시간 알림 수신을 위한 SseEmitter를 생성합니다.</p>
     * <p>다중 기기 지원을 위해 사용자 ID와 토큰 ID 조합으로 고유한 연결 식별자를 관리합니다.</p>
     * <p>{@link NotificationSseController}에서 클라이언트의 SSE 구독 API 요청을 처리하기 위해 호출됩니다.</p>
     *
     * @param memberId 구독할 사용자 ID
     * @param tokenId 구독 토큰 ID (다중 기기 구분용)
     * @return SseEmitter 객체 (무제한 타임아웃, 로그아웃 시 명시적으로 종료)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public SseEmitter subscribe(Long memberId, Long tokenId) {
        return ssePort.subscribe(memberId, tokenId);
    }

    /**
     * <h3>SSE 연결 정리</h3>
     * <p>tokenId가 null인 경우 모든 SSE 연결을 정리하고, 값이 있는 경우 특정 기기만 정리합니다.</p>
     * <p>사용자 탈퇴 시에는 tokenId를 null로 전달하여 모든 연결을 정리하고,</p>
     * <p>개별 기기 로그아웃 시에는 tokenId를 전달하여 해당 기기만 연결 해제합니다.</p>
     *
     * @param memberId 사용자 ID
     * @param tokenId 토큰 ID (null인 경우 모든 연결 정리)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteEmitters(Long memberId, Long tokenId) {
        ssePort.deleteEmitters(memberId, tokenId);
    }

    /**
     * <h3>댓글 알림 SSE 전송</h3>
     * <p>댓글 작성 완료 시 게시글 작성자에게 SSE 실시간 알림을 전송합니다.</p>
     * <p>알림 메시지와 이동할 URL을 생성하여 SseMessage를 구성하고, 활성 SSE 연결에 브로드캐스트합니다.</p>
     * <p>{@link NotificationGenerateListener}에서 댓글 작성 이벤트 발생 시 호출됩니다.</p>
     *
     * @param postUserId    게시글 작성자 ID
     * @param commenterName 댓글 작성자 이름
     * @param postId        게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void sendCommentNotification(Long postUserId, String commenterName, Long postId) {
        String message = commenterName + "님이 댓글을 남겼습니다!";
        String url = urlGeneratorPort.generatePostUrl(postId);
        SseMessage sseMessage = SseMessage.of(postUserId, NotificationType.COMMENT, message, url);
        ssePort.send(sseMessage);
    }

    /**
     * <h3>롤링페이퍼 메시지 알림 SSE 전송</h3>
     * <p>롤링페이퍼에 새 메시지 작성 완료 시 롤링페이퍼 소유자에게 SSE 실시간 알림을 전송합니다.</p>
     * <p>알림 메시지와 롤링페이퍼 페이지 URL을 생성하여 SseMessage를 구성하고, 활성 SSE 연결에 브로드캐스트합니다.</p>
     * <p>{@link NotificationGenerateListener}에서 롤링페이퍼 메시지 작성 이벤트 발생 시 호출됩니다.</p>
     *
     * @param farmOwnerId 롤링페이퍼 주인 ID
     * @param memberName    사용자 이름
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void sendPaperPlantNotification(Long farmOwnerId, String memberName) {
        String message = "롤링페이퍼에 메시지가 작성되었어요!";
        String url = urlGeneratorPort.generateRollingPaperUrl(memberName);
        SseMessage sseMessage = SseMessage.of(farmOwnerId, NotificationType.MESSAGE, message, url);
        ssePort.send(sseMessage);
    }

    /**
     * <h3>인기글 등극 알림 SSE 전송</h3>
     * <p>게시글이 인기글로 선정되었을 때 게시글 작성자에게 SSE 실시간 알림을 전송합니다.</p>
     * <p>전달받은 알림 메시지와 게시글 페이지 URL을 사용하여 SseMessage를 구성하고, 활성 SSE 연결에 브로드캐스트합니다.</p>
     * <p>{@link NotificationGenerateListener}에서 인기글 등극 이벤트 발생 시 호출됩니다.</p>
     *
     * @param memberId  사용자 ID
     * @param message 알림 메시지
     * @param postId  게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void sendPostFeaturedNotification(Long memberId, String message, Long postId) {
        String url = urlGeneratorPort.generatePostUrl(postId);
        SseMessage sseMessage = SseMessage.of(memberId, NotificationType.POST_FEATURED, message, url);
        ssePort.send(sseMessage);
    }
}
