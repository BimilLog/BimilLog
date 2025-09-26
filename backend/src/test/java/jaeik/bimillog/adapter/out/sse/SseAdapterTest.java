package jaeik.bimillog.adapter.out.sse;

import jaeik.bimillog.domain.notification.application.port.out.NotificationCommandPort;
import jaeik.bimillog.domain.notification.application.port.out.NotificationUtilPort;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.notification.entity.SseMessage;
import jaeik.bimillog.domain.notification.exception.NotificationCustomException;
import jaeik.bimillog.domain.notification.exception.NotificationErrorCode;
import jaeik.bimillog.domain.user.application.port.in.UserQueryUseCase;
import jaeik.bimillog.infrastructure.adapter.out.sse.EmitterRepository;
import jaeik.bimillog.infrastructure.adapter.out.sse.SseAdapter;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.SseTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>SseAdapter 테스트</h2>
 * <p>SSE Emitter 어댑터의 인프라 구현을 검증하는 단위 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("SseAdapter 테스트")
@Tag("fast")
class SseAdapterTest extends BaseUnitTest {

    @Mock
    private EmitterRepository emitterRepository;

    @Mock
    private NotificationUtilPort notificationUtilPort;

    @Mock
    private UserQueryUseCase userQueryUseCase;

    @Mock
    private NotificationCommandPort notificationCommandPort;

    @InjectMocks
    private SseAdapter sseAdapter;

    private Long userId;
    private Long tokenId;
    private String emitterId;
    private SseEmitter mockEmitter;

    @BeforeEach
    void setUp() {
        userId = 1L;
        tokenId = 100L;
        emitterId = SseTestHelper.defaultEmitterId(userId);
        mockEmitter = SseTestHelper.createMockEmitter();
    }

    @Test
    @DisplayName("SSE 구독 - 성공")
    void shouldSubscribe_WhenValidInput() {
        // Given
        when(emitterRepository.save(any(String.class), any(SseEmitter.class)))
            .thenAnswer(invocation -> invocation.getArgument(1));

        // When
        SseEmitter result = sseAdapter.subscribe(userId, tokenId);

        // Then
        assertThat(result).isNotNull();
        verify(emitterRepository).save(any(String.class), any(SseEmitter.class));
        assertThat(result).isInstanceOf(SseEmitter.class);
    }

    @Test
    @DisplayName("SSE 알림 전송 - 성공")
    void shouldSend_WhenValidInput() {
        // Given
        given(notificationUtilPort.SseEligibleForNotification(userId, NotificationType.COMMENT)).willReturn(true);
        given(userQueryUseCase.findById(userId)).willReturn(Optional.of(getTestUser()));
        SseTestHelper.setupSingleEmitter(emitterRepository, userId, emitterId, mockEmitter);

        // When
        SseMessage sseMessage = SseTestHelper.commentMessage(userId, "테스터", 100L);
        sseAdapter.send(sseMessage);

        // Then
        verify(notificationUtilPort).SseEligibleForNotification(userId, NotificationType.COMMENT);
        verify(userQueryUseCase).findById(userId);
        verify(notificationCommandPort).save(eq(getTestUser()), eq(NotificationType.COMMENT), anyString(), anyString());
        verify(emitterRepository).findAllEmitterByUserId(userId);
    }

    @Test
    @DisplayName("SSE 알림 전송 - 사용자 없음 예외")
    void shouldThrowException_WhenUserNotFound() {
        // Given
        given(notificationUtilPort.SseEligibleForNotification(userId, NotificationType.COMMENT)).willReturn(true);
        given(userQueryUseCase.findById(userId)).willReturn(Optional.empty());

        // When & Then
        SseMessage sseMessage = SseTestHelper.defaultSseMessage();
        assertThatThrownBy(() -> sseAdapter.send(sseMessage))
                .isInstanceOf(NotificationCustomException.class)
                .hasFieldOrPropertyWithValue("notificationErrorCode", NotificationErrorCode.INVALID_USER_CONTEXT);

        verify(notificationUtilPort).SseEligibleForNotification(userId, NotificationType.COMMENT);
        verify(userQueryUseCase).findById(userId);
        verify(notificationCommandPort, never()).save(any(), any(), any(), any());
        verify(emitterRepository, never()).findAllEmitterByUserId(any());
    }

    @Test
    @DisplayName("SSE 알림 전송 - 알림 저장 실패 시 예외")
    void shouldThrowException_WhenNotificationSaveFails() {
        // Given
        given(notificationUtilPort.SseEligibleForNotification(userId, NotificationType.COMMENT)).willReturn(true);
        given(userQueryUseCase.findById(userId)).willReturn(Optional.of(getTestUser()));
        doThrow(new RuntimeException("DB 저장 실패"))
                .when(notificationCommandPort).save(any(), any(), any(), any());

        // When & Then
        SseMessage sseMessage = SseTestHelper.defaultSseMessage();
        assertThatThrownBy(() -> sseAdapter.send(sseMessage))
                .isInstanceOf(NotificationCustomException.class)
                .hasFieldOrPropertyWithValue("notificationErrorCode", NotificationErrorCode.NOTIFICATION_SEND_ERROR);

        verify(notificationUtilPort).SseEligibleForNotification(userId, NotificationType.COMMENT);
        verify(userQueryUseCase).findById(userId);
        verify(notificationCommandPort).save(eq(getTestUser()), eq(NotificationType.COMMENT), anyString(), anyString());
        verify(emitterRepository, never()).findAllEmitterByUserId(any());
    }

    @Test
    @DisplayName("SSE 알림 전송 - Emitter가 없는 경우")
    void shouldHandleEmptyEmitters_WhenNoEmittersExist() {
        // Given
        given(notificationUtilPort.SseEligibleForNotification(userId, NotificationType.COMMENT)).willReturn(true);
        given(userQueryUseCase.findById(userId)).willReturn(Optional.of(getTestUser()));
        SseTestHelper.setupEmptyRepository(emitterRepository, userId);

        // When
        SseMessage sseMessage = SseTestHelper.defaultSseMessage();
        sseAdapter.send(sseMessage);

        // Then
        verify(notificationUtilPort).SseEligibleForNotification(userId, NotificationType.COMMENT);
        verify(userQueryUseCase).findById(userId);
        verify(notificationCommandPort).save(eq(getTestUser()), eq(NotificationType.COMMENT), anyString(), anyString());
        verify(emitterRepository).findAllEmitterByUserId(userId);
        // Emitter가 없어도 예외가 발생하지 않아야 함
    }

    @Test
    @DisplayName("사용자 SSE 연결 정리 - 성공")
    void shouldDeleteAllEmitterByUserId_WhenValidUserId() {
        // When
        sseAdapter.deleteAllEmitterByUserId(userId);

        // Then
        SseTestHelper.verifyDeleteAllEmitters(emitterRepository, userId);
    }

    @Test
    @DisplayName("여러 Emitter에 동시 전송 테스트")
    void shouldSendToMultipleEmitters() {
        // Given
        given(notificationUtilPort.SseEligibleForNotification(userId, NotificationType.COMMENT)).willReturn(true);
        given(userQueryUseCase.findById(userId)).willReturn(Optional.of(getTestUser()));
        
        Map<String, SseEmitter> emitters = SseTestHelper.createMultiDeviceEmitters(userId, 2);
        SseTestHelper.setupEmitterRepository(emitterRepository, userId, emitters);

        // When
        SseMessage sseMessage = SseTestHelper.defaultSseMessage();
        sseAdapter.send(sseMessage);

        // Then
        verify(notificationUtilPort).SseEligibleForNotification(userId, NotificationType.COMMENT);
        verify(userQueryUseCase).findById(userId);
        verify(notificationCommandPort).save(eq(getTestUser()), eq(NotificationType.COMMENT), anyString(), anyString());
        verify(emitterRepository).findAllEmitterByUserId(userId);
    }

    @Test
    @DisplayName("SSE 알림 전송 - 알림 설정 비활성화된 경우 조기 리턴")
    void shouldNotSend_WhenNotificationDisabled() {
        // Given
        given(notificationUtilPort.SseEligibleForNotification(userId, NotificationType.COMMENT)).willReturn(false);

        // When
        SseMessage sseMessage = SseTestHelper.defaultSseMessage();
        sseAdapter.send(sseMessage);

        // Then: 알림 설정 확인만 하고 나머지는 호출되지 않아야 함
        verify(notificationUtilPort).SseEligibleForNotification(userId, NotificationType.COMMENT);
        verify(userQueryUseCase, never()).findById(any());
        verify(notificationCommandPort, never()).save(any(), any(), any(), any());
        verify(emitterRepository, never()).findAllEmitterByUserId(any());
    }
}