package jaeik.bimillog.infrastructure.api.fcm;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
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
 * <p>FCM API 호출이 올바르게 수행되는지 검증합니다.</p>
 */
@Tag("unit")
class FcmAdapterTest extends BaseUnitTest {

    @Mock private FcmApiClient fcmApiClient;

    @InjectMocks private FcmAdapter fcmAdapter;

    @Test
    @DisplayName("FCM 메시지 전송 - 성공")
    void shouldSendMessageTo() throws IOException {
        String token = "token-1";
        String title = "제목";
        String body = "내용";
        doNothing().when(fcmApiClient).sendMessage(anyString(), anyString(), any());

        try (MockedStatic<GoogleCredentials> mockedCredentials = Mockito.mockStatic(GoogleCredentials.class)) {
            GoogleCredentials credentials = Mockito.mock(GoogleCredentials.class);
            AccessToken accessToken = new AccessToken("mock-access-token", new Date(System.currentTimeMillis() + 60_000));

            mockedCredentials.when(() -> GoogleCredentials.fromStream(any(InputStream.class))).thenReturn(credentials);
            given(credentials.createScoped(anyList())).willReturn(credentials);
            doNothing().when(credentials).refreshIfExpired();
            given(credentials.getAccessToken()).willReturn(accessToken);

            fcmAdapter.sendMessageTo(token, title, body);

            ArgumentCaptor<String> authHeaderCaptor = ArgumentCaptor.forClass(String.class);
            verify(fcmApiClient).sendMessage(authHeaderCaptor.capture(), Mockito.eq(MediaType.APPLICATION_JSON_VALUE), any());
            assertThat(authHeaderCaptor.getValue()).isEqualTo("Bearer mock-access-token");
        }
    }

}
