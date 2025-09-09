package jaeik.bimillog.infrastructure.outadapter.notification.fcm;

import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.notification.entity.FcmToken;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.notification.out.fcm.FcmAdapter;
import jaeik.bimillog.infrastructure.adapter.notification.out.jpa.FcmTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
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

    @InjectMocks
    private FcmAdapter fcmAdapter;

    private User testUser;
    private FcmToken testFcmToken;

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




}