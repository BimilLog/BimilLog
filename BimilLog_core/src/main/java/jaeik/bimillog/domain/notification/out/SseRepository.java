package jaeik.bimillog.domain.notification.out;

import jaeik.bimillog.domain.notification.controller.NotificationSseController;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.notification.entity.SseMessage;
import jaeik.bimillog.domain.notification.listener.NotificationSendListener;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * <h2>SSE 저장소</h2>
 * <p>SSE의 연결 관리와 알림 전송을 담당합니다.</p>
 * <p>다중 기기 지원을 위해 사용자별, 토큰별로 복수의 SSE 연결을 관리합니다.</p>
 * <p>SSE 연결 구독 및 초기화 메시지 전송</p>
 * <p>사용자별 다중 Emitter 관리 (멀티 디바이스 지원)</p>
 * <p>알림 메시지 브로드캐스팅</p>
 * <p>연결 실패 시 자동 정리</p>
 * <p>{@link NotificationSseController} - SSE 구독 요청</p>
 * <p>{@link NotificationSendListener} - 알림 이벤트 발생 시 전송</p>
 * <p>로그아웃, 회원 제재, 회원 탈퇴 시 연결 정리</p>
 *
 * @author Jaeik
 * @version 2.3.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseRepository {

    /**
     * SSE Emitter 저장소
     * <p>키: EmitterId (memberId_tokenId_timestamp), 값: SseEmitter 객체</p>
     */
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * <h3>SSE 구독</h3>
     * <p>새로운 SSE 연결을 생성하고 초기화 메시지를 전송합니다.</p>
     * <p>onCompletion: 클라이언트 정상 종료 시 삭제</p>
     * <p>onTimeout: 연결 타임아웃 시 삭제</p>
     *
     * @param memberId 구독할 사용자의 ID
     * @param tokenId FCM 토큰 ID (멀티 디바이스 구분용)
     * @return 생성된 SseEmitter 객체 (무제한 타임아웃, 로그아웃 시 명시적으로 종료)
     * @author Jaeik
     * @since 2.3.0
     */
    public SseEmitter subscribe(Long memberId, Long tokenId) {
        String emitterId = memberId + "_" + tokenId + "_" + System.currentTimeMillis();
        log.info("SSE 구독 요청됨 - 멤버id={}, 토큰ID={}, 이미터ID={}", memberId, tokenId, emitterId);
        SseEmitter sseEmitter = new SseEmitter(0L);
        emitters.put(emitterId, sseEmitter);

        sseEmitter.onCompletion(() -> {
            log.info("SSE 연결 완료 - 이미터ID={}", emitterId);
            emitters.remove(emitterId);
        });
        sseEmitter.onTimeout(() -> {
            log.warn("SSE 타임아웃 - 이미터ID={}", emitterId);
            emitters.remove(emitterId);
        });
        sseEmitter.onError(throwable -> {
            log.warn("SSE 연결 오류 - 이미터ID={}, 메시지={}", emitterId,
                    throwable != null ? throwable.getMessage() : "원인불명", throwable);
            emitters.remove(emitterId);
        });

        SseMessage initMessage = SseMessage.of(memberId, NotificationType.INITIATE,
                "이벤트 스트림이 생성되었습니다. [이미터ID=%s]".formatted(emitterId), "");

        // 초기 메시지는 retry 간격을 포함하여 전송 (클라이언트 재연결 간격 5초)
        try {
            sseEmitter.send(SseEmitter.event()
                    .id(emitterId)
                    .name(initMessage.type().toString())
                    .data(initMessage.toJsonData())
                    .reconnectTime(5000L)); // 5초 후 재연결 시도
        } catch (IOException e) {
            log.warn("SSE 초기화 전송 실패 - 이미터ID={}, 이유={}", emitterId, e.getMessage(), e);
            emitters.remove(emitterId);
        }

        return sseEmitter;
    }

    /**
     * <h3>SSE 알림 전송</h3>
     * <p>해당 사용자의 모든 Emitter에 SSE 전송</p>
     *
     * @param sseMessage SSE 메시지 (사용자ID, 타입, 내용, URL 포함)
     * @author Jaeik
     * @since 2.3.0
     */
    public void send(SseMessage sseMessage) {
        String prefix = sseMessage.memberId() + "_";
        emitters.forEach((emitterId, emitter) -> {
            if (emitterId.startsWith(prefix)) {
                try {
                    emitter.send(SseEmitter.event()
                            .name(sseMessage.type().toString())
                            .data(sseMessage.toJsonData()));
                } catch (IOException | IllegalStateException e) {
                    log.warn("SSE 전송 실패, Emitter 정리: {} (이유: {})", emitterId, e.getMessage());
                    cleanup(emitterId, emitter);
                }
            }
        });
    }

    /**
     * <h3>SSE 연결 삭제</h3>
     * <p>사용자의 SSE 연결을 정리합니다. 로그아웃, 회원탈퇴 시 호출됩니다.</p>
     * <p>AuthEventHandler.handleLogout, UserEventHandler.handleWithdraw에서 호출됨</p>
     * <p>authTokenId == null: 사용자의 모든 연결 제거 (완전 로그아웃)</p>
     * <p>authTokenId != null: 특정 기기의 연결만 제거 (단일 기기 로그아웃)</p>
     *
     * @param memberId 사용자 ID
     * @param tokenId FCM 토큰 ID (null 허용, null이면 모든 연결 제거)
     * @author Jaeik
     * @since 2.3.0
     */
    public void deleteEmitters(Long memberId, Long tokenId) {
        if (tokenId != null) {
            emitters.entrySet().removeIf(entry -> entry.getKey().startsWith(memberId + "_" + tokenId + "_"));
        } else {
            emitters.entrySet().removeIf(entry -> entry.getKey().startsWith(memberId + "_"));
        }
    }

    /**
     * <h3>Heartbeat 전송 - 연결 활성 유지</h3>
     * <p>주기적으로 모든 활성 SSE 연결에 Heartbeat 메시지를 전송합니다.</p>
     *
     * <p>Heartbeat는 SSE comment 형태로 전송되며, 클라이언트에서 별도 처리 불필요합니다.</p>
     * <p>전송 실패 시 해당 Emitter를 자동으로 정리</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    @Scheduled(fixedDelay = 30000L)
    public void sendHeartbeat() {
        if (emitters.isEmpty()) return;

        log.debug("SSE Heartbeat 전송 시작. 활성 Emitter 수: {}", emitters.size());

        emitters.forEach((emitterId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (IOException | IllegalStateException e) {
                // 클라이언트가 이미 나갔거나 연결이 사용 불가능한 경우 (정상적인 끊김 상황)
                log.warn("Heartbeat 전송 실패, Emitter 정리: {} (이유: {})", emitterId, e.getMessage());
                cleanup(emitterId, emitter);
            } catch (Exception e) {
                // 그 외 진짜 예상치 못한 심각한 에러
                log.error("Heartbeat 전송 중 알 수 없는 오류: {}", emitterId, e);
                cleanup(emitterId, emitter);
            }
        });
    }

    private void cleanup(String emitterId, SseEmitter emitter) {
        emitters.remove(emitterId);
        try {
            emitter.complete(); // 명시적으로 종료하여 서블릿 컨테이너에 알림
        } catch (Exception e) {
            // 이미 종료된 경우 무시
        }
    }
}
