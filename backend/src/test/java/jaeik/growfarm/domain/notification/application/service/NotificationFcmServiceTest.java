package jaeik.growfarm.domain.notification.application.service;

import jaeik.growfarm.domain.notification.application.port.out.FcmPort;
import jaeik.growfarm.domain.notification.application.port.out.LoadUserPort;
import jaeik.growfarm.domain.notification.entity.FcmToken;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.notification.entity.FcmMessage;
import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>NotificationFcmService 테스트</h2>
 * <p>FCM 알림 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 *
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationFcmService 테스트")
class NotificationFcmServiceTest {

    @Mock
    private FcmPort fcmPort;

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private User user;

    @InjectMocks
    private NotificationFcmService notificationFcmService;

    @Test
    @DisplayName("FCM 토큰 등록 - 성공")
    void shouldRegisterFcmToken_WhenValidInput() {
        // Given
        Long userId = 1L;
        String fcmToken = "valid-fcm-token";
        
        given(loadUserPort.findById(userId)).willReturn(Optional.of(user));

        // When
        notificationFcmService.registerFcmToken(userId, fcmToken);

        // Then
        verify(loadUserPort, times(1)).findById(userId);
        verify(fcmPort, times(1)).save(any(FcmToken.class));
        verifyNoMoreInteractions(loadUserPort, fcmPort);
    }

    @Test
    @DisplayName("FCM 토큰 등록 - 사용자 없음 예외")
    void shouldThrowException_WhenUserNotFound() {
        // Given
        Long userId = 999L;
        String fcmToken = "valid-fcm-token";
        
        given(loadUserPort.findById(userId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> notificationFcmService.registerFcmToken(userId, fcmToken))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

        verify(loadUserPort, times(1)).findById(userId);
        verify(fcmPort, never()).save(any());
    }

    @Test
    @DisplayName("FCM 토큰 등록 - null 토큰인 경우")
    void shouldNotRegister_WhenFcmTokenIsNull() {
        // Given
        Long userId = 1L;
        String fcmToken = null;

        // When
        notificationFcmService.registerFcmToken(userId, fcmToken);

        // Then
        verify(loadUserPort, never()).findById(anyLong());
        verify(fcmPort, never()).save(any());
    }

    @Test
    @DisplayName("FCM 토큰 등록 - 빈 토큰인 경우")
    void shouldNotRegister_WhenFcmTokenIsEmpty() {
        // Given
        Long userId = 1L;
        String fcmToken = "";

        // When
        notificationFcmService.registerFcmToken(userId, fcmToken);

        // Then
        verify(loadUserPort, never()).findById(anyLong());
        verify(fcmPort, never()).save(any());
    }

    @Test
    @DisplayName("FCM 토큰 삭제 - 성공")
    void shouldDeleteFcmTokens_WhenValidUserId() {
        // Given
        Long userId = 1L;

        // When
        notificationFcmService.deleteFcmTokens(userId);

        // Then
        verify(fcmPort, times(1)).deleteByUserId(userId);
        verifyNoMoreInteractions(fcmPort);
    }

    @Test
    @DisplayName("댓글 알림 FCM 전송 - 성공")
    void shouldSendCommentNotification_WhenValidTokens() throws IOException {
        // Given
        Long postUserId = 1L;
        String commenterName = "테스트사용자";
        
        List<FcmToken> fcmTokens = Arrays.asList(
                createMockFcmToken("token1"),
                createMockFcmToken("token2")
        );
        
        given(fcmPort.findValidFcmTokensForCommentNotification(postUserId)).willReturn(fcmTokens);

        // When
        notificationFcmService.sendCommentNotification(postUserId, commenterName);

        // Then
        verify(fcmPort, times(1)).findValidFcmTokensForCommentNotification(postUserId);
        verify(fcmPort, times(2)).sendMessageTo(any(FcmMessage.class));
        verifyNoMoreInteractions(fcmPort);
    }

    @Test
    @DisplayName("댓글 알림 FCM 전송 - 토큰 없음")
    void shouldNotSendCommentNotification_WhenNoTokens() throws IOException {
        // Given
        Long postUserId = 1L;
        String commenterName = "테스트사용자";
        
        given(fcmPort.findValidFcmTokensForCommentNotification(postUserId)).willReturn(Collections.emptyList());

        // When
        notificationFcmService.sendCommentNotification(postUserId, commenterName);

        // Then
        verify(fcmPort, times(1)).findValidFcmTokensForCommentNotification(postUserId);
        verify(fcmPort, never()).sendMessageTo(any());
        verifyNoMoreInteractions(fcmPort);
    }

    @Test
    @DisplayName("댓글 알림 FCM 전송 - 예외 발생 시 로그만 출력")
    void shouldLogError_WhenCommentNotificationFails() {
        // Given
        Long postUserId = 1L;
        String commenterName = "테스트사용자";
        
        given(fcmPort.findValidFcmTokensForCommentNotification(postUserId))
                .willThrow(new RuntimeException("FCM 서비스 오류"));

        // When
        notificationFcmService.sendCommentNotification(postUserId, commenterName);

        // Then
        verify(fcmPort, times(1)).findValidFcmTokensForCommentNotification(postUserId);
        // 예외가 발생해도 서비스는 정상적으로 완료되어야 함
    }

    @Test
    @DisplayName("롤링페이퍼 메시지 알림 FCM 전송 - 성공")
    void shouldSendPaperPlantNotification_WhenValidTokens() throws IOException {
        // Given
        Long farmOwnerId = 1L;
        
        List<FcmToken> fcmTokens = Arrays.asList(
                createMockFcmToken("token1"),
                createMockFcmToken("token2")
        );
        
        given(fcmPort.findValidFcmTokensForMessageNotification(farmOwnerId)).willReturn(fcmTokens);

        // When
        notificationFcmService.sendPaperPlantNotification(farmOwnerId);

        // Then
        verify(fcmPort, times(1)).findValidFcmTokensForMessageNotification(farmOwnerId);
        verify(fcmPort, times(2)).sendMessageTo(any(FcmMessage.class));
        verifyNoMoreInteractions(fcmPort);
    }

    @Test
    @DisplayName("롤링페이퍼 메시지 알림 FCM 전송 - 토큰 없음")
    void shouldNotSendPaperPlantNotification_WhenNoTokens() throws IOException {
        // Given
        Long farmOwnerId = 1L;
        
        given(fcmPort.findValidFcmTokensForMessageNotification(farmOwnerId)).willReturn(Collections.emptyList());

        // When
        notificationFcmService.sendPaperPlantNotification(farmOwnerId);

        // Then
        verify(fcmPort, times(1)).findValidFcmTokensForMessageNotification(farmOwnerId);
        verify(fcmPort, never()).sendMessageTo(any());
        verifyNoMoreInteractions(fcmPort);
    }

    @Test
    @DisplayName("인기글 등극 알림 FCM 전송 - 성공")
    void shouldSendPostFeaturedNotification_WhenValidTokens() throws IOException {
        // Given
        Long userId = 1L;
        String title = "축하합니다!";
        String body = "게시글이 인기글로 선정되었습니다.";
        
        List<FcmToken> fcmTokens = List.of(
                createMockFcmToken("token1")
        );
        
        given(fcmPort.findValidFcmTokensForPostFeaturedNotification(userId)).willReturn(fcmTokens);

        // When
        notificationFcmService.sendPostFeaturedNotification(userId, title, body);

        // Then
        verify(fcmPort, times(1)).findValidFcmTokensForPostFeaturedNotification(userId);
        verify(fcmPort, times(1)).sendMessageTo(any(FcmMessage.class));
        verifyNoMoreInteractions(fcmPort);
    }

    @Test
    @DisplayName("인기글 등극 알림 FCM 전송 - 토큰 없음")
    void shouldNotSendPostFeaturedNotification_WhenNoTokens() throws IOException {
        // Given
        Long userId = 1L;
        String title = "축하합니다!";
        String body = "게시글이 인기글로 선정되었습니다.";
        
        given(fcmPort.findValidFcmTokensForPostFeaturedNotification(userId)).willReturn(Collections.emptyList());

        // When
        notificationFcmService.sendPostFeaturedNotification(userId, title, body);

        // Then
        verify(fcmPort, times(1)).findValidFcmTokensForPostFeaturedNotification(userId);
        verify(fcmPort, never()).sendMessageTo(any());
        verifyNoMoreInteractions(fcmPort);
    }

    @Test
    @DisplayName("인기글 등극 알림 FCM 전송 - 예외 발생 시 로그만 출력")
    void shouldLogError_WhenPostFeaturedNotificationFails() {
        // Given
        Long userId = 1L;
        String title = "축하합니다!";
        String body = "게시글이 인기글로 선정되었습니다.";
        
        given(fcmPort.findValidFcmTokensForPostFeaturedNotification(userId))
                .willThrow(new RuntimeException("FCM 서비스 오류"));

        // When
        notificationFcmService.sendPostFeaturedNotification(userId, title, body);

        // Then
        verify(fcmPort, times(1)).findValidFcmTokensForPostFeaturedNotification(userId);
        // 예외가 발생해도 서비스는 정상적으로 완료되어야 함
    }

    @Test
    @DisplayName("FCM 토큰 등록 - null 사용자 ID")
    void shouldThrowException_WhenUserIdIsNull() {
        // Given
        Long userId = null;
        String fcmToken = "valid-fcm-token";
        given(loadUserPort.findById(userId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> notificationFcmService.registerFcmToken(userId, fcmToken))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");

        verify(loadUserPort, times(1)).findById(userId);
        verify(fcmPort, never()).save(any());
    }

    @Test
    @DisplayName("FCM 토큰 등록 - 음수 사용자 ID")
    void shouldThrowException_WhenUserIdIsNegative() {
        // Given
        Long userId = -1L;
        String fcmToken = "valid-fcm-token";
        
        given(loadUserPort.findById(userId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> notificationFcmService.registerFcmToken(userId, fcmToken))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

        verify(loadUserPort, times(1)).findById(userId);
        verify(fcmPort, never()).save(any());
    }

    @Test
    @DisplayName("FCM 토큰 등록 - 매우 긴 토큰")
    void shouldRegisterFcmToken_WhenVeryLongToken() {
        // Given
        Long userId = 1L;
        String veryLongToken = "A".repeat(1000); // 매우 긴 토큰
        
        given(loadUserPort.findById(userId)).willReturn(Optional.of(user));

        // When
        notificationFcmService.registerFcmToken(userId, veryLongToken);

        // Then
        verify(loadUserPort, times(1)).findById(userId);
        verify(fcmPort, times(1)).save(any(FcmToken.class));
        verifyNoMoreInteractions(loadUserPort, fcmPort);
    }

    @Test
    @DisplayName("FCM 토큰 삭제 - null 사용자 ID")
    void shouldDeleteFcmTokens_WhenUserIdIsNull() {
        // Given
        Long userId = null;

        // When
        notificationFcmService.deleteFcmTokens(userId);

        // Then
        verify(fcmPort, times(1)).deleteByUserId(userId);
        verifyNoMoreInteractions(fcmPort);
    }

    @Test
    @DisplayName("댓글 알림 FCM 전송 - null 댓글자 이름")
    void shouldSendCommentNotification_WhenCommenterNameIsNull() throws IOException {
        // Given
        Long postUserId = 1L;
        String commenterName = null;
        
        List<FcmToken> fcmTokens = List.of(
                createMockFcmToken("token1")
        );
        
        given(fcmPort.findValidFcmTokensForCommentNotification(postUserId)).willReturn(fcmTokens);

        // When
        notificationFcmService.sendCommentNotification(postUserId, commenterName);

        // Then
        verify(fcmPort, times(1)).findValidFcmTokensForCommentNotification(postUserId);
        verify(fcmPort, times(1)).sendMessageTo(any(FcmMessage.class));
        verifyNoMoreInteractions(fcmPort);
    }

    @Test
    @DisplayName("롤링페이퍼 메시지 알림 FCM 전송 - null 농장 주인 ID")
    void shouldNotSendPaperPlantNotification_WhenFarmOwnerIdIsNull() throws IOException {
        // Given
        Long farmOwnerId = null;
        
        given(fcmPort.findValidFcmTokensForMessageNotification(farmOwnerId)).willReturn(Collections.emptyList());

        // When
        notificationFcmService.sendPaperPlantNotification(farmOwnerId);

        // Then
        verify(fcmPort, times(1)).findValidFcmTokensForMessageNotification(farmOwnerId);
        verify(fcmPort, never()).sendMessageTo(any());
        verifyNoMoreInteractions(fcmPort);
    }

    @Test
    @DisplayName("인기글 등극 알림 FCM 전송 - null 제목과 본문")
    void shouldSendPostFeaturedNotification_WhenTitleAndBodyAreNull() throws IOException {
        // Given
        Long userId = 1L;
        String title = null;
        String body = null;
        
        List<FcmToken> fcmTokens = List.of(
                createMockFcmToken("token1")
        );
        
        given(fcmPort.findValidFcmTokensForPostFeaturedNotification(userId)).willReturn(fcmTokens);

        // When
        notificationFcmService.sendPostFeaturedNotification(userId, title, body);

        // Then
        verify(fcmPort, times(1)).findValidFcmTokensForPostFeaturedNotification(userId);
        verify(fcmPort, times(1)).sendMessageTo(any(FcmMessage.class));
        verifyNoMoreInteractions(fcmPort);
    }

    private FcmToken createMockFcmToken(String token) {
        FcmToken fcmToken = mock(FcmToken.class);
        given(fcmToken.getFcmRegistrationToken()).willReturn(token);
        return fcmToken;
    }
}