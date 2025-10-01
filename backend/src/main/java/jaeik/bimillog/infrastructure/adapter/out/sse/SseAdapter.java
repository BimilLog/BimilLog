package jaeik.bimillog.infrastructure.adapter.out.sse;

import jaeik.bimillog.domain.notification.application.port.out.NotificationCommandPort;
import jaeik.bimillog.domain.notification.application.port.out.NotificationUtilPort;
import jaeik.bimillog.domain.notification.application.port.out.SsePort;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.notification.entity.SseMessage;
import jaeik.bimillog.domain.notification.exception.NotificationCustomException;
import jaeik.bimillog.domain.notification.exception.NotificationErrorCode;
import jaeik.bimillog.domain.member.application.port.in.MemberQueryUseCase;
import jaeik.bimillog.domain.member.entity.member.Member;
import jaeik.bimillog.infrastructure.adapter.in.notification.listener.NotificationGenerateListener;
import jaeik.bimillog.infrastructure.adapter.in.notification.web.NotificationSseController;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * <h2>SSE 어댑터</h2>
 * <p>SSE의 연결 관리와 알림 전송을 담당합니다.</p>
 * <p>다중 기기 지원을 위해 사용자별, 토큰별로 복수의 SSE 연결을 관리합니다.</p>
 *
 * <h3>주요 기능</h3>
 * <ul>
 *   <li>SSE 연결 구독 및 초기화 메시지 전송</li>
 *   <li>사용자별 다중 Emitter 관리 (멀티 디바이스 지원)</li>
 *   <li>알림 메시지 브로드캐스팅</li>
 *   <li>연결 실패 시 자동 정리</li>
 * </ul>
 *
 ** <ul>
 *   <li>{@link NotificationSseController} - SSE 구독 요청</li>
 *   <li>{@link NotificationGenerateListener} - 알림 이벤트 발생 시 전송</li>
 *   <li>로그아웃, 회원 제재, 회원 탈퇴 시 연결 정리</li>
 * </ul>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class SseAdapter implements SsePort {

    /**
     * SSE Emitter 저장소
     * <p>키: EmitterId (memberId_tokenId_timestamp), 값: SseEmitter 객체</p>
     */
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    private final NotificationUtilPort notificationUtilPort;
    private final MemberQueryUseCase memberQueryUseCase;
    private final NotificationCommandPort notificationCommandPort;

    /**
     * <h3>SSE 구독 - 실시간 알림 채널 생성</h3>
     * <p>새로운 SSE 연결을 생성하고 초기화 메시지를 전송합니다.</p>
     *
     * <p>연결 생명주기 관리:</p>
     * <ul>
     *   <li>onCompletion: 클라이언트 정상 종료 시 {@link #deleteById} 호출</li>
     *   <li>onTimeout: 연결 타임아웃 시 {@link #deleteById} 호출</li>
     * </ul>
     *
     * @param memberId 구독할 사용자의 ID
     * @param tokenId FCM 토큰 ID (멀티 디바이스 구분용)
     * @return 생성된 SseEmitter 객체 (무한 타임아웃)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public SseEmitter subscribe(Long memberId, Long tokenId) {
        String emitterId = makeTimeIncludeId(memberId, tokenId);
        SseEmitter emitter = save(emitterId, new SseEmitter(Long.MAX_VALUE));

        emitter.onCompletion(() -> deleteById(emitterId));
        emitter.onTimeout(() -> deleteById(emitterId));

        SseMessage initMessage = SseMessage.of(memberId, NotificationType.INITIATE,
                "이벤트 스트림이 생성되었습니다. [emitterId=%s]".formatted(emitterId), "");
        sendNotification(emitter, emitterId, initMessage);

        return emitter;
    }

    /**
     * <h3>SSE 알림 전송 - 실시간 이벤트 브로드캐스팅</h3>
     * <p>지정된 사용자의 모든 활성 연결에 알림을 전송하고 DB에 저장합니다.</p>
     * <p>NotificationEventHandler와 각종 도메인 이벤트 핸들러에서 호출됨</p>
     *
     * <p>처리 흐름:</p>
     * <ol>
     *   <li>알림 설정 확인 ({@link NotificationUtilPort#SseEligibleForNotification})</li>
     *   <li>사용자 정보 조회 및 검증</li>
     *   <li>알림 데이터베이스 저장</li>
     *   <li>해당 사용자의 모든 Emitter에 브로드캐스팅</li>
     * </ol>
     *
     * @param sseMessage SSE 메시지 (사용자ID, 타입, 내용, URL 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void send(SseMessage sseMessage) {
        try {
            // 알림 설정 확인
            if (!notificationUtilPort.SseEligibleForNotification(sseMessage.memberId(), sseMessage.type())) {
                return; // 알림 수신이 비활성화된 경우 전송하지 않음
            }
            
            Member member = memberQueryUseCase.findById(sseMessage.memberId())
                    .orElseThrow(() -> new NotificationCustomException(NotificationErrorCode.INVALID_USER_CONTEXT));
            notificationCommandPort.save(member, sseMessage.type(), sseMessage.message(), sseMessage.url());
            Map<String, SseEmitter> emitters = findAllEmitterByMemberId(sseMessage.memberId());

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
     * <h3>단일 SSE 알림 전송 - 개별 연결 처리</h3>
     * <p>특정 Emitter를 통해 클라이언트에게 알림을 전송합니다.</p>
     * <p>{@link #subscribe}의 초기 메시지와 {@link #send}의 실제 알림 전송에서 호출됨</p>
     *
     * <p>IOException 발생 시 해당 Emitter를 자동으로 정리합니다.</p>
     * <p>이는 클라이언트 연결이 끊어진 경우를 처리하기 위함입니다.</p>
     *
     * @param emitter 전송에 사용할 SseEmitter 객체
     * @param emitterId Emitter의 고유 ID (삭제 시 사용)
     * @param sseMessage SSE 메시지 (타입별 이벤트명과 JSON 데이터)
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
     * <p>사용자의 SSE 연결을 정리합니다. 로그아웃, 회원탈퇴 시 호출됩니다.</p>
     * <p>AuthEventHandler.handleLogout, UserEventHandler.handleWithdraw에서 호출됨</p>
     *
     * <p>동작 방식:</p>
     * <ul>
     *   <li>tokenId == null: 사용자의 모든 연결 제거 (완전 로그아웃)</li>
     *   <li>tokenId != null: 특정 기기의 연결만 제거 (단일 기기 로그아웃)</li>
     * </ul>
     *
     * @param memberId 사용자 ID
     * @param tokenId FCM 토큰 ID (null 허용, null이면 모든 연결 제거)
     * @see #deleteAllEmitterByUserId(Long)
     * @see #deleteEmitterByUserIdAndTokenId(Long, Long)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteEmitters(Long memberId, Long tokenId) {
        if (tokenId != null) {
            deleteEmitterByUserIdAndTokenId(memberId, tokenId);
        } else {
            deleteAllEmitterByUserId(memberId);
        }
    }

    /**
     * <h3>시간 기반 고유 Emitter ID 생성</h3>
     * <p>동일한 사용자/기기에서도 시간으로 구분되는 고유 ID를 생성합니다.</p>
     *
     * @param memberId 사용자 ID
     * @param tokenId FCM 토큰 ID
     * @return 생성된 고유 Emitter ID (시간 기반으로 중복 불가)
     * @author Jaeik
     * @since 2.0.0
     */
    private String makeTimeIncludeId(Long memberId, Long tokenId) {
        return memberId + "_" + tokenId + "_" + System.currentTimeMillis();
    }

    /**
     * <h3>Emitter 저장 - Map에 연결 등록</h3>
     * <p>새로운 SSE 연결을 내부 저장소에 등록합니다.</p>
     * <p>{@link #subscribe}에서 새 연결 생성 시 호출됨</p>
     *
     * @param emitterId  고유 Emitter ID (memberId_tokenId_timestamp)
     * @param sseEmitter 저장할 SseEmitter 객체
     * @return 저장된 SseEmitter 객체 (체이닝용)
     * @author Jaeik
     * @since 2.0.0
     */
    private SseEmitter save(String emitterId, SseEmitter sseEmitter) {
        emitters.put(emitterId, sseEmitter);
        return sseEmitter;
    }

    /**
     * <h3>사용자의 모든 Emitter 조회 - 멀티 디바이스 지원</h3>
     * <p>특정 사용자의 모든 활성 SSE 연결을 조회합니다.</p>
     *
     * <p>memberId로 시작하는 모든 emitterId를 필터링합니다.</p>
     *
     * @param memberId 조회할 사용자 ID
     * @return 해당 사용자의 모든 Emitter Map (비어있을 수 있음)
     * @author Jaeik
     * @since 2.0.0
     */
    private Map<String, SseEmitter> findAllEmitterByMemberId(Long memberId) {
        String memberIdPrefix = memberId + "_";
        return emitters.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(memberIdPrefix))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * <h3>단일 Emitter 삭제 - 연결 정리</h3>
     * <p>특정 Emitter를 Map에서 제거합니다.</p>
     * <p>다음 상황에서 호출됨:</p>
     * <ul>
     *   <li>{@link #subscribe}의 onCompletion/onTimeout 콜백</li>
     *   <li>{@link #sendNotification}의 IOException 발생 시</li>
     * </ul>
     *
     * @param emitterId 삭제할 Emitter ID
     * @author Jaeik
     * @since 2.0.0
     */
    private void deleteById(String emitterId) {
        emitters.remove(emitterId);
    }

    /**
     * <h3>사용자의 모든 Emitter 일괄 삭제 - 완전 로그아웃</h3>
     * <p>특정 사용자의 모든 SSE 연결을 한 번에 제거합니다.</p>
     * <p>{@link #deleteEmitters}에서 tokenId가 null일 때 호출됨</p>
     *
     * <p>removeIf를 사용하여 원자적으로 삭제를 수행합니다.</p>
     * <p>회원탈퇴 등 사용자의 모든 연결을 정리할 때 사용됩니다.</p>
     *
     * @param memberId 삭제할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    private void deleteAllEmitterByUserId(Long memberId) {
        String memberIdPrefix = memberId + "_";
        emitters.entrySet().removeIf(entry -> entry.getKey().startsWith(memberIdPrefix));
    }

    /**
     * <h3>특정 기기 Emitter 삭제 - 선택적 로그아웃</h3>
     * <p>사용자의 특정 기기(토큰)에 해당하는 연결만 제거합니다.</p>
     * <p>{@link #deleteEmitters}에서 tokenId가 지정됐을 때 호출됨</p>
     *
     * <p>멀티 디바이스 환경에서 하나의 기기만 로그아웃할 때 사용됩니다.</p>
     * <p>같은 기기의 여러 연결(재접속 등)을 모두 정리합니다.</p>
     *
     * @param memberId 사용자 ID
     * @param tokenId FCM 토큰 ID (기기 식별자)
     * @author Jaeik
     * @since 2.0.0
     */
    private void deleteEmitterByUserIdAndTokenId(Long memberId, Long tokenId) {
        String memberTokenPrefix = memberId + "_" + tokenId + "_";
        emitters.entrySet().removeIf(entry -> entry.getKey().startsWith(memberTokenPrefix));
    }
}