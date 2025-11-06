package jaeik.bimillog.adapter.out.sse;

import jaeik.bimillog.domain.member.application.port.in.MemberQueryUseCase;
import jaeik.bimillog.domain.notification.application.port.out.NotificationCommandPort;
import jaeik.bimillog.domain.notification.application.port.out.NotificationUtilPort;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.notification.entity.SseMessage;
import jaeik.bimillog.domain.notification.exception.NotificationCustomException;
import jaeik.bimillog.domain.notification.exception.NotificationErrorCode;
import jaeik.bimillog.domain.notification.out.SseAdapter;
import jaeik.bimillog.testutil.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>SseAdapter 단위 테스트</h2>
 * <p>SSE(Server-Sent Events) 연결 관리 및 알림 전송 기능을 검증합니다.</p>
 * <p>ConcurrentHashMap 기반 Emitter 관리와 동시성 처리를 테스트합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @see SseAdapter
 */
@Tag("unit")
@DisplayName("SseAdapter 단위 테스트")
class SseAdapterTest extends BaseUnitTest {

    @Mock
    private NotificationUtilPort notificationUtilPort;

    @Mock
    private MemberQueryUseCase memberQueryUseCase;

    @Mock
    private NotificationCommandPort notificationCommandPort;

    @InjectMocks
    private SseAdapter sseAdapter;

    private Long memberId;
    private Long tokenId;

    @BeforeEach
    void setUp() {
        memberId = 1L;
        tokenId = 100L;
    }

    /**
     * Private 필드에 접근하기 위한 Reflection 유틸리티 메서드
     */
    @SuppressWarnings("unchecked")
    private Map<String, SseEmitter> getEmittersMap() throws Exception {
        Field field = SseAdapter.class.getDeclaredField("emitters");
        field.setAccessible(true);
        return (Map<String, SseEmitter>) field.get(sseAdapter);
    }


    @Test
    @DisplayName("SSE 구독 - Emitter 생성 및 저장 확인")
    void shouldSubscribe_WhenValidInput() throws Exception {
        // When
        SseEmitter result = sseAdapter.subscribe(memberId, tokenId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(SseEmitter.class);

        // Reflection으로 내부 Map 확인
        Map<String, SseEmitter> emitters = getEmittersMap();
        assertThat(emitters).isNotEmpty();

        // EmitterId 형식 검증 (memberId_tokenId_timestamp)
        String emitterIdPattern = memberId + "_" + tokenId + "_";
        boolean hasMatchingEmitter = emitters.keySet().stream()
                .anyMatch(key -> key.startsWith(emitterIdPattern));
        assertThat(hasMatchingEmitter).isTrue();
    }

    @Test
    @DisplayName("SSE 알림 전송 - 정상 전송 확인")
    void shouldSend_WhenValidInput() throws Exception {
        // Given
        String emitterId = memberId + "_100_1234567890";
        SseEmitter mockEmitter = mock(SseEmitter.class);

        // Reflection으로 emitters Map에 테스트 데이터 주입
        Map<String, SseEmitter> emitters = getEmittersMap();
        emitters.put(emitterId, mockEmitter);

        given(notificationUtilPort.SseEligibleForNotification(memberId, NotificationType.COMMENT)).willReturn(true);
        given(memberQueryUseCase.findById(memberId)).willReturn(Optional.of(getTestMember()));

        // When
        SseMessage sseMessage = SseMessage.of(memberId, NotificationType.COMMENT,
                "테스터님이 댓글을 남겼습니다!", "/board/post/100");
        sseAdapter.send(sseMessage);

        // Then
        verify(notificationUtilPort).SseEligibleForNotification(memberId, NotificationType.COMMENT);
        verify(memberQueryUseCase).findById(memberId);
        verify(notificationCommandPort).save(eq(getTestMember()), eq(NotificationType.COMMENT), anyString(), anyString());

        // SseEmitter로 실제 전송 시도 확인
        verify(mockEmitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("SSE 알림 전송 - 사용자 없음 예외")
    void shouldThrowException_WhenMemberNotFound() {
        // Given
        given(notificationUtilPort.SseEligibleForNotification(memberId, NotificationType.COMMENT)).willReturn(true);
        given(memberQueryUseCase.findById(memberId)).willReturn(Optional.empty());

        // When & Then
        SseMessage sseMessage = SseMessage.of(memberId, NotificationType.COMMENT, "테스트 메시지", "/test/url");
        assertThatThrownBy(() -> sseAdapter.send(sseMessage))
                .isInstanceOf(NotificationCustomException.class)
                .hasFieldOrPropertyWithValue("notificationErrorCode", NotificationErrorCode.INVALID_USER_CONTEXT);

        verify(notificationUtilPort).SseEligibleForNotification(memberId, NotificationType.COMMENT);
        verify(memberQueryUseCase).findById(memberId);
        verify(notificationCommandPort, never()).save(any(), any(), any(), any());
    }

    @Test
    @DisplayName("SSE 알림 전송 - 알림 저장 실패 시 예외")
    void shouldThrowException_WhenNotificationSaveFails() {
        // Given
        given(notificationUtilPort.SseEligibleForNotification(memberId, NotificationType.COMMENT)).willReturn(true);
        given(memberQueryUseCase.findById(memberId)).willReturn(Optional.of(getTestMember()));
        doThrow(new RuntimeException("DB 저장 실패"))
                .when(notificationCommandPort).save(any(), any(), any(), any());

        // When & Then
        SseMessage sseMessage = SseMessage.of(memberId, NotificationType.COMMENT, "테스트 메시지", "/test/url");
        assertThatThrownBy(() -> sseAdapter.send(sseMessage))
                .isInstanceOf(NotificationCustomException.class)
                .hasFieldOrPropertyWithValue("notificationErrorCode", NotificationErrorCode.NOTIFICATION_SEND_ERROR);

        verify(notificationUtilPort).SseEligibleForNotification(memberId, NotificationType.COMMENT);
        verify(memberQueryUseCase).findById(memberId);
        verify(notificationCommandPort).save(eq(getTestMember()), eq(NotificationType.COMMENT), anyString(), anyString());
    }

    @Test
    @DisplayName("SSE 알림 전송 - Emitter가 없는 경우 정상 처리")
    void shouldHandleEmptyEmitters_WhenNoEmittersExist() throws Exception {
        // Given
        given(notificationUtilPort.SseEligibleForNotification(memberId, NotificationType.COMMENT)).willReturn(true);
        given(memberQueryUseCase.findById(memberId)).willReturn(Optional.of(getTestMember()));

        // emitters Map이 비어있는 상태 확인
        Map<String, SseEmitter> emitters = getEmittersMap();
        emitters.clear();

        // When
        SseMessage sseMessage = SseMessage.of(memberId, NotificationType.COMMENT, "테스트 메시지", "/test/url");
        sseAdapter.send(sseMessage);

        // Then
        verify(notificationUtilPort).SseEligibleForNotification(memberId, NotificationType.COMMENT);
        verify(memberQueryUseCase).findById(memberId);
        verify(notificationCommandPort).save(eq(getTestMember()), eq(NotificationType.COMMENT), anyString(), anyString());
        // Emitter가 없어도 예외가 발생하지 않아야 함
    }

    @Test
    @DisplayName("사용자의 모든 SSE 연결 정리")
    void shouldDeleteAllEmitterByMemberId_WhenTokenIdIsNull() throws Exception {
        // Given
        Map<String, SseEmitter> emitters = getEmittersMap();
        emitters.put(memberId + "_100_1234567890", mock(SseEmitter.class));
        emitters.put(memberId + "_101_1234567891", mock(SseEmitter.class));
        emitters.put("999_102_1234567892", mock(SseEmitter.class)); // 다른 사용자

        // When
        sseAdapter.deleteEmitters(memberId, null);

        // Then
        Map<String, SseEmitter> remainingEmitters = getEmittersMap();
        assertThat(remainingEmitters).hasSize(1);
        assertThat(remainingEmitters.containsKey("999_102_1234567892")).isTrue();
    }

    @Test
    @DisplayName("특정 기기 SSE 연결 정리")
    void shouldDeleteEmitterByMemberIdAndTokenId_WhenTokenIdProvided() throws Exception {
        // Given
        Map<String, SseEmitter> emitters = getEmittersMap();
        emitters.put(memberId + "_100_1234567890", mock(SseEmitter.class));
        emitters.put(memberId + "_100_1234567891", mock(SseEmitter.class)); // 같은 토큰
        emitters.put(memberId + "_101_1234567892", mock(SseEmitter.class)); // 다른 토큰

        // When
        sseAdapter.deleteEmitters(memberId, 100L);

        // Then
        Map<String, SseEmitter> remainingEmitters = getEmittersMap();
        assertThat(remainingEmitters).hasSize(1);
        assertThat(remainingEmitters.containsKey(memberId + "_101_1234567892")).isTrue();
    }

    @Test
    @DisplayName("여러 Emitter에 동시 전송")
    void shouldSendToMultipleEmitters() throws Exception {
        // Given
        Map<String, SseEmitter> emitters = getEmittersMap();
        SseEmitter mockEmitter1 = mock(SseEmitter.class);
        SseEmitter mockEmitter2 = mock(SseEmitter.class);

        emitters.put(memberId + "_100_1234567890", mockEmitter1);
        emitters.put(memberId + "_101_1234567891", mockEmitter2);

        given(notificationUtilPort.SseEligibleForNotification(memberId, NotificationType.COMMENT)).willReturn(true);
        given(memberQueryUseCase.findById(memberId)).willReturn(Optional.of(getTestMember()));

        // When
        SseMessage sseMessage = SseMessage.of(memberId, NotificationType.COMMENT, "테스트 메시지", "/test/url");
        sseAdapter.send(sseMessage);

        // Then
        verify(notificationUtilPort).SseEligibleForNotification(memberId, NotificationType.COMMENT);
        verify(memberQueryUseCase).findById(memberId);
        verify(notificationCommandPort).save(eq(getTestMember()), eq(NotificationType.COMMENT), anyString(), anyString());

        // 두 Emitter 모두에게 전송 확인
        verify(mockEmitter1, times(1)).send(any(SseEmitter.SseEventBuilder.class));
        verify(mockEmitter2, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("SSE 알림 전송 - 알림 설정 비활성화 시 조기 리턴")
    void shouldNotSend_WhenNotificationDisabled() {
        // Given
        given(notificationUtilPort.SseEligibleForNotification(memberId, NotificationType.COMMENT)).willReturn(false);

        // When
        SseMessage sseMessage = SseMessage.of(memberId, NotificationType.COMMENT, "테스트 메시지", "/test/url");
        sseAdapter.send(sseMessage);

        // Then: 알림 설정 확인만 하고 나머지는 호출되지 않아야 함
        verify(notificationUtilPort).SseEligibleForNotification(memberId, NotificationType.COMMENT);
        verify(memberQueryUseCase, never()).findById(any());
        verify(notificationCommandPort, never()).save(any(), any(), any(), any());
    }

    @Test
    @DisplayName("IOException 발생 시 Emitter 자동 정리")
    void shouldRemoveEmitterOnIOException() throws Exception {
        // Given
        String emitterId = memberId + "_100_1234567890";
        SseEmitter mockEmitter = mock(SseEmitter.class);

        Map<String, SseEmitter> emitters = getEmittersMap();
        emitters.put(emitterId, mockEmitter);

        // IOException 발생하도록 설정
        doThrow(new IOException("Connection lost"))
                .when(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));

        given(notificationUtilPort.SseEligibleForNotification(memberId, NotificationType.COMMENT)).willReturn(true);
        given(memberQueryUseCase.findById(memberId)).willReturn(Optional.of(getTestMember()));

        // When
        SseMessage sseMessage = SseMessage.of(memberId, NotificationType.COMMENT, "테스트 메시지", "/test/url");
        sseAdapter.send(sseMessage);

        // Then: IOException이 발생해도 전체 프로세스는 정상 완료
        verify(notificationCommandPort).save(eq(getTestMember()), eq(NotificationType.COMMENT), anyString(), anyString());

        // Emitter가 Map에서 제거되었는지 확인
        Map<String, SseEmitter> remainingEmitters = getEmittersMap();
        assertThat(remainingEmitters).doesNotContainKey(emitterId);
    }
}