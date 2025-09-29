package jaeik.bimillog.infrastructure.adapter.out.sse;

import jaeik.bimillog.domain.notification.application.port.out.NotificationCommandPort;
import jaeik.bimillog.domain.notification.application.port.out.NotificationUtilPort;
import jaeik.bimillog.domain.notification.application.port.out.SsePort;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.notification.entity.SseMessage;
import jaeik.bimillog.domain.notification.exception.NotificationCustomException;
import jaeik.bimillog.domain.notification.exception.NotificationErrorCode;
import jaeik.bimillog.domain.user.application.port.in.UserQueryUseCase;
import jaeik.bimillog.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * <h2>SSE 어댑터</h2>
 * <p>Server-Sent Events 연결 관리와 메시지 전송을 담당하는 어댑터입니다.</p>
 * <p>SSE 구독, SSE 연결 정리, SSE 메시지 전송, Emitter ID 생성</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class SseAdapter implements SsePort {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

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
        String emitterId = makeTimeIncludeId(userId, tokenId);
        SseEmitter emitter = save(emitterId, new SseEmitter(Long.MAX_VALUE));

        emitter.onCompletion(() -> deleteById(emitterId));
        emitter.onTimeout(() -> deleteById(emitterId));

        SseMessage initMessage = SseMessage.of(userId, NotificationType.INITIATE,
                "이벤트 스트림이 생성되었습니다. [emitterId=%s]".formatted(emitterId), "");
        sendNotification(emitter, emitterId, initMessage);

        return emitter;
    }

    /**
     * <h3>SSE 알림 전송</h3>
     * <p>지정된 사용자에게 SSE 알림을 전송하고 데이터베이스에 알림을 저장합니다.</p>
     *
     * @param sseMessage SSE 메시지 값 객체
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void send(SseMessage sseMessage) {
        try {
            // 알림 설정 확인
            if (!notificationUtilPort.SseEligibleForNotification(sseMessage.userId(), sseMessage.type())) {
                return; // 알림 수신이 비활성화된 경우 전송하지 않음
            }
            
            User user = userQueryUseCase.findById(sseMessage.userId())
                    .orElseThrow(() -> new NotificationCustomException(NotificationErrorCode.INVALID_USER_CONTEXT));
            notificationCommandPort.save(user, sseMessage.type(), sseMessage.message(), sseMessage.url());
            Map<String, SseEmitter> emitters = findAllEmitterByUserId(sseMessage.userId());

            emitters.forEach(
                    (emitterId, emitter) -> {
                        sendNotification(emitter, emitterId, sseMessage);
                    });
        } catch (NotificationCustomException e) {
            throw e; // 비즈니스 예외는 그대로 전파
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
     * @param sseMessage SSE 메시지 값 객체
     * @author Jaeik
     * @since 2.0.0
     */
    private void sendNotification(SseEmitter emitter, String emitterId, SseMessage sseMessage) {
        try {
            emitter.send(SseEmitter.event()
                    .name(sseMessage.type().toString())
                    .data(sseMessage.toJsonData()));
        } catch (IOException e) {
            deleteById(emitterId);
        }
    }



    /**
     * <h3>SSE 연결 정리</h3>
     * <p>tokenId가 null인 경우 사용자의 모든 SSE Emitter 연결을 정리하고,</p>
     * <p>tokenId가 있는 경우 해당 기기의 SSE Emitter 연결만 선택적으로 정리합니다.</p>
     *
     * @param userId 사용자 ID
     * @param tokenId 토큰 ID (null인 경우 모든 연결 정리)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteEmitters(Long userId, Long tokenId) {
        if (tokenId != null) {
            deleteEmitterByUserIdAndTokenId(userId, tokenId);
        } else {
            deleteAllEmitterByUserId(userId);
        }
    }

    /**
     * <h3>시간 포함 Emitter ID 생성</h3>
     * <p>사용자 ID, 토큰 ID, 현재 시간을 조합하여 고유한 Emitter ID를 생성합니다.</p>
     *
     * @param userId 사용자 ID
     * @param tokenId 토큰 ID
     * @return 생성된 Emitter ID 문자열
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public String makeTimeIncludeId(Long userId, Long tokenId) {
        return userId + "_" + tokenId + "_" + System.currentTimeMillis();
    }

    /**
     * <h3>Emitter 저장</h3>
     *
     * <p>고유한 Emitter ID로 SseEmitter 객체를 저장합니다.</p>
     * <p>사용자 ID와 토큰 ID와 생성 시간을 조합하여 고유한 Emitter ID를 생성합니다.</p>
     *
     * @param emitterId  Emitter ID
     * @param sseEmitter SseEmitter 객체
     * @return 저장된 SseEmitter 객체
     * @author Jaeik
     * @since 2.0.0
     */
    private SseEmitter save(String emitterId, SseEmitter sseEmitter) {
        emitters.put(emitterId, sseEmitter);
        return sseEmitter;
    }

    /**
     * <h3>사용자의 모든 Emitter 조회</h3>
     *
     * <p>사용자 ID에 해당하는 모든 Emitter를 조회합니다.</p>
     * <p>해당 사용자에 연결된 모든 기기로 알림을 보내는 용도입니다.</p>
     * <p>EmitterId 형식: "userId_tokenId_timestamp"</p>
     *
     * @param userId 유저 ID
     * @return 사용자 ID에 해당하는 모든 Emitter Map
     * @author Jaeik
     * @since 2.0.0
     */
    private Map<String, SseEmitter> findAllEmitterByUserId(Long userId) {
        String userIdPrefix = userId + "_";
        return emitters.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(userIdPrefix))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * <h3>사용자의 특정 Emitter 삭제</h3>
     *
     * <p>EmitterID로 특정한 SseEmitter 객체를 삭제합니다.</p>
     * <p>일시적 오류에서 사용합니다.</p>
     *
     * @param emitterId emitter ID
     * @author Jaeik
     * @since 2.0.0
     */
    private void deleteById(String emitterId) {
        emitters.remove(emitterId);
    }

    /**
     * <h3>사용자의 모든 Emitter 삭제</h3>
     *
     * <p>사용자 ID에 해당하는 모든 Emitter를 삭제합니다.</p>
     * <p>로그아웃, 회원탈퇴에서 사용합니다.</p>
     * <p>EmitterId 형식: "userId_tokenId_timestamp"</p>
     *
     * @param userId 유저 ID
     * @author Jaeik
     * @since 2.0.0
     */
    private void deleteAllEmitterByUserId(Long userId) {
        String userIdPrefix = userId + "_";
        emitters.entrySet().removeIf(entry -> entry.getKey().startsWith(userIdPrefix));
    }

    /**
     * <h3>사용자의 특정 토큰 Emitter 삭제</h3>
     *
     * <p>사용자 ID와 토큰 ID에 해당하는 특정 기기의 Emitter를 삭제합니다.</p>
     * <p>다중 기기 로그인 환경에서 특정 기기만 로그아웃 처리할 때 사용합니다.</p>
     * <p>EmitterId 형식: "userId_tokenId_timestamp"</p>
     *
     * @param userId 유저 ID
     * @param tokenId 토큰 ID
     * @author Jaeik
     * @since 2.0.0
     */
    private void deleteEmitterByUserIdAndTokenId(Long userId, Long tokenId) {
        String userTokenPrefix = userId + "_" + tokenId + "_";
        emitters.entrySet().removeIf(entry -> entry.getKey().startsWith(userTokenPrefix));
    }
}