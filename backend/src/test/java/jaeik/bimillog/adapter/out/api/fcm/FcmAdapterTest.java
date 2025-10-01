package jaeik.bimillog.adapter.out.api.fcm;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import jaeik.bimillog.domain.notification.entity.FcmMessage;
import jaeik.bimillog.domain.notification.entity.FcmToken;
import jaeik.bimillog.infrastructure.adapter.out.api.fcm.FcmAdapter;
import jaeik.bimillog.infrastructure.adapter.out.api.fcm.FcmApiClient;
import jaeik.bimillog.infrastructure.adapter.out.notification.FcmTokenRepository;
import jaeik.bimillog.testutil.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
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
@Tag("test")
class FcmAdapterTest extends BaseUnitTest {

    @Mock
    private FcmTokenRepository fcmTokenRepository;
    
    @Mock
    private FcmApiClient fcmApiClient;

    @InjectMocks
    private FcmAdapter fcmAdapter;

    private FcmToken testFcmToken;

    @BeforeEach
    void setUp() {
        // Given: 테스트용 FCM 토큰 설정
        testFcmToken = FcmToken.create(getTestMember(), "test-fcm-TemporaryToken");
    }

    @Test
    @DisplayName("정상 케이스 - FCM 토큰 저장")
    void shouldSaveFcmToken_WhenValidTokenProvided() {
        // Given: 저장할 FCM 토큰과 예상 결과
        FcmToken savedToken = FcmToken.builder()
                .id(1L)
                .member(getTestMember())
                .fcmRegistrationToken("new-fcm-TemporaryToken")
                .build();

        given(fcmTokenRepository.save(any(FcmToken.class))).willReturn(savedToken);

        // When: FCM 토큰 저장
        FcmToken result = fcmAdapter.save(testFcmToken);

        // Then: 저장 결과 검증
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getFcmRegistrationToken()).isEqualTo("new-fcm-TemporaryToken");
        assertThat(result.getMember()).isEqualTo(getTestMember());

        verify(fcmTokenRepository).save(any(FcmToken.class));
    }

    @Test
    @DisplayName("정상 케이스 - 사용자 ID로 FCM 토큰 삭제")
    void shouldDeleteFcmTokensByMemberId_WhenMemberIdProvided() {
        // Given: 삭제할 사용자 ID
        Long memberId = 1L;
        doNothing().when(fcmTokenRepository).deleteByMember_Id(memberId);

        // When: 사용자 ID로 FCM 토큰 삭제
        fcmAdapter.deleteFcmTokens(memberId, null);

        // Then: 삭제 동작 검증
        verify(fcmTokenRepository).deleteByMember_Id(memberId);
    }

    @Test
    @DisplayName("정상 케이스 - FCM 메시지 전송 성공")
    void shouldSendMessageTo_WhenValidFcmMessage() throws IOException {
        // Given: FCM 메시지 설정
        FcmMessage fcmMessage = FcmMessage.of("test-fcm-TemporaryToken", "테스트 제목", "테스트 내용");

        // fcmApiClient가 정상적으로 동작하도록 모킹
        doNothing().when(fcmApiClient).sendMessage(anyString(), anyString(), any());

        try (MockedStatic<GoogleCredentials> mockedCredentials = Mockito.mockStatic(GoogleCredentials.class)) {
            GoogleCredentials credentials = Mockito.mock(GoogleCredentials.class);
            AccessToken accessToken = new AccessToken("mock-access-token", new Date(System.currentTimeMillis() + 60_000));

            mockedCredentials.when(() -> GoogleCredentials.fromStream(any(InputStream.class))).thenReturn(credentials);
            given(credentials.createScoped(anyList())).willReturn(credentials);
            doNothing().when(credentials).refreshIfExpired();
            given(credentials.getAccessToken()).willReturn(accessToken);

            // When
            fcmAdapter.sendMessageTo(fcmMessage);

            // Then: FCM API 클라이언트가 호출되었는지 검증
            ArgumentCaptor<String> authHeaderCaptor = ArgumentCaptor.forClass(String.class);
            verify(fcmApiClient).sendMessage(authHeaderCaptor.capture(), eq(MediaType.APPLICATION_JSON_VALUE), any());
            assertThat(authHeaderCaptor.getValue()).isEqualTo("Bearer mock-access-token");
        }
    }

    @Test
    @DisplayName("예외 케이스 - FCM 메시지 전송 시 null 메시지")
    void shouldThrowException_WhenFcmMessageIsNull() {
        // Given: null FCM 메시지
        FcmMessage fcmMessage = null;

        // When & Then: NullPointerException 발생 예상
        assertThatThrownBy(() -> fcmAdapter.sendMessageTo(fcmMessage))
                .isInstanceOf(NullPointerException.class);
    }
}