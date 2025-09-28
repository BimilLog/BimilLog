package jaeik.bimillog.domain.notification.application.port.out;

import jaeik.bimillog.domain.notification.entity.SseMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * <h2>SSE 포트</h2>
 * <p>Server-Sent Events 연결 관리와 메시지 전송을 담당하는 포트입니다.</p>
 * <p>SSE 구독, SSE 연결 정리, SSE 메시지 전송, Emitter ID 생성</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface SsePort {

    /**
     * <h3>SSE 구독</h3>
     * <p>클라이언트의 SSE 구독 요청에 대응하여 새로운 SseEmitter 연결을 생성하고 연결 저장소에 등록합니다.</p>
     * <p>30분 타임아웃을 설정하며, 사용자 ID와 토큰 ID 조합으로 고유한 연결 식별자를 생성합니다.</p>
     * <p>NotificationSseService에서 클라이언트의 구독 요청 시 호출됩니다.</p>
     *
     * @param userId  사용자 ID
     * @param tokenId 토큰 ID (다중 기기 구분용)
     * @return SseEmitter (30분 타임아웃 설정된 SSE 연결)
     * @author Jaeik
     * @since 2.0.0
     */
    SseEmitter subscribe(Long userId, Long tokenId);

    /**
     * <h3>SSE 연결 정리</h3>
     * <p>SSE 연결을 연결 저장소에서 제거하고 연결을 해제합니다.</p>
     * <p>tokenId가 null인 경우 사용자의 모든 SSE 연결을 일괄적으로 정리하고,</p>
     * <p>tokenId가 있는 경우 해당 기기의 SSE 연결만 선택적으로 제거합니다.</p>
     * <p>NotificationSseService에서 사용자 탈퇴 또는 로그아웃 시 연결 정리를 위해 호출됩니다.</p>
     *
     * @param userId 사용자 ID
     * @param tokenId 토큰 ID (null인 경우 모든 연결 정리)
     * @since 2.0.0
     * @author Jaeik
     */
    void deleteEmitters(Long userId, Long tokenId);

    /**
     * <h3>SSE 메시지 전송</h3>
     * <p>사용자의 모든 활성 SSE 연결에 실시간 알림 메시지를 브로드캐스트합니다.</p>
     * <p>연결이 끊어지거나 유효하지 않은 Emitter는 자동으로 제거하고, 성공한 연결에만 메시지를 전송합니다.</p>
     * <p>NotificationSseService에서 다양한 알림 이벤트 발송 시 호출됩니다.</p>
     *
     * @param sseMessage SSE 메시지 값 객체 (수신자, 내용, 메타데이터 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    void send(SseMessage sseMessage);

    /**
     * <h3>고유 Emitter ID 생성</h3>
     * <p>사용자 ID, 토큰 ID, 타임스탬프를 조합하여 고유한 SSE 연결 식별자를 생성합니다.</p>
     * <p>동시 접속 또는 연결 재시도 시에도 고유성을 보장하기 위해 밀리초 단위의 타임스탬프를 포함합니다.</p>
     * <p>SSE 연결 저장소에서 연결 관리를 위해 사용되는 내부 유틸리티 메서드입니다.</p>
     *
     * @param userId  사용자 ID
     * @param tokenId 토큰 ID
     * @return 고유한 Emitter ID (userId_tokenId_timestamp 형식)
     * @author Jaeik
     * @since 2.0.0
     */
    String makeTimeIncludeId(Long userId, Long tokenId);

}