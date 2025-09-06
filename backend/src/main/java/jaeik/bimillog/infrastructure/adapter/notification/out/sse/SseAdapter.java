package jaeik.bimillog.infrastructure.adapter.notification.out.sse;

import jaeik.bimillog.domain.notification.application.port.out.NotificationCommandPort;
import jaeik.bimillog.domain.notification.application.port.out.NotificationUtilPort;
import jaeik.bimillog.domain.notification.application.port.out.SsePort;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.notification.exception.NotificationCustomException;
import jaeik.bimillog.domain.notification.exception.NotificationErrorCode;
import jaeik.bimillog.domain.user.application.port.in.UserQueryUseCase;
import jaeik.bimillog.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

/**
 * <h2>SSE Emitter 어댑터</h2>
 * <p>SSE 연결 관리를 위한 인프라 구현</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class SseAdapter implements SsePort {

    private final EmitterRepository emitterRepository;
    private final NotificationUtilPort notificationUtilPort;
    private final UserQueryUseCase userQueryUseCase;
    private final NotificationCommandPort notificationCommandPort;

    /**
     * <h3>SSE 구독</h3>
     * <p>주어진 사용자 ID와 토큰 ID로 SSE 연결을 구독하고 초기 데이터를 전송합니다.</p>
     *
     * @param userId 구독할 사용자의 ID
     * @param tokenId 구독 토큰 ID
     * @return 생성된 SseEmitter 객체
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public SseEmitter subscribe(Long userId, Long tokenId) {
        String emitterId = notificationUtilPort.makeTimeIncludeId(userId, tokenId);
        SseEmitter emitter = emitterRepository.save(emitterId, new SseEmitter(Long.MAX_VALUE));

        emitter.onCompletion(() -> emitterRepository.deleteById(emitterId));
        emitter.onTimeout(() -> emitterRepository.deleteById(emitterId));

        sendNotification(emitter, emitterId, NotificationType.INITIATE,
                "이벤트 스트림이 생성되었습니다. [emitterId=%s]".formatted(emitterId), "");

        return emitter;
    }

    /**
     * <h3>SSE 알림 전송</h3>
     * <p>지정된 사용자에게 SSE 알림을 전송하고 데이터베이스에 알림을 저장합니다.</p>
     *
     * @param userId  알림을 받을 사용자의 ID
     * @param type    알림 유형
     * @param message 알림 메시지
     * @param url     알림 URL
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void send(Long userId, NotificationType type, String message, String url) {
        try {
            User user = userQueryUseCase.findById(userId)
                    .orElseThrow(() -> new NotificationCustomException(NotificationErrorCode.INVALID_USER_CONTEXT));
            notificationCommandPort.save(user, type, message, url);
            Map<String, SseEmitter> emitters = emitterRepository.findAllEmitterByUserId(userId);

            emitters.forEach(
                    (emitterId, emitter) -> {
                        sendNotification(emitter, emitterId, type, message, url);
                    });
        } catch (Exception e) {
            throw new NotificationCustomException(NotificationErrorCode.NOTIFICATION_SEND_ERROR, e);
        }
    }

    /**
     * <h3>단일 SSE 알림 전송</h3>
     * <p>주어진 Emitter를 통해 클라이언트에게 단일 알림을 전송합니다.</p>
     *
     * @param emitter 전송에 사용할 SseEmitter 객체
     * @param emitterId Emitter의 고유 ID
     * @param type 알림 유형
     * @param data 알림 내용
     * @param url 알림 클릭 시 이동할 URL
     * @author Jaeik
     * @since 2.0.0
     */
    private void sendNotification(SseEmitter emitter, String emitterId, NotificationType type, String data, String url) {
        String jsonData = String.format("{\"message\": \"%s\", \"url\": \"%s\"}",
                data, url);
        try {
            emitter.send(SseEmitter.event()
                    .name(type.toString())
                    .data(jsonData));
        } catch (IOException e) {
            emitterRepository.deleteById(emitterId);
        }
    }


    /**
     * <h3>사용자 SSE 연결 정리</h3>
     * <p>사용자와 관련된 모든 SSE Emitter 연결을 정리합니다.</p>
     *
     * @param userId 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteAllEmitterByUserId(Long userId) {
        emitterRepository.deleteAllEmitterByUserId(userId);
    }

    /**
     * <h3>특정 기기 SSE 연결 정리</h3>
     * <p>사용자의 특정 기기(토큰)에 해당하는 SSE Emitter 연결을 정리합니다.</p>
     * <p>다중 기기 로그인 환경에서 특정 기기만 로그아웃 처리할 때 사용합니다.</p>
     *
     * @param userId 사용자 ID
     * @param tokenId 토큰 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteEmitterByUserIdAndTokenId(Long userId, Long tokenId) {
        emitterRepository.deleteEmitterByUserIdAndTokenId(userId, tokenId);
    }
}