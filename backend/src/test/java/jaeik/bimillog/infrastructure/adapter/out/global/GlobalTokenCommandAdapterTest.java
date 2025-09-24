package jaeik.bimillog.infrastructure.adapter.out.global;

import jaeik.bimillog.domain.auth.entity.Token;
import jaeik.bimillog.infrastructure.adapter.out.auth.jpa.TokenRepository;
import jaeik.bimillog.testutil.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * <h2>GlobalTokenCommandAdapter 단위 테스트</h2>
 * <p>토큰 명령 공용 어댑터의 동작을 검증하는 단위 테스트입니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("GlobalTokenCommandAdapter 단위 테스트")
class GlobalTokenCommandAdapterTest extends BaseUnitTest {

    @Mock
    private TokenRepository tokenRepository;

    @InjectMocks
    private GlobalTokenCommandAdapter globalTokenCommandAdapter;

    private Token testToken;
    private Token temporaryToken;

    @BeforeEach
    void setUp() {
        // 실제 Token 객체 생성
        testToken = Token.createToken("test-access-token", "test-refresh-token", getTestUser());
        temporaryToken = Token.createTemporaryToken("temp-access-token", "temp-refresh-token");
    }

    @Test
    @DisplayName("토큰 저장 - 사용자와 연결된 토큰")
    void shouldSaveToken_WhenValidTokenProvided() {
        // Given
        given(tokenRepository.save(testToken)).willReturn(testToken);

        // When
        Token result = globalTokenCommandAdapter.save(testToken);

        // Then
        assertThat(result).isEqualTo(testToken);
        assertThat(result.getAccessToken()).isEqualTo("test-access-token");
        assertThat(result.getRefreshToken()).isEqualTo("test-refresh-token");
        assertThat(result.getUsers()).isEqualTo(getTestUser());
        verify(tokenRepository, times(1)).save(testToken);
    }

    @Test
    @DisplayName("토큰 저장 - 임시 토큰")
    void shouldSaveTemporaryToken_WhenTemporaryTokenProvided() {
        // Given
        given(tokenRepository.save(temporaryToken)).willReturn(temporaryToken);

        // When
        Token result = globalTokenCommandAdapter.save(temporaryToken);

        // Then
        assertThat(result).isEqualTo(temporaryToken);
        assertThat(result.getAccessToken()).isEqualTo("temp-access-token");
        assertThat(result.getRefreshToken()).isEqualTo("temp-refresh-token");
        assertThat(result.getUsers()).isNull();
        verify(tokenRepository, times(1)).save(temporaryToken);
    }

    @Test
    @DisplayName("사용자 ID로 모든 토큰 삭제 - 성공")
    void shouldDeleteAllTokensByUserId_WhenUserIdProvided() {
        // Given
        Long userId = getTestUser().getId();

        // When
        globalTokenCommandAdapter.deleteAllByUserId(userId);

        // Then
        verify(tokenRepository, times(1)).deleteAllByUserId(userId);
    }

    @Test
    @DisplayName("관리자 사용자 토큰 저장 - 성공")
    void shouldSaveAdminToken_WhenAdminTokenProvided() {
        // Given
        Token adminToken = Token.createToken("admin-access", "admin-refresh", getAdminUser());
        given(tokenRepository.save(adminToken)).willReturn(adminToken);

        // When
        Token result = globalTokenCommandAdapter.save(adminToken);

        // Then
        assertThat(result).isEqualTo(adminToken);
        assertThat(result.getUsers()).isEqualTo(getAdminUser());
        assertThat(result.getUsers().getRole()).isEqualTo(jaeik.bimillog.domain.user.entity.UserRole.ADMIN);
        verify(tokenRepository, times(1)).save(adminToken);
    }
}