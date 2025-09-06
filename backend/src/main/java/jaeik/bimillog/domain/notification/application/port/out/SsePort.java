package jaeik.bimillog.domain.notification.application.port.out;

import jaeik.bimillog.domain.notification.entity.SseMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * <h2>SSE 포트</h2>
 * <p>SSE 연결 관리 관련 인프라 액세스를 정의하는 아웃바운드 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface SsePort {

    /**
     * <h3>SSE 구독</h3>
     * <p>특정 사용자와 토큰에 대한 SSE 연결을 생성합니다.</p>
     *
     * @param userId  사용자 ID
     * @param tokenId 토큰 ID
     * @return SSE Emitter
     * @author Jaeik
     * @since 2.0.0
     */
    SseEmitter subscribe(Long userId, Long tokenId);

    /**
     * <h3>사용자 SSE 연결 정리</h3>
     * <p>사용자와 관련된 모든 SSE Emitter 연결을 정리합니다.</p>
     *
     * @param userId 사용자 ID
     * @since 2.0.0
     * @author Jaeik
     */
    void deleteAllEmitterByUserId(Long userId);

    /**
     * <h3>특정 기기 SSE 연결 정리</h3>
     * <p>사용자의 특정 기기(토큰)에 해당하는 SSE Emitter 연결을 정리합니다.</p>
     * <p>다중 기기 로그인 환경에서 특정 기기만 로그아웃 처리할 때 사용합니다.</p>
     *
     * @param userId 사용자 ID
     * @param tokenId 토큰 ID
     * @since 2.0.0
     * @author Jaeik
     */
    void deleteEmitterByUserIdAndTokenId(Long userId, Long tokenId);

    /**
     * <h3>알림 전송</h3>
     * <p>
     * 사용자에게 SSE 알림을 전송합니다.
     * </p>
     *
     * @param sseMessage SSE 메시지 값 객체
     * @author Jaeik
     * @since 2.0.0
     */
    void send(SseMessage sseMessage);

    /**
     * <h3>고유 Emitter ID 생성</h3>
     * <p>타임스탬프를 포함한 고유한 Emitter ID를 생성합니다.</p>
     *
     * @param userId  사용자 ID
     * @param tokenId 토큰 ID
     * @return Emitter ID
     * @author Jaeik
     * @since 2.0.0
     */
    String makeTimeIncludeId(Long userId, Long tokenId);

}