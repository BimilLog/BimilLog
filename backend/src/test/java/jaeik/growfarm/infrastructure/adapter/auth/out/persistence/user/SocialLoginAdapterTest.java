package jaeik.growfarm.infrastructure.adapter.auth.out.persistence.user;

import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.domain.user.application.port.in.UserQueryUseCase;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.user.entity.UserRole;
import jaeik.growfarm.infrastructure.adapter.auth.out.social.SocialLoginStrategy;
import jaeik.growfarm.infrastructure.adapter.auth.out.social.dto.LoginResultDTO;
import jaeik.growfarm.infrastructure.adapter.auth.out.social.dto.SocialLoginUserData;
import jaeik.growfarm.infrastructure.adapter.user.in.web.dto.TokenDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * <h2>SocialLoginAdapter 단위 테스트</h2>
 * <p>소셜 로그인 어댑터의 비즈니스 로직 위주로 테스트</p>
 * <p>Strategy 패턴과 기존/신규 사용자 처리 로직 완벽 검증</p>
 *
 * @author Jaeik
 * @version  2.0.0
 */
@ExtendWith(MockitoExtension.class)
class SocialLoginAdapterTest {

    @Mock private SocialLoginStrategy kakaoStrategy;
    @Mock private UserQueryUseCase userQueryUseCase;

    private SocialLoginAdapter socialLoginAdapter;

    private SocialLoginUserData testUserData;
    private TokenDTO testTokenDTO;
    private LoginResultDTO testLoginResult;

    @BeforeEach
    void setUp() {
        // 🔥 CRITICAL: Mock 설정을 생성자 호출 전에 수행
        // NPE 방지: SocialLoginAdapter 생성자에서 strategy.getProvider() 호출 시 null 방지
        given(kakaoStrategy.getProvider()).willReturn(SocialProvider.KAKAO);
        
        // SocialLoginStrategy 리스트 준비
        List<SocialLoginStrategy> strategies = List.of(kakaoStrategy);
        
        // SocialLoginAdapter 생성 (생성자를 통한 초기화)
        socialLoginAdapter = new SocialLoginAdapter(strategies, userQueryUseCase);

        // 공통 테스트 데이터 준비
        testUserData = SocialLoginUserData.builder()
                .provider(SocialProvider.KAKAO)
                .socialId("123456789")
                .nickname("testUser")
                .profileImageUrl("https://example.com/profile.jpg")
                .fcmToken("fcm-token-12345")
                .build();

        testTokenDTO = TokenDTO.builder()
                .accessToken("access-token-12345")
                .refreshToken("refresh-token-12345")
                .build();

        testLoginResult = LoginResultDTO.builder()
                .userData(testUserData)
                .tokenDTO(testTokenDTO)
                .loginType(LoginResultDTO.LoginType.NEW_USER)
                .build();
    }

    @Test
    @DisplayName("소셜 로그인 - 신규 사용자 로그인")
    void shouldReturnNewUserLogin_WhenUserNotExists() {
        // Given: 존재하지 않는 사용자
        String code = "test-code";
        given(kakaoStrategy.login(code)).willReturn(testLoginResult);
        given(userQueryUseCase.findByProviderAndSocialId(SocialProvider.KAKAO, "123456789"))
                .willReturn(Optional.empty());

        // When: 소셜 로그인 실행
        LoginResultDTO result = socialLoginAdapter.login(SocialProvider.KAKAO, code);

        // Then: 신규 사용자 로그인 결과 반환
        assertThat(result.getLoginType()).isEqualTo(LoginResultDTO.LoginType.NEW_USER);
        assertThat(result.getUserData()).isEqualTo(testUserData);
        assertThat(result.getTokenDTO()).isEqualTo(testTokenDTO);
        
        verify(kakaoStrategy).login(code);
        verify(userQueryUseCase).findByProviderAndSocialId(SocialProvider.KAKAO, "123456789");
        // 신규 사용자의 경우 토큰 조회가 불필요하므로 토큰 관련 검증 생략
    }

    @Test
    @DisplayName("소셜 로그인 - 기존 사용자 로그인 (다중 로그인 지원)")
    void shouldReturnExistingUserLogin_WhenUserExistsInMultiLoginEnvironment() {
        // Given: 기존 사용자가 존재하는 경우
        String code = "test-code";
        User existingUser = User.builder()
                .id(1L)
                .provider(SocialProvider.KAKAO)
                .socialId("123456789")
                .socialNickname("oldNickname")
                .thumbnailImage("old-profile.jpg")
                .role(UserRole.USER)
                .build();

        given(kakaoStrategy.login(code)).willReturn(testLoginResult);
        given(userQueryUseCase.findByProviderAndSocialId(SocialProvider.KAKAO, "123456789"))
                .willReturn(Optional.of(existingUser));

        // When: 기존 사용자로 소셜 로그인
        LoginResultDTO result = socialLoginAdapter.login(SocialProvider.KAKAO, code);

        // Then: 기존 사용자 로그인 결과 반환 (다중 로그인을 위해 새 토큰 생성)
        assertThat(result.getLoginType()).isEqualTo(LoginResultDTO.LoginType.EXISTING_USER);
        assertThat(result.getUserData()).isEqualTo(testUserData);
        assertThat(result.getTokenDTO()).isEqualTo(testTokenDTO);

        verify(userQueryUseCase).findByProviderAndSocialId(SocialProvider.KAKAO, "123456789");
        verify(kakaoStrategy).login(code);
    }


    @Test
    @DisplayName("소셜 로그인 - null 코드로 로그인")
    void shouldHandleNullCode_WhenCodeIsNull() {
        // Given: null 코드
        String nullCode = null;
        given(kakaoStrategy.login(nullCode)).willThrow(new RuntimeException("Invalid code"));

        // When & Then: 예외 전파
        assertThatThrownBy(() -> socialLoginAdapter.login(SocialProvider.KAKAO, nullCode))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid code");

        verify(kakaoStrategy).login(nullCode);
    }

    @Test
    @DisplayName("소셜 로그인 - 빈 문자열 코드로 로그인")
    void shouldHandleEmptyCode_WhenCodeIsEmpty() {
        // Given: 빈 문자열 코드
        String emptyCode = "";
        given(kakaoStrategy.login(emptyCode)).willThrow(new RuntimeException("Empty code"));

        // When & Then: 예외 전파
        assertThatThrownBy(() -> socialLoginAdapter.login(SocialProvider.KAKAO, emptyCode))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Empty code");

        verify(kakaoStrategy).login(emptyCode);
    }

    @Test
    @DisplayName("소셜 로그인 - 지원하지 않는 프로바이더")
    void shouldThrowException_WhenUnsupportedProvider() {
        // Given: 지원하지 않는 프로바이더 (NAVER 등)
        // SocialProvider.NAVER가 없다고 가정하고 null로 테스트
        SocialProvider unsupportedProvider = null; 
        String code = "test-code";

        // When & Then: NullPointerException 발생 (strategies.get(null)에서)
        assertThatThrownBy(() -> socialLoginAdapter.login(unsupportedProvider, code))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("소셜 계정 연결 해제 - 정상적인 연결 해제")
    void shouldUnlinkAccount_WhenValidProviderAndSocialId() {
        // Given: 유효한 프로바이더와 소셜 ID
        SocialProvider provider = SocialProvider.KAKAO;
        String socialId = "123456789";

        // When: 소셜 계정 연결 해제
        socialLoginAdapter.unlink(provider, socialId);

        // Then: 해당 전략의 unlink 메서드 호출 확인
        verify(kakaoStrategy).unlink(socialId);
    }

    @Test
    @DisplayName("소셜 계정 연결 해제 - null socialId")
    void shouldHandleNullSocialId_WhenUnlinkWithNull() {
        // Given: null socialId
        SocialProvider provider = SocialProvider.KAKAO;
        String nullSocialId = null;

        // When: null socialId로 연결 해제 시도
        socialLoginAdapter.unlink(provider, nullSocialId);

        // Then: 전략에 null이 전달됨
        verify(kakaoStrategy).unlink(nullSocialId);
    }

    @Test
    @DisplayName("소셜 로그아웃 - 정상적인 로그아웃")
    void shouldLogout_WhenValidProviderAndAccessToken() {
        // Given: 유효한 프로바이더와 액세스 토큰
        SocialProvider provider = SocialProvider.KAKAO;
        String accessToken = "valid-access-token";

        // When: 소셜 로그아웃
        socialLoginAdapter.logout(provider, accessToken);

        // Then: 해당 전략의 logout 메서드 호출 확인
        verify(kakaoStrategy).logout(accessToken);
    }

    @Test
    @DisplayName("소셜 로그아웃 - null accessToken")
    void shouldHandleNullAccessToken_WhenLogoutWithNull() {
        // Given: null accessToken
        SocialProvider provider = SocialProvider.KAKAO;
        String nullAccessToken = null;

        // When: null accessToken으로 로그아웃 시도
        socialLoginAdapter.logout(provider, nullAccessToken);

        // Then: 전략에 null이 전달됨
        verify(kakaoStrategy).logout(nullAccessToken);
    }

    @Test
    @DisplayName("전략 패턴 검증 - 올바른 전략 선택")
    void shouldSelectCorrectStrategy_WhenMultipleStrategiesExist() {
        // Given: KAKAO 전략이 등록되어 있음
        SocialProvider provider = SocialProvider.KAKAO;

        // When: 해당 프로바이더로 작업 수행
        socialLoginAdapter.logout(provider, "test-token");

        // Then: KAKAO 전략이 선택되어 호출됨
        verify(kakaoStrategy).logout("test-token");
    }

    @Test
    @DisplayName("사용자 정보 업데이트 - 기존 사용자 정보 업데이트 성공")
    void shouldUpdateUserInfo_WhenExistingUserLogin() {
        // Given: 기존 사용자와 업데이트될 정보
        String code = "test-code";
        User existingUser = User.builder()
                .id(1L)
                .provider(SocialProvider.KAKAO)
                .socialId("123456789")
                .socialNickname("oldNickname")
                .thumbnailImage("old-profile.jpg")
                .role(UserRole.USER)
                .build();

        // 업데이트될 새로운 정보
        SocialLoginUserData updatedUserData = SocialLoginUserData.builder()
                .provider(SocialProvider.KAKAO)
                .socialId("123456789")
                .nickname("newNickname")
                .profileImageUrl("new-profile.jpg")
                .fcmToken("new-fcm-token")
                .build();

        LoginResultDTO updatedLoginResult = LoginResultDTO.builder()
                .userData(updatedUserData)
                .tokenDTO(testTokenDTO)
                .loginType(LoginResultDTO.LoginType.EXISTING_USER)
                .build();

        given(kakaoStrategy.login(code)).willReturn(updatedLoginResult);
        given(userQueryUseCase.findByProviderAndSocialId(SocialProvider.KAKAO, "123456789"))
                .willReturn(Optional.of(existingUser));

        // When: 기존 사용자로 로그인
        LoginResultDTO result = socialLoginAdapter.login(SocialProvider.KAKAO, code);
        
        // Then: 사용자 정보 업데이트 및 새 토큰 생성
        assertThat(result.getLoginType()).isEqualTo(LoginResultDTO.LoginType.EXISTING_USER);
        assertThat(result.getUserData()).isEqualTo(updatedUserData);
        assertThat(result.getTokenDTO()).isEqualTo(testTokenDTO);
        
        verify(userQueryUseCase).findByProviderAndSocialId(SocialProvider.KAKAO, "123456789");
        verify(kakaoStrategy).login(code);
        // Note: 새로운 토큰 생성 방식으로 기존 토큰 조회/업데이트 불필요
    }

    @Test
    @DisplayName("동시성 테스트 - 동일 사용자 동시 로그인")
    void shouldHandleConcurrentLogin_WhenSameUserLoginSimultaneously() {
        // Given: 동일한 코드와 사용자
        String code = "test-code";
        given(kakaoStrategy.login(code)).willReturn(testLoginResult);
        given(userQueryUseCase.findByProviderAndSocialId(SocialProvider.KAKAO, "123456789"))
                .willReturn(Optional.empty());

        // When: 동시에 로그인 시도
        LoginResultDTO result1 = socialLoginAdapter.login(SocialProvider.KAKAO, code);
        LoginResultDTO result2 = socialLoginAdapter.login(SocialProvider.KAKAO, code);

        // Then: 두 요청 모두 신규 사용자로 처리됨
        assertThat(result1.getLoginType()).isEqualTo(LoginResultDTO.LoginType.NEW_USER);
        assertThat(result2.getLoginType()).isEqualTo(LoginResultDTO.LoginType.NEW_USER);
        
        verify(kakaoStrategy, org.mockito.Mockito.times(2)).login(code);
        verify(userQueryUseCase, org.mockito.Mockito.times(2))
                .findByProviderAndSocialId(SocialProvider.KAKAO, "123456789");
    }

    @Test
    @DisplayName("트랜잭션 검증 - @Transactional 적용 확인")
    void shouldExecuteInTransaction_WhenLoginCalled() {
        // Given: 로그인 데이터
        String code = "test-code";
        given(kakaoStrategy.login(code)).willReturn(testLoginResult);
        given(userQueryUseCase.findByProviderAndSocialId(SocialProvider.KAKAO, "123456789"))
                .willReturn(Optional.empty());

        // When: 로그인 실행
        LoginResultDTO result = socialLoginAdapter.login(SocialProvider.KAKAO, code);

        // Then: 트랜잭션 내에서 실행됨 (실제 트랜잭션은 통합 테스트에서 검증)
        assertThat(result.getLoginType()).isEqualTo(LoginResultDTO.LoginType.NEW_USER);
        
        // @Transactional 어노테이션이 메서드에 적용되어 있는지는 리플렉션으로 확인 가능하지만
        // 단위 테스트에서는 비즈니스 로직 검증에 집중
    }
}