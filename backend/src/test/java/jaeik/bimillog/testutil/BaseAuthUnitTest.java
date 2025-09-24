package jaeik.bimillog.testutil;

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
    // Lazy 초기화를 위한 캐시 필드들
    private Token cachedTestToken;
    private SocialUserProfile cachedTestUserProfile;
    private ExistingUserDetail cachedExistingUserDetail;
    private NewUserDetail cachedNewUserDetail;
    private CustomUserDetails cachedTestCustomUserDetails;
    private List<ResponseCookie> cachedLogoutCookies;
    private List<ResponseCookie> cachedJwtCookies;

    /**
     * 테스트용 토큰 객체 획득 (lazy 초기화)
     */
    protected Token getTestToken() {
        if (cachedTestToken == null) {
            cachedTestToken = Token.createTemporaryToken(TEST_ACCESS_TOKEN, TEST_REFRESH_TOKEN);
        }
        return cachedTestToken;
    }

    /**
     * 테스트용 소셜 사용자 프로필 획득 (lazy 초기화)
     */
    protected SocialUserProfile getTestUserProfile() {
        if (cachedTestUserProfile == null) {
            cachedTestUserProfile = new SocialUserProfile(
                TEST_SOCIAL_ID,
                TEST_EMAIL,
                TEST_PROVIDER,
                TEST_SOCIAL_NICKNAME,
                TEST_PROFILE_IMAGE,
                getTestToken()
            );
        }
        return cachedTestUserProfile;
    }

    /**
     * 기존 사용자 상세 정보 획득 (lazy 초기화)
     */
    protected ExistingUserDetail getExistingUserDetail() {
        if (cachedExistingUserDetail == null) {
            cachedExistingUserDetail = TestFixtures.createExistingUserDetail(getTestUser(), 1L, 1L);
        }
        return cachedExistingUserDetail;
    }

    /**
     * 신규 사용자 상세 정보 획득 (lazy 초기화)
     */
    protected NewUserDetail getNewUserDetail() {
        if (cachedNewUserDetail == null) {
            cachedNewUserDetail = NewUserDetail.of("test-uuid-" + System.currentTimeMillis());
        }
        return cachedNewUserDetail;
    }

    /**
     * 테스트용 CustomUserDetails 획득 (lazy 초기화)
     */
    protected CustomUserDetails getTestCustomUserDetails() {
        if (cachedTestCustomUserDetails == null) {
            cachedTestCustomUserDetails = TestFixtures.createCustomUserDetails(getTestUser());
        }
        return cachedTestCustomUserDetails;
    }

    /**
     * 로그아웃 쿠키 리스트 획득 (lazy 초기화)
     */
    protected List<ResponseCookie> getLogoutCookies() {
        if (cachedLogoutCookies == null) {
            cachedLogoutCookies = createLogoutCookies();
        }
        return cachedLogoutCookies;
    }

    /**
     * JWT 쿠키 리스트 획득 (lazy 초기화)
     */
    protected List<ResponseCookie> getJwtCookies() {
        if (cachedJwtCookies == null) {
            cachedJwtCookies = createJwtCookies(TEST_ACCESS_TOKEN, TEST_REFRESH_TOKEN);
        }
        return cachedJwtCookies;
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
        mockAuthenticatedUser(mockedSecurityContext, getTestCustomUserDetails(), UserRole.USER);
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
    

}