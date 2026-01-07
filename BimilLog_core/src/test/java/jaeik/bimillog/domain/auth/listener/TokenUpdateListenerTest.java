package jaeik.bimillog.domain.auth.listener;

import jaeik.bimillog.domain.auth.service.AuthTokenService;
import jaeik.bimillog.domain.member.event.MemberTokenUpdateEvent;
import jaeik.bimillog.testutil.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * <h2>TokenUpdateListener 단위 테스트</h2>
 * <p>JWT 리프레시 토큰 업데이트 이벤트 리스너의 동작을 검증하는 단위 테스트</p>
 *
 * @version 2.4.0
 */
@DisplayName("TokenUpdateListener 단위 테스트")
@Tag("unit")
class TokenUpdateListenerTest extends BaseUnitTest {

    private static final Long TOKEN_ID = 99L;
    private static final String NEW_REFRESH_TOKEN = "new-refresh-token";

    @Mock
    private AuthTokenService authTokenService;

    @InjectMocks
    private TokenUpdateListener tokenUpdateListener;

    @Test
    @DisplayName("JWT 리프레시 토큰 업데이트 - 이벤트 처리 성공")
    void shouldUpdateJwtRefreshToken_WhenEventReceived() {
        // Given
        MemberTokenUpdateEvent event = new MemberTokenUpdateEvent(TOKEN_ID, NEW_REFRESH_TOKEN);

        // When
        tokenUpdateListener.updateJwtRefreshToken(event);

        // Then
        verify(authTokenService, times(1)).updateJwtRefreshToken(TOKEN_ID, NEW_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("JWT 리프레시 토큰 업데이트 - 토큰 ID와 토큰 값 전달 검증")
    void shouldPassCorrectParameters_WhenUpdatingToken() {
        // Given
        Long tokenId = 123L;
        String refreshToken = "test-refresh-token";
        MemberTokenUpdateEvent event = new MemberTokenUpdateEvent(tokenId, refreshToken);

        // When
        tokenUpdateListener.updateJwtRefreshToken(event);

        // Then
        verify(authTokenService, times(1)).updateJwtRefreshToken(tokenId, refreshToken);
    }
}
