package jaeik.bimillog.infrastructure.outadapter.notification.fcm;

import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.notification.entity.FcmToken;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.notification.entity.NotificationVO;
import jaeik.bimillog.domain.notification.exception.NotificationCustomException;
import jaeik.bimillog.domain.notification.exception.NotificationErrorCode;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.notification.out.fcm.FcmAdapter;
import jaeik.bimillog.infrastructure.adapter.notification.out.persistence.notification.FcmTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

/**
 * <h2>FcmAdapter 테스트</h2>
 * <p>FCM 어댑터의 외부 API 연동 및 토큰 관리 동작 검증</p>
 * <p>Firebase Cloud Messaging 기능과 FCM 토큰 CRUD 작업 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class FcmAdapterTest {

    @Mock
    private FcmTokenRepository fcmTokenRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private FcmAdapter fcmAdapter;

    private User testUser;
    private FcmToken testFcmToken;
    private NotificationVO testEvent;

    @BeforeEach
    void setUp() {
        // Given: 테스트용 사용자 설정
        Setting testSetting = Setting.builder()
                .messageNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(true)
                .build();

        testUser = User.builder()
                .id(1L)
                .socialId("12345")
                .provider(SocialProvider.KAKAO)
                .userName("테스트유저")
                .role(UserRole.USER)
                .setting(testSetting)
                .build();

        testFcmToken = FcmToken.create(testUser, "test-fcm-token");
                

        testEvent = NotificationVO.create(
                NotificationType.COMMENT,
                "테스트 알림 메시지",
                "/test/url"
        );
    }

    @Test
    @DisplayName("정상 케이스 - FCM 토큰 저장")
    void shouldSaveFcmToken_WhenValidTokenProvided() {
        // Given: 저장할 FCM 토큰과 예상 결과
        FcmToken savedToken = FcmToken.builder()
                .id(1L)
                .user(testUser)
                .fcmRegistrationToken("new-fcm-token")
                .build();

        given(fcmTokenRepository.save(any(FcmToken.class))).willReturn(savedToken);

        // When: FCM 토큰 저장
        FcmToken result = fcmAdapter.save(testFcmToken);

        // Then: 저장 결과 검증
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getFcmRegistrationToken()).isEqualTo("new-fcm-token");
        assertThat(result.getUser()).isEqualTo(testUser);

        verify(fcmTokenRepository).save(any(FcmToken.class));
    }

    @Test
    @DisplayName("정상 케이스 - 메시지 알림용 유효한 FCM 토큰 조회")
    void shouldFindValidFcmTokensForMessageNotification_WhenUserExists() {
        // Given: 메시지 알림용 FCM 토큰 목록
        List<FcmToken> expectedTokens = Arrays.asList(testFcmToken);
        given(fcmTokenRepository.findValidFcmTokensForMessageNotification(1L))
                .willReturn(expectedTokens);

        // When: 메시지 알림용 FCM 토큰 조회
        List<FcmToken> result = fcmAdapter.findValidFcmTokensForMessageNotification(1L);

        // Then: 조회 결과 검증
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testFcmToken);

        verify(fcmTokenRepository).findValidFcmTokensForMessageNotification(1L);
    }

    @Test
    @DisplayName("정상 케이스 - 댓글 알림용 유효한 FCM 토큰 조회")
    void shouldFindValidFcmTokensForCommentNotification_WhenUserExists() {
        // Given: 댓글 알림용 FCM 토큰 목록
        List<FcmToken> expectedTokens = Arrays.asList(testFcmToken);
        given(fcmTokenRepository.findValidFcmTokensForCommentNotification(1L))
                .willReturn(expectedTokens);

        // When: 댓글 알림용 FCM 토큰 조회
        List<FcmToken> result = fcmAdapter.findValidFcmTokensForCommentNotification(1L);

        // Then: 조회 결과 검증
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testFcmToken);

        verify(fcmTokenRepository).findValidFcmTokensForCommentNotification(1L);
    }

    @Test
    @DisplayName("정상 케이스 - 인기글 알림용 유효한 FCM 토큰 조회")
    void shouldFindValidFcmTokensForPostFeaturedNotification_WhenUserExists() {
        // Given: 인기글 알림용 FCM 토큰 목록
        List<FcmToken> expectedTokens = Arrays.asList(testFcmToken);
        given(fcmTokenRepository.findValidFcmTokensForPostFeaturedNotification(1L))
                .willReturn(expectedTokens);

        // When: 인기글 알림용 FCM 토큰 조회
        List<FcmToken> result = fcmAdapter.findValidFcmTokensForPostFeaturedNotification(1L);

        // Then: 조회 결과 검증
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testFcmToken);

        verify(fcmTokenRepository).findValidFcmTokensForPostFeaturedNotification(1L);
    }

    @Test
    @DisplayName("정상 케이스 - 사용자 ID로 FCM 토큰 삭제")
    void shouldDeleteFcmTokensByUserId_WhenUserIdProvided() {
        // Given: 삭제할 사용자 ID
        Long userId = 1L;
        doNothing().when(fcmTokenRepository).deleteByUser_Id(userId);

        // When: 사용자 ID로 FCM 토큰 삭제
        fcmAdapter.deleteByUserId(userId);

        // Then: 삭제 동작 검증
        verify(fcmTokenRepository).deleteByUser_Id(userId);
    }

    @Test
    @DisplayName("경계 케이스 - 빈 FCM 토큰 목록 조회")
    void shouldReturnEmptyList_WhenNoValidFcmTokensFound() {
        // Given: 빈 토큰 목록
        given(fcmTokenRepository.findValidFcmTokensForMessageNotification(1L))
                .willReturn(Collections.emptyList());

        // When: 메시지 알림용 FCM 토큰 조회
        List<FcmToken> result = fcmAdapter.findValidFcmTokensForMessageNotification(1L);

        // Then: 빈 목록 반환 검증
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(fcmTokenRepository).findValidFcmTokensForMessageNotification(1L);
    }

    @Test
    @DisplayName("정상 케이스 - FCM 알림 전송 (토큰이 없는 경우 조기 리턴)")
    void shouldReturnEarly_WhenNoFcmTokensAvailable() {
        // Given: 빈 FCM 토큰 목록
        given(fcmTokenRepository.findValidFcmTokensForMessageNotification(1L))
                .willReturn(Collections.emptyList());

        // When: FCM 알림 전송 시도
        fcmAdapter.send(1L, testEvent);

        // Then: FCM 토큰 조회만 수행되고 실제 전송은 수행되지 않음
        verify(fcmTokenRepository).findValidFcmTokensForMessageNotification(1L);
        // RestTemplate 호출이 없어야 함 (조기 리턴으로 인해)
    }

    @Test
    @DisplayName("예외 케이스 - FCM 알림 전송 중 예외 발생")
    void shouldThrowCustomException_WhenFcmSendFails() {
        // TODO: 테스트 실패 - 메인 로직 Firebase 연동 검증 필요
        // 기존: 단순 예외 발생만 확인
        // 수정: 실제 Firebase FCM API 호출 실패에 대한 적절한 예외 처리 검증
        
        // Given: FCM 토큰은 존재하지만 Firebase API 호출 실패 상황
        List<FcmToken> tokens = Arrays.asList(testFcmToken);
        given(fcmTokenRepository.findValidFcmTokensForMessageNotification(1L))
                .willReturn(tokens);

        // Firebase Admin SDK가 초기화되지 않은 상황에서는 예외 발생
        // When & Then: FCM 전송 중 Firebase 연결 실패 시 적절한 예외 발생 검증
        assertThatThrownBy(() -> fcmAdapter.send(1L, testEvent))
                .isInstanceOf(NotificationCustomException.class)
                .hasFieldOrPropertyWithValue("notificationErrorCode", NotificationErrorCode.FCM_SEND_ERROR);

        verify(fcmTokenRepository).findValidFcmTokensForMessageNotification(1L);
        // 실제 구현에서는 Firebase Admin SDK 초기화 상태도 확인해야 함
    }

    @Test
    @DisplayName("경계 케이스 - null FCM 토큰 목록 처리")
    void shouldReturnEarly_WhenFcmTokensIsNull() {
        // Given: null FCM 토큰 목록
        given(fcmTokenRepository.findValidFcmTokensForMessageNotification(1L))
                .willReturn(null);

        // When: FCM 알림 전송 시도
        fcmAdapter.send(1L, testEvent);

        // Then: 조기 리턴으로 실제 전송 수행되지 않음
        verify(fcmTokenRepository).findValidFcmTokensForMessageNotification(1L);
        // RestTemplate 호출이 없어야 함
    }
}