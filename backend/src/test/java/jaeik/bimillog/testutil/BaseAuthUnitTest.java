package jaeik.bimillog.testutil;

import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.domain.auth.entity.SocialUserProfile;
import jaeik.bimillog.domain.auth.entity.Token;
import jaeik.bimillog.domain.user.entity.*;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockedStatic;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * <h2>Auth 테스트 전용 베이스 클래스</h2>
 * <p>인증/인가 관련 단위 테스트가 상속받아 사용하는 기본 클래스</p>
 * <p>Auth 도메인 테스트에 필요한 공통 데이터와 유틸리티 메서드 제공</p>
 * 
 * <h3>제공되는 기능:</h3>
 * <ul>
 *   <li>Auth 도메인 공통 상수</li>
 *   <li>미리 설정된 Token, SocialUserProfile, UserDetail 객체</li>
 *   <li>SecurityContext Mock 헬퍼 메서드</li>
 *   <li>ResponseCookie 생성 유틸리티</li>
 * </ul>
 * 
 * <h3>사용 예시:</h3>
 * <pre>
 * class SomeAuthServiceTest extends BaseAuthUnitTest {
 *     {@literal @}Mock private SomeRepository repository;
 *     
 *     {@literal @}Test
 *     void test() {
 *         // testToken, testUserProfile, existingUserDetail 등이 이미 준비됨
 *         // mockAnonymousAuthentication() 등의 헬퍼 메서드 사용 가능
 *     }
 * }
 * </pre>
 *
 * @author Jaeik
 * @version 1.0.0
 */
public abstract class BaseAuthUnitTest extends BaseUnitTest {

    // ==================== 공통 상수 ====================
    protected static final String TEST_SOCIAL_ID = "kakao123456";
    protected static final String TEST_EMAIL = "test@example.com";
    protected static final String TEST_USERNAME = "testUser";
    protected static final String TEST_SOCIAL_NICKNAME = "테스트유저";
    protected static final String TEST_PROFILE_IMAGE = "http://example.com/profile.jpg";
    protected static final String TEST_ACCESS_TOKEN = "access-test-token";
    protected static final String TEST_REFRESH_TOKEN = "refresh-test-token";
    protected static final String TEST_AUTH_CODE = "auth-code-123";
    protected static final String TEST_FCM_TOKEN = "fcm-token-123";
    protected static final SocialProvider TEST_PROVIDER = SocialProvider.KAKAO;
    
    // ==================== Auth 관련 테스트 데이터 ====================
    /**
     * 테스트용 토큰 객체
     */
    protected Token testToken;
    
    /**
     * 테스트용 소셜 사용자 프로필
     */
    protected SocialUserProfile testUserProfile;
    
    /**
     * 기존 사용자 상세 정보
     */
    protected ExistingUserDetail existingUserDetail;
    
    /**
     * 신규 사용자 상세 정보
     */
    protected NewUserDetail newUserDetail;
    
    /**
     * 테스트용 CustomUserDetails
     */
    protected CustomUserDetails testCustomUserDetails;
    
    /**
     * 로그아웃 쿠키 리스트
     */
    protected List<ResponseCookie> logoutCookies;
    
    /**
     * JWT 쿠키 리스트
     */
    protected List<ResponseCookie> jwtCookies;

    /**
     * 각 테스트 메서드 실행 전 Auth 관련 데이터 초기화
     */
    @BeforeEach
    protected void setUpAuthBase() {
        // 기본 토큰 생성
        this.testToken = Token.createTemporaryToken(TEST_ACCESS_TOKEN, TEST_REFRESH_TOKEN);
        
        // 소셜 사용자 프로필 생성
        this.testUserProfile = new SocialUserProfile(
            TEST_SOCIAL_ID,
            TEST_EMAIL,
            TEST_PROVIDER,
            TEST_SOCIAL_NICKNAME,
            TEST_PROFILE_IMAGE,
            testToken
        );
        
        // 기존 사용자 상세 정보 생성
        this.existingUserDetail = ExistingUserDetail.builder()
            .userId(testUser.getId() != null ? testUser.getId() : 1L)
            .tokenId(1L)
            .fcmTokenId(1L)
            .socialId(TEST_SOCIAL_ID)
            .provider(TEST_PROVIDER)
            .settingId(defaultSetting.getId() != null ? defaultSetting.getId() : 1L)
            .userName(TEST_USERNAME)
            .socialNickname(TEST_SOCIAL_NICKNAME)
            .thumbnailImage(TEST_PROFILE_IMAGE)
            .role(UserRole.USER)
            .build();
        
        // 신규 사용자 상세 정보 생성
        this.newUserDetail = NewUserDetail.of("test-uuid-" + System.currentTimeMillis());
        
        // CustomUserDetails 생성
        this.testCustomUserDetails = new CustomUserDetails(existingUserDetail);
        
        // 로그아웃 쿠키 생성
        this.logoutCookies = createLogoutCookies();
        
        // JWT 쿠키 생성
        this.jwtCookies = createJwtCookies(TEST_ACCESS_TOKEN, TEST_REFRESH_TOKEN);
        
        // 하위 클래스의 추가 설정
        setUpAuthChild();
    }
    
    /**
     * 하위 클래스에서 추가 Auth 설정이 필요한 경우 오버라이드
     */
    protected void setUpAuthChild() {
        // 하위 클래스에서 필요시 오버라이드
    }

    // ==================== SecurityContext Mock 헬퍼 메서드 ====================
    
    /**
     * 익명 사용자로 SecurityContext를 Mock 설정
     * @param mockedSecurityContext MockedStatic SecurityContextHolder
     */
    protected void mockAnonymousAuthentication(MockedStatic<SecurityContextHolder> mockedSecurityContext) {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = new AnonymousAuthenticationToken(
            "anonymous",
            "anonymous",
            List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );
        
        mockedSecurityContext.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        given(securityContext.getAuthentication()).willReturn(authentication);
    }
    
    /**
     * 인증된 사용자로 SecurityContext를 Mock 설정
     * @param mockedSecurityContext MockedStatic SecurityContextHolder
     */
    protected void mockAuthenticatedUser(MockedStatic<SecurityContextHolder> mockedSecurityContext) {
        mockAuthenticatedUser(mockedSecurityContext, testCustomUserDetails, UserRole.USER);
    }
    
    /**
     * 특정 사용자와 권한으로 SecurityContext를 Mock 설정
     * @param mockedSecurityContext MockedStatic SecurityContextHolder
     * @param userDetails CustomUserDetails
     * @param role 사용자 권한
     */
    protected void mockAuthenticatedUser(MockedStatic<SecurityContextHolder> mockedSecurityContext,
                                        CustomUserDetails userDetails,
                                        UserRole role) {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
        
        mockedSecurityContext.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        given(securityContext.getAuthentication()).willReturn(authentication);
    }

    // ==================== Cookie 생성 헬퍼 메서드 ====================
    
    /**
     * 로그아웃 쿠키 생성
     * @return 로그아웃용 쿠키 리스트 (값이 비어있고 만료시간 0)
     */
    protected List<ResponseCookie> createLogoutCookies() {
        return List.of(
            ResponseCookie.from("jwt_access_token", "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .build(),
            ResponseCookie.from("jwt_refresh_token", "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .build()
        );
    }
    
    /**
     * JWT 인증 쿠키 생성
     * @param accessToken 액세스 토큰
     * @param refreshToken 리프레시 토큰
     * @return JWT 쿠키 리스트
     */
    protected List<ResponseCookie> createJwtCookies(String accessToken, String refreshToken) {
        return List.of(
            ResponseCookie.from("jwt_access_token", accessToken)
                .path("/")
                .maxAge(60 * 60) // 1시간
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .build(),
            ResponseCookie.from("jwt_refresh_token", refreshToken)
                .path("/")
                .maxAge(60 * 60 * 24 * 7) // 7일
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .build()
        );
    }
    
    /**
     * 임시 UUID 쿠키 생성
     * @param uuid UUID 값
     * @return 임시 쿠키
     */
    protected ResponseCookie createTempCookie(String uuid) {
        return ResponseCookie.from("temp", uuid)
            .path("/")
            .maxAge(60 * 10) // 10분
            .httpOnly(true)
            .secure(true)
            .sameSite("Lax")
            .build();
    }

    // ==================== Token 생성 헬퍼 메서드 ====================
    
    /**
     * 특정 사용자를 포함한 Mock Token 생성
     * @param user 사용자
     * @return Mock Token with User
     */
    protected Token createMockTokenWithUser(User user) {
        Token mockToken = mock(Token.class);
        given(mockToken.getUsers()).willReturn(user);
        given(mockToken.getAccessToken()).willReturn(TEST_ACCESS_TOKEN);
        given(mockToken.getRefreshToken()).willReturn(TEST_REFRESH_TOKEN);
        given(mockToken.getId()).willReturn(1L);
        return mockToken;
    }
    

    
    /**
     * 여러 토큰 생성
     * @param count 생성할 토큰 개수
     * @return 토큰 리스트
     */
    protected List<Token> createMultipleTokens(int count) {
        return java.util.stream.IntStream.range(0, count)
            .mapToObj(i -> Token.createTemporaryToken(
                "access-token-" + i,
                "refresh-token-" + i
            ))
            .collect(java.util.stream.Collectors.toList());
    }

    // ==================== UserDetail 생성 헬퍼 메서드 ====================
    
    /**
     * 특정 사용자로부터 ExistingUserDetail 생성
     * @param user 사용자
     * @return ExistingUserDetail
     */
    protected ExistingUserDetail createExistingUserDetailFrom(User user) {
        return ExistingUserDetail.builder()
            .userId(user.getId())
            .tokenId(1L)
            .fcmTokenId(1L)
            .socialId(user.getSocialId())
            .provider(user.getProvider())
            .settingId(user.getSetting() != null ? user.getSetting().getId() : null)
            .userName(user.getUserName())
            .socialNickname(user.getSocialNickname())
            .thumbnailImage(user.getThumbnailImage())
            .role(user.getRole())
            .build();
    }
    
    /**
     * 특정 사용자로부터 CustomUserDetails 생성
     * @param user 사용자
     * @return CustomUserDetails
     */
    protected CustomUserDetails createCustomUserDetailsFrom(User user) {
        ExistingUserDetail userDetail = createExistingUserDetailFrom(user);
        return new CustomUserDetails(userDetail);
    }

    // ==================== LoginResult 생성 헬퍼 메서드 ====================
    
    /**
     * 기존 사용자 로그인 결과 생성
     * @param cookies 쿠키 리스트
     * @return LoginResult.ExistingUser
     */
    protected LoginResult.ExistingUser createExistingUserLoginResult(List<ResponseCookie> cookies) {
        return new LoginResult.ExistingUser(cookies);
    }
    
    /**
     * 신규 사용자 로그인 결과 생성
     * @param uuid UUID
     * @param tempCookie 임시 쿠키
     * @return LoginResult.NewUser
     */
    protected LoginResult.NewUser createNewUserLoginResult(String uuid, ResponseCookie tempCookie) {
        return new LoginResult.NewUser(uuid, tempCookie);
    }
}