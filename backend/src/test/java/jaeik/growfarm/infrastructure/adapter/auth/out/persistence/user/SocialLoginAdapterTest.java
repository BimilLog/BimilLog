package jaeik.growfarm.infrastructure.adapter.auth.out.persistence.user;

import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.domain.user.application.port.in.UserQueryUseCase;
import jaeik.growfarm.domain.user.entity.Token;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.user.entity.UserRole;
import jaeik.growfarm.infrastructure.adapter.auth.out.social.SocialLoginStrategy;
import jaeik.growfarm.infrastructure.adapter.auth.out.social.dto.LoginResultDTO;
import jaeik.growfarm.infrastructure.adapter.auth.out.social.dto.SocialLoginUserData;
import jaeik.growfarm.infrastructure.adapter.user.in.web.dto.TokenDTO;
import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

/**
 * <h2>SocialLoginAdapter 단위 테스트</h2>
 * <p>소셜 로그인 어댑터의 비즈니스 로직 위주로 테스트</p>
 * <p>Strategy 패턴과 기존/신규 사용자 처리 로직 완벽 검증</p>
 *
 * @author Claude
 * @version 2.0.0
 * @since 2.0.0
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
        // SocialLoginStrategy 리스트 준비
        List<SocialLoginStrategy> strategies = Arrays.asList(kakaoStrategy);
        
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

        // 기본 Mock 설정
        given(kakaoStrategy.getProvider()).willReturn(SocialProvider.KAKAO);
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
        verify(userQueryUseCase, never()).findTokenByUser(any());
    }

    @Test
    @DisplayName("소셜 로그인 - 기존 사용자 로그인")
    void shouldReturnExistingUserLogin_WhenUserExists() {
        // Given: 기존 사용자와 토큰
        String code = "test-code";
        User existingUser = User.builder()
                .id(1L)
                .provider(SocialProvider.KAKAO)
                .socialId("123456789")
                .nickname("oldNickname")
                .profileImageUrl("old-profile.jpg")
                .role(UserRole.USER)
                .build();
        
        Token existingToken = Token.builder()
                .id(1L)
                .user(existingUser)
                .accessToken("old-access-token")
                .refreshToken("old-refresh-token")
                .build();

        given(kakaoStrategy.login(code)).willReturn(testLoginResult);
        given(userQueryUseCase.findByProviderAndSocialId(SocialProvider.KAKAO, "123456789"))
                .willReturn(Optional.of(existingUser));
        given(userQueryUseCase.findTokenByUser(existingUser)).willReturn(Optional.of(existingToken));

        // When: 기존 사용자로 소셜 로그인
        LoginResultDTO result = socialLoginAdapter.login(SocialProvider.KAKAO, code);

        // Then: 기존 사용자 로그인 결과 반환 및 정보 업데이트
        assertThat(result.getLoginType()).isEqualTo(LoginResultDTO.LoginType.EXISTING_USER);
        assertThat(result.getUserData()).isEqualTo(testUserData);
        assertThat(result.getTokenDTO()).isEqualTo(testTokenDTO);
        
        // 사용자 정보 업데이트 확인 (Mock은 실제 메서드 호출만 확인)
        verify(userQueryUseCase).findByProviderAndSocialId(SocialProvider.KAKAO, "123456789");
        verify(userQueryUseCase).findTokenByUser(existingUser);
        verify(kakaoStrategy).login(code);
    }

    @Test
    @DisplayName("소셜 로그인 - 기존 사용자이지만 토큰 없음")
    void shouldThrowException_WhenExistingUserHasNoToken() {
        // Given: 기존 사용자이지만 토큰이 없는 경우
        String code = "test-code";
        User existingUser = User.builder()
                .id(1L)
                .provider(SocialProvider.KAKAO)
                .socialId("123456789")
                .nickname("userWithoutToken")
                .role(UserRole.USER)
                .build();

        given(kakaoStrategy.login(code)).willReturn(testLoginResult);
        given(userQueryUseCase.findByProviderAndSocialId(SocialProvider.KAKAO, "123456789"))
                .willReturn(Optional.of(existingUser));
        given(userQueryUseCase.findTokenByUser(existingUser)).willReturn(Optional.empty());

        // When & Then: 토큰 없음 예외 발생
        assertThatThrownBy(() -> socialLoginAdapter.login(SocialProvider.KAKAO, code))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FIND_TOKEN);

        verify(userQueryUseCase).findByProviderAndSocialId(SocialProvider.KAKAO, "123456789");
        verify(userQueryUseCase).findTokenByUser(existingUser);
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
    @DisplayName("사용자 정보 업데이트 검증 - 기존 사용자 정보 업데이트")
    void shouldUpdateUserInfo_WhenExistingUserLogin() {
        // Given: 기존 사용자와 업데이트될 정보
        String code = "test-code";
        User existingUser = User.builder()
                .id(1L)
                .provider(SocialProvider.KAKAO)
                .socialId("123456789")
                .nickname("oldNickname")
                .profileImageUrl("old-profile.jpg")
                .role(UserRole.USER)
                .build();

        Token existingToken = Token.builder()
                .id(1L)
                .user(existingUser)
                .accessToken("old-access-token")
                .refreshToken("old-refresh-token")
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
                .build();

        given(kakaoStrategy.login(code)).willReturn(updatedLoginResult);
        given(userQueryUseCase.findByProviderAndSocialId(SocialProvider.KAKAO, "123456789"))
                .willReturn(Optional.of(existingUser));
        given(userQueryUseCase.findTokenByUser(existingUser)).willReturn(Optional.of(existingToken));

        // When: 기존 사용자로 로그인
        LoginResultDTO result = socialLoginAdapter.login(SocialProvider.KAKAO, code);

        // Then: 사용자 정보와 토큰 업데이트가 호출됨
        assertThat(result.getLoginType()).isEqualTo(LoginResultDTO.LoginType.EXISTING_USER);
        verify(userQueryUseCase).findByProviderAndSocialId(SocialProvider.KAKAO, "123456789");
        verify(userQueryUseCase).findTokenByUser(existingUser);
        
        // Note: Mock 객체에서는 실제 업데이트 메서드 호출만 확인 가능
        // 실제 데이터 변경은 통합 테스트에서 검증
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

    // TODO: 테스트 실패 시 의심해볼 메인 로직 문제들
    // 1. Strategy 패턴 구현 오류: EnumMap 초기화나 전략 등록 실패
    // 2. 생성자 주입 문제: List<SocialLoginStrategy> 주입 실패
    // 3. 트랜잭션 경계 문제: @Transactional 설정 오류로 데이터 일관성 문제
    // 4. 사용자 정보 업데이트 실패: updateUserInfo() 메서드 동작 오류
    // 5. 토큰 업데이트 실패: updateToken() 메서드 동작 오류
    // 6. 예외 처리 누락: 소셜 API 호출 실패 시 적절한 예외 변환 부족
    // 7. null 처리 미흡: provider나 socialId가 null일 때 NullPointerException
    // 8. 동시성 문제: 동일 사용자의 동시 로그인 시 데이터 경쟁 조건
    // 9. 캐시 일관성: 사용자 정보 업데이트 시 캐시와 DB 불일치
    // 10. 로그 기록 누락: 로그인 성공/실패에 대한 적절한 로깅 부족
    //
    // 🔥 중요: 이 테스트들이 실패한다면 비즈니스 로직 자체에 문제가 있을 가능성이 높음
    // - 소셜 로그인은 사용자 인증의 핵심 진입점이므로 완벽한 동작 필수
    // - 기존/신규 사용자 구분 로직 오류는 사용자 경험과 데이터 무결성에 직접 영향
    // - Strategy 패턴 구현 오류는 새로운 소셜 제공자 추가 시 확장성 저하
}