package jaeik.bimillog.adapter.out.api.fcm;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import jaeik.bimillog.domain.notification.entity.FcmMessage;
import jaeik.bimillog.infrastructure.out.api.fcm.FcmAdapter;
import jaeik.bimillog.infrastructure.out.api.fcm.FcmApiClient;
import jaeik.bimillog.domain.notification.out.FcmTokenRepository;
import jaeik.bimillog.testutil.BaseUnitTest;
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
 * <h2>FcmAdapter 단위 테스트</h2>
 * <p>FCM API 호출과 토큰 삭제 위임이 올바르게 수행되는지 검증합니다.</p>
 */
@Tag("unit")
class FcmAdapterTest extends BaseUnitTest {

    @Mock private FcmTokenRepository fcmTokenRepository;
    @Mock private FcmApiClient fcmApiClient;

    @InjectMocks private FcmAdapter fcmAdapter;

    @Test
    @DisplayName("FCM 토큰 삭제 - 특정 토큰 지정")
    void shouldDeleteSpecificToken() {
        fcmAdapter.deleteFcmTokens(1L, 10L);
        verify(fcmTokenRepository).deleteByMember_IdAndId(1L, 10L);
    }

    @Test
    @DisplayName("FCM 토큰 삭제 - 전체 토큰 삭제")
    void shouldDeleteAllTokensWhenTokenIdNull() {
        fcmAdapter.deleteFcmTokens(1L, null);
        verify(fcmTokenRepository).deleteByMember_Id(1L);
    }

    @Test
    @DisplayName("FCM 메시지 전송 - 성공")
    void shouldSendMessageTo() throws IOException {
        FcmMessage message = FcmMessage.of("token-1", "제목", "내용");
        doNothing().when(fcmApiClient).sendMessage(anyString(), anyString(), any());

        try (MockedStatic<GoogleCredentials> mockedCredentials = Mockito.mockStatic(GoogleCredentials.class)) {
            GoogleCredentials credentials = Mockito.mock(GoogleCredentials.class);
            AccessToken accessToken = new AccessToken("mock-access-token", new Date(System.currentTimeMillis() + 60_000));

            mockedCredentials.when(() -> GoogleCredentials.fromStream(any(InputStream.class))).thenReturn(credentials);
            given(credentials.createScoped(anyList())).willReturn(credentials);
            doNothing().when(credentials).refreshIfExpired();
            given(credentials.getAccessToken()).willReturn(accessToken);

            fcmAdapter.sendMessageTo(message);

            ArgumentCaptor<String> authHeaderCaptor = ArgumentCaptor.forClass(String.class);
            verify(fcmApiClient).sendMessage(authHeaderCaptor.capture(), Mockito.eq(MediaType.APPLICATION_JSON_VALUE), any());
            assertThat(authHeaderCaptor.getValue()).isEqualTo("Bearer mock-access-token");
        }
    }

    @Test
    @DisplayName("FCM 메시지 전송 - null 메시지 예외")
    void shouldThrowWhenMessageNull() {
        assertThatThrownBy(() -> fcmAdapter.sendMessageTo(null))
                .isInstanceOf(NullPointerException.class);
    }
}
