package jaeik.bimillog.domain.notification.service;

import jaeik.bimillog.domain.global.application.port.out.GlobalUserQueryPort;
import jaeik.bimillog.domain.notification.application.port.out.FcmPort;
import jaeik.bimillog.domain.notification.application.port.out.NotificationUtilPort;
import jaeik.bimillog.domain.notification.application.service.FcmService;
import jaeik.bimillog.domain.notification.entity.FcmMessage;
import jaeik.bimillog.domain.notification.entity.FcmToken;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.notification.exception.NotificationCustomException;
import jaeik.bimillog.domain.notification.exception.NotificationErrorCode;
import jaeik.bimillog.domain.member.entity.member.Member;
import jaeik.bimillog.domain.member.entity.member.MemberRole;
import jaeik.bimillog.testutil.TestUsers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>FcmService 테스트</h2>
 * <p>FCM 알림 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 *
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FcmService 테스트")
@Tag("test")
class FcmServiceTest {

    @Mock
    private FcmPort fcmPort;

    @Mock
    private GlobalUserQueryPort globalUserQueryPort;

    @Mock
    private NotificationUtilPort notificationUtilPort;

    @InjectMocks
    private FcmService notificationFcmService;

    @Test
    @DisplayName("FCM 토큰 등록 - 성공")
    void shouldRegisterFcmToken_WhenValidInput() {
        // Given
        Member member = TestUsers.MEMBER_1;
        String fcmToken = "valid-fcm-TemporaryToken";
        Long expectedTokenId = 100L;
        FcmToken savedToken = mock(FcmToken.class);

        given(savedToken.getId()).willReturn(expectedTokenId);
        given(fcmPort.save(any(FcmToken.class))).willReturn(savedToken);

        // When
        Long actualTokenId = notificationFcmService.registerFcmToken(member, fcmToken);

        // Then
        verify(fcmPort, times(1)).save(any(FcmToken.class));
        verifyNoMoreInteractions(fcmPort);
        assertThat(actualTokenId).isEqualTo(expectedTokenId);
    }

    @Test
    @DisplayName("FCM 토큰 등록 - 사용자 null 예외")
    void shouldThrowException_WhenUserIsNull() {
        // Given
        Member member = null;
        String fcmToken = "valid-fcm-TemporaryToken";

        // When & Then
        assertThatThrownBy(() -> notificationFcmService.registerFcmToken(member, fcmToken))
                .isInstanceOf(NotificationCustomException.class)
                .hasFieldOrPropertyWithValue("notificationErrorCode", NotificationErrorCode.NOTIFICATION_USER_NOT_FOUND);

        verify(fcmPort, never()).save(any());
    }

    @Test
    @DisplayName("FCM 토큰 등록 - null 토큰인 경우")
    void shouldNotRegister_WhenFcmTokenIsNull() {
        // Given
        Member member = TestUsers.MEMBER_1;
        String fcmToken = null;

        // When
        Long result = notificationFcmService.registerFcmToken(member, fcmToken);

        // Then
        verify(fcmPort, never()).save(any());
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("FCM 토큰 등록 - 빈 토큰인 경우")
    void shouldNotRegister_WhenFcmTokenIsEmpty() {
        // Given
        Member member = TestUsers.MEMBER_2;
        String fcmToken = "";

        // When
        Long result = notificationFcmService.registerFcmToken(member, fcmToken);

        // Then
        verify(fcmPort, never()).save(any());
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("FCM 토큰 삭제 - 성공")
    void shouldDeleteFcmTokens_WhenValidUserId() {
        // Given
        Long userId = 1L;

        // When
        notificationFcmService.deleteFcmTokens(userId, null);

        // Then
        verify(fcmPort, times(1)).deleteFcmTokens(userId, null);
        verifyNoMoreInteractions(fcmPort);
    }

    @Test
    @DisplayName("댓글 알림 FCM 전송 - 성공")
    void shouldSendCommentNotification_WhenValidTokens() throws IOException {
        // Given
        Long postUserId = 1L;
        String commenterName = TestUsers.MEMBER_3.getSocialNickname();
        
        List<FcmToken> fcmTokens = Arrays.asList(
                createMockFcmToken("token1"),
                createMockFcmToken("token2")
        );
        
        given(notificationUtilPort.FcmEligibleFcmTokens(postUserId, NotificationType.COMMENT)).willReturn(fcmTokens);

        // When
        notificationFcmService.sendCommentNotification(postUserId, commenterName);

        // Then
        verify(notificationUtilPort, times(1)).FcmEligibleFcmTokens(postUserId, NotificationType.COMMENT);
        verify(fcmPort, times(2)).sendMessageTo(any(FcmMessage.class));
        verifyNoMoreInteractions(fcmPort);
    }

    @Test
    @DisplayName("댓글 알림 FCM 전송 - 토큰 없음")
    void shouldNotSendCommentNotification_WhenNoTokens() throws IOException {
        // Given
        Long postUserId = 2L;
        String commenterName = TestUsers.MEMBER_2.getUserName();
        
        given(notificationUtilPort.FcmEligibleFcmTokens(postUserId, NotificationType.COMMENT)).willReturn(Collections.emptyList());

        // When
        notificationFcmService.sendCommentNotification(postUserId, commenterName);

        // Then
        verify(notificationUtilPort, times(1)).FcmEligibleFcmTokens(postUserId, NotificationType.COMMENT);
        verify(fcmPort, never()).sendMessageTo(any());
        verifyNoMoreInteractions(fcmPort);
    }

    @Test
    @DisplayName("댓글 알림 FCM 전송 - 예외 발생 시 로그만 출력")
    void shouldLogError_WhenCommentNotificationFails() {
        // Given
        Long postUserId = 1L;
        String commenterName = TestUsers.withRole(MemberRole.ADMIN).getUserName();
        
        given(notificationUtilPort.FcmEligibleFcmTokens(postUserId, NotificationType.COMMENT))
                .willThrow(new RuntimeException("FCM 서비스 오류"));

        // When
        notificationFcmService.sendCommentNotification(postUserId, commenterName);

        // Then
        verify(notificationUtilPort, times(1)).FcmEligibleFcmTokens(postUserId, NotificationType.COMMENT);
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
        
        given(notificationUtilPort.FcmEligibleFcmTokens(farmOwnerId, NotificationType.PAPER)).willReturn(fcmTokens);

        // When
        notificationFcmService.sendPaperPlantNotification(farmOwnerId);

        // Then
        verify(notificationUtilPort, times(1)).FcmEligibleFcmTokens(farmOwnerId, NotificationType.PAPER);
        verify(fcmPort, times(2)).sendMessageTo(any(FcmMessage.class));
        verifyNoMoreInteractions(fcmPort);
    }

    @Test
    @DisplayName("롤링페이퍼 메시지 알림 FCM 전송 - 토큰 없음")
    void shouldNotSendPaperPlantNotification_WhenNoTokens() throws IOException {
        // Given
        Long farmOwnerId = 1L;
        
        given(notificationUtilPort.FcmEligibleFcmTokens(farmOwnerId, NotificationType.PAPER)).willReturn(Collections.emptyList());

        // When
        notificationFcmService.sendPaperPlantNotification(farmOwnerId);

        // Then
        verify(notificationUtilPort, times(1)).FcmEligibleFcmTokens(farmOwnerId, NotificationType.PAPER);
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
        
        given(notificationUtilPort.FcmEligibleFcmTokens(userId, NotificationType.POST_FEATURED)).willReturn(fcmTokens);

        // When
        notificationFcmService.sendPostFeaturedNotification(userId, title, body);

        // Then
        verify(notificationUtilPort, times(1)).FcmEligibleFcmTokens(userId, NotificationType.POST_FEATURED);
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
        
        given(notificationUtilPort.FcmEligibleFcmTokens(userId, NotificationType.POST_FEATURED)).willReturn(Collections.emptyList());

        // When
        notificationFcmService.sendPostFeaturedNotification(userId, title, body);

        // Then
        verify(notificationUtilPort, times(1)).FcmEligibleFcmTokens(userId, NotificationType.POST_FEATURED);
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
        
        given(notificationUtilPort.FcmEligibleFcmTokens(userId, NotificationType.POST_FEATURED))
                .willThrow(new RuntimeException("FCM 서비스 오류"));

        // When
        notificationFcmService.sendPostFeaturedNotification(userId, title, body);

        // Then
        verify(notificationUtilPort, times(1)).FcmEligibleFcmTokens(userId, NotificationType.POST_FEATURED);
        // 예외가 발생해도 서비스는 정상적으로 완료되어야 함
    }


    private FcmToken createMockFcmToken(String token) {
        FcmToken fcmToken = mock(FcmToken.class);
        given(fcmToken.getFcmRegistrationToken()).willReturn(token);
        return fcmToken;
    }
}