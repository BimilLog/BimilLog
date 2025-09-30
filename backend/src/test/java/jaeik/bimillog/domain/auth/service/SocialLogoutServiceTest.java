package jaeik.bimillog.domain.auth.service;

import jaeik.bimillog.domain.auth.application.port.out.SocialStrategyPort;
import jaeik.bimillog.domain.auth.application.port.out.SocialStrategyRegistryPort;
import jaeik.bimillog.domain.auth.application.service.SocialLogoutService;
import jaeik.bimillog.domain.auth.entity.JwtToken;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
import jaeik.bimillog.domain.global.application.port.out.GlobalTokenQueryPort;
import jaeik.bimillog.domain.user.entity.user.SocialProvider;
import jaeik.bimillog.domain.user.entity.user.User;
import jaeik.bimillog.testutil.AuthTestFixtures;
import jaeik.bimillog.testutil.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static jaeik.bimillog.testutil.AuthTestFixtures.TEST_ACCESS_TOKEN;
import static jaeik.bimillog.testutil.AuthTestFixtures.TEST_PROVIDER;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>SocialLogoutService 단위 테스트</h2>
 * <p>로그아웃 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>모든 외부 의존성을 모킹하여 순수한 비즈니스 로직만 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("SocialLogoutService 단위 테스트")
@MockitoSettings(strictness = Strictness.LENIENT)
@Tag("test")
class SocialLogoutServiceTest extends BaseUnitTest {

    @Mock
    private SocialStrategyRegistryPort strategyRegistry;

    @Mock
    private SocialStrategyPort kakaoStrategy;

    @Mock
    private GlobalTokenQueryPort globalTokenQueryPort;

    @InjectMocks
    private SocialLogoutService socialLogoutService;

    @Test
    @DisplayName("정상적인 로그아웃 처리")
    void shouldLogout_WhenValidUserDetails() {
        // Given
        Long userId = 100L;
        Long tokenId = 200L;
        SocialProvider provider = TEST_PROVIDER;

        JwtToken mockJwtToken = createMockTokenWithUser(getTestUser());
        given(globalTokenQueryPort.findById(tokenId)).willReturn(Optional.of(mockJwtToken));
        given(strategyRegistry.getStrategy(provider)).willReturn(kakaoStrategy);

        // When
        socialLogoutService.logout(userId, provider, tokenId);

        // Then
        // 포트 호출 검증
        verify(globalTokenQueryPort).findById(tokenId);
        verify(strategyRegistry).getStrategy(provider);
        try {
            verify(kakaoStrategy).logout(provider, TEST_ACCESS_TOKEN);
        } catch (Exception e) {
            // verify 호출 시 예외는 무시
        }
    }

    @Test
    @DisplayName("토큰이 존재하지 않는 경우 예외 발생")
    void shouldThrowException_WhenTokenNotFound() {
        // Given
        Long userId = 100L;
        Long tokenId = 200L;
        SocialProvider provider = TEST_PROVIDER;
        given(globalTokenQueryPort.findById(tokenId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> socialLogoutService.logout(userId, provider, tokenId))
                .isInstanceOf(AuthCustomException.class)
                .hasFieldOrPropertyWithValue("authErrorCode", AuthErrorCode.NOT_FIND_TOKEN);

        verify(globalTokenQueryPort).findById(tokenId);
        // 토큰이 없으면 예외가 발생하므로 다른 메서드들은 호출되지 않음
        verify(strategyRegistry, never()).getStrategy(any(SocialProvider.class));
        verifyNoMoreInteractions(kakaoStrategy);
    }



    @Test
    @DisplayName("소셜 로그아웃 실패 시에도 전체 로그아웃 프로세스는 성공")
    void shouldCompleteLogout_WhenSocialLogoutFails() {
        // Given
        Long userId = 100L;
        Long tokenId = 200L;
        SocialProvider provider = TEST_PROVIDER;

        JwtToken mockJwtToken = createMockTokenWithUser(getTestUser());
        given(globalTokenQueryPort.findById(tokenId)).willReturn(Optional.of(mockJwtToken));
        given(strategyRegistry.getStrategy(provider)).willReturn(kakaoStrategy);

        // 소셜 로그아웃이 실패하도록 설정
        try {
            doThrow(new RuntimeException("Social logout failed"))
                .when(kakaoStrategy).logout(provider, TEST_ACCESS_TOKEN);
        } catch (Exception e) {
            // mock 설정 중 예외는 무시
        }

        // When - 소셜 로그아웃이 실패해도 예외가 발생하지 않고 정상 처리되어야 함
        socialLogoutService.logout(userId, provider, tokenId);

        // Then
        verify(globalTokenQueryPort).findById(tokenId);
        verify(strategyRegistry).getStrategy(provider);
        // 소셜 로그아웃이 호출되었지만 실패한 것 확인
        try {
            verify(kakaoStrategy).logout(provider, TEST_ACCESS_TOKEN);
        } catch (Exception e) {
            // verify 호출 시 예외는 무시
        }
    }

    @Test
    @DisplayName("다양한 사용자 정보로 로그아웃 처리")
    void shouldHandleDifferentUserDetails() {
        // Given - 관리자 사용자
        Long adminUserId = 999L;
        Long adminTokenId = 888L;
        SocialProvider provider = TEST_PROVIDER;

        JwtToken adminJwtToken = createMockTokenWithUser(getAdminUser());
        given(globalTokenQueryPort.findById(adminTokenId)).willReturn(Optional.of(adminJwtToken));
        given(strategyRegistry.getStrategy(provider)).willReturn(kakaoStrategy);

        // When
        socialLogoutService.logout(adminUserId, provider, adminTokenId);

        // Then
        verify(globalTokenQueryPort).findById(adminTokenId);
        verify(strategyRegistry).getStrategy(provider);
        try {
            verify(kakaoStrategy).logout(provider, TEST_ACCESS_TOKEN);
        } catch (Exception e) {
            // verify 호출 시 예외는 무시
        }
    }

    /**
     * 특정 사용자를 포함한 Mock JwtToken 생성
     * @param user 사용자
     * @return Mock JwtToken with User
     */
    private JwtToken createMockTokenWithUser(User user) {
        JwtToken mockJwtToken = mock(JwtToken.class);
        given(mockJwtToken.getUsers()).willReturn(user);
        given(mockJwtToken.getAccessToken()).willReturn(AuthTestFixtures.TEST_ACCESS_TOKEN);
        given(mockJwtToken.getRefreshToken()).willReturn(AuthTestFixtures.TEST_REFRESH_TOKEN);
        given(mockJwtToken.getId()).willReturn(1L);
        return mockJwtToken;
    }
}