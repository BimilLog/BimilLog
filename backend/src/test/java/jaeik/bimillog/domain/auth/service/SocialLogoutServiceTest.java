package jaeik.bimillog.domain.auth.service;

import jaeik.bimillog.domain.auth.application.port.out.SocialStrategyPort;
import jaeik.bimillog.domain.auth.application.port.out.SocialStrategyRegistryPort;
import jaeik.bimillog.domain.auth.application.service.SocialLogoutService;
import jaeik.bimillog.domain.auth.entity.AuthToken;
import jaeik.bimillog.domain.auth.entity.KakaoToken;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
import jaeik.bimillog.domain.global.application.port.out.GlobalKakaoTokenQueryPort;
import jaeik.bimillog.domain.global.application.port.out.GlobalAuthTokenQueryPort;
import jaeik.bimillog.domain.member.entity.member.SocialProvider;
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
@Tag("unit")
class SocialLogoutServiceTest extends BaseUnitTest {

    @Mock
    private SocialStrategyRegistryPort strategyRegistry;

    @Mock
    private SocialStrategyPort kakaoStrategy;

    @Mock
    private GlobalAuthTokenQueryPort globalAuthTokenQueryPort;

    @Mock
    private GlobalKakaoTokenQueryPort globalKakaoTokenQueryPort;

    @InjectMocks
    private SocialLogoutService socialLogoutService;

    @Test
    @DisplayName("정상적인 로그아웃 처리")
    void shouldSocialLogout_WhenValidMemberDetails() throws Exception {
        // Given
        Long memberId = 100L;
        Long tokenId = 200L;
        SocialProvider provider = TEST_PROVIDER;

        AuthToken mockAuthToken = AuthToken.builder()
                .refreshToken("test-refresh-token")
                .member(getTestMember())
                .build();
        KakaoToken mockKakaoToken = KakaoToken.createKakaoToken(TEST_ACCESS_TOKEN, "kakao-refresh-token");

        given(globalAuthTokenQueryPort.findById(tokenId)).willReturn(Optional.of(mockAuthToken));
        given(globalKakaoTokenQueryPort.findByMemberId(memberId)).willReturn(Optional.of(mockKakaoToken));
        given(strategyRegistry.getStrategy(provider)).willReturn(kakaoStrategy);

        // When
        socialLogoutService.socialLogout(memberId, provider, tokenId);

        // Then
        verify(globalAuthTokenQueryPort).findById(tokenId);
        verify(globalKakaoTokenQueryPort).findByMemberId(memberId);
        verify(strategyRegistry).getStrategy(provider);
        try {
            verify(kakaoStrategy).logout(provider, TEST_ACCESS_TOKEN);
        } catch (Exception e) {
            // verify 호출 시 예외는 무시
        }
    }

    @Test
    @DisplayName("토큰이 존재하지 않는 경우 예외 발생")
    void shouldThrowException_WhenTokenNotFound() throws Exception {
        // Given
        Long memberId = 100L;
        Long tokenId = 200L;
        SocialProvider provider = TEST_PROVIDER;
        given(globalAuthTokenQueryPort.findById(tokenId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> socialLogoutService.socialLogout(memberId, provider, tokenId))
                .isInstanceOf(AuthCustomException.class)
                .hasFieldOrPropertyWithValue("authErrorCode", AuthErrorCode.NOT_FIND_TOKEN);

        verify(globalAuthTokenQueryPort).findById(tokenId);
        // 토큰이 없으면 예외가 발생하므로 다른 메서드들은 호출되지 않음
        verify(strategyRegistry, never()).getStrategy(any(SocialProvider.class));
        verifyNoMoreInteractions(kakaoStrategy);
    }



    @Test
    @DisplayName("소셜 로그아웃 실패 시에도 전체 로그아웃 프로세스는 성공")
    void shouldCompleteLogout_WhenSocialSocialLogoutFails() throws Exception {
        // Given
        Long memberId = 100L;
        Long tokenId = 200L;
        SocialProvider provider = TEST_PROVIDER;

        AuthToken mockAuthToken = AuthToken.builder()
                .refreshToken("test-refresh-token")
                .member(getTestMember())
                .build();
        KakaoToken mockKakaoToken = KakaoToken.createKakaoToken(TEST_ACCESS_TOKEN, "kakao-refresh-token");

        given(globalAuthTokenQueryPort.findById(tokenId)).willReturn(Optional.of(mockAuthToken));
        given(globalKakaoTokenQueryPort.findByMemberId(memberId)).willReturn(Optional.of(mockKakaoToken));
        given(strategyRegistry.getStrategy(provider)).willReturn(kakaoStrategy);

        // 소셜 로그아웃이 실패하도록 설정
        try {
            doThrow(new RuntimeException("Social socialLogout failed"))
                .when(kakaoStrategy).logout(provider, TEST_ACCESS_TOKEN);
        } catch (Exception e) {
            // mock 설정 중 예외는 무시
        }

        // When - 소셜 로그아웃이 실패해도 예외가 발생하지 않고 정상 처리되어야 함
        socialLogoutService.socialLogout(memberId, provider, tokenId);

        // Then
        verify(globalAuthTokenQueryPort).findById(tokenId);
        verify(globalKakaoTokenQueryPort).findByMemberId(memberId);
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
    void shouldHandleDifferentMemberDetails() throws Exception {
        // Given - 관리자 사용자
        Long adminMemberId = 999L;
        Long adminTokenId = 888L;
        SocialProvider provider = TEST_PROVIDER;

        AuthToken adminAuthToken = AuthToken.builder()
                .refreshToken("admin-refresh-token")
                .member(getAdminMember())
                .build();
        KakaoToken mockKakaoToken = KakaoToken.createKakaoToken(TEST_ACCESS_TOKEN, "kakao-refresh-token");

        given(globalAuthTokenQueryPort.findById(adminTokenId)).willReturn(Optional.of(adminAuthToken));
        given(globalKakaoTokenQueryPort.findByMemberId(adminMemberId)).willReturn(Optional.of(mockKakaoToken));
        given(strategyRegistry.getStrategy(provider)).willReturn(kakaoStrategy);

        // When
        socialLogoutService.socialLogout(adminMemberId, provider, adminTokenId);

        // Then
        verify(globalAuthTokenQueryPort).findById(adminTokenId);
        verify(globalKakaoTokenQueryPort).findByMemberId(adminMemberId);
        verify(strategyRegistry).getStrategy(provider);
        try {
            verify(kakaoStrategy).logout(provider, TEST_ACCESS_TOKEN);
        } catch (Exception e) {
            // verify 호출 시 예외는 무시
        }
    }
}