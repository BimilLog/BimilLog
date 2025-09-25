package jaeik.bimillog.testutil;

import jaeik.bimillog.domain.auth.entity.SocialUserProfile;
import jaeik.bimillog.domain.auth.entity.Token;
import jaeik.bimillog.domain.user.entity.ExistingUserDetail;
import jaeik.bimillog.domain.user.entity.NewUserDetail;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
import org.springframework.http.ResponseCookie;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <h2>인증 도메인 단위 테스트 베이스 클래스</h2>
 * <p>인증 관련 단위 테스트가 사용하는 기본 클래스</p>
 * <p>BaseUnitTest를 확장하여 인증 관련 테스트 데이터를 lazy 초기화로 제공</p>
 *
 * <h3>제공되는 기능:</h3>
 * <ul>
 *   <li>테스트용 토큰 데이터</li>
 *   <li>소셜 로그인 사용자 프로필</li>
 *   <li>기존/신규 사용자 상세 정보</li>
 *   <li>CustomUserDetails (Spring Security)</li>
 *   <li>JWT 및 로그아웃 쿠키</li>
 * </ul>
 */
public abstract class BaseAuthUnitTest extends BaseUnitTest {
    
    // 테스트 상수들
    protected static final String TEST_SOCIAL_ID = "kakao123456";
    protected static final String TEST_ACCESS_TOKEN = "access-test-token";
    protected static final String TEST_REFRESH_TOKEN = "refresh-test-token";
    protected static final String TEST_AUTH_CODE = "auth-code-123";
    protected static final String TEST_FCM_TOKEN = "fcm-token-123";
    protected static final SocialProvider TEST_PROVIDER = SocialProvider.KAKAO;
    protected static final String TEST_EMAIL = "test@example.com";
    protected static final String TEST_SOCIAL_NICKNAME = "테스트유저";
    protected static final String TEST_PROFILE_IMAGE = "http://example.com/profile.jpg";
    
    // Lazy 초기화를 위한 필드들
    private Token cachedTestToken;
    private SocialUserProfile cachedTestUserProfile;
    private ExistingUserDetail cachedExistingUserDetail;
    private NewUserDetail cachedNewUserDetail;
    private CustomUserDetails cachedTestCustomUserDetails;
    private List<ResponseCookie> cachedLogoutCookies;
    private List<ResponseCookie> cachedJwtCookies;
    
    /**
     * 테스트용 토큰 획득
     */
    protected Token getTestToken() {
        if (cachedTestToken == null) {
            cachedTestToken = Token.createTemporaryToken(TEST_ACCESS_TOKEN, TEST_REFRESH_TOKEN);
        }
        return cachedTestToken;
    }
    
    /**
     * 소셜 로그인 사용자 프로필 획득
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
     * 기존 사용자 상세 정보 획득
     */
    protected ExistingUserDetail getExistingUserDetail() {
        if (cachedExistingUserDetail == null) {
            Long settingId = 1L;
            if (getTestUser().getSetting() != null && getTestUser().getSetting().getId() != null) {
                settingId = getTestUser().getSetting().getId();
            }
            cachedExistingUserDetail = ExistingUserDetail.builder()
                    .userId(getTestUser().getId())
                    .socialId(TEST_SOCIAL_ID)
                    .provider(getTestUser().getProvider())
                    .settingId(settingId)
                    .socialNickname(TEST_SOCIAL_NICKNAME)
                    .thumbnailImage(TEST_PROFILE_IMAGE)
                    .userName(getTestUser().getUserName())
                    .role(getTestUser().getRole())
                    .tokenId(null)
                    .fcmTokenId(null)
                    .build();
        }
        return cachedExistingUserDetail;
    }
    
    /**
     * 신규 사용자 상세 정보 획득
     */
    protected NewUserDetail getNewUserDetail() {
        if (cachedNewUserDetail == null) {
            cachedNewUserDetail = NewUserDetail.builder()
                    .uuid("test-uuid-123")
                    .build();
        }
        return cachedNewUserDetail;
    }
    
    /**
     * CustomUserDetails 획득
     */
    protected CustomUserDetails getTestCustomUserDetails() {
        if (cachedTestCustomUserDetails == null) {
            cachedTestCustomUserDetails = TestFixtures.createCustomUserDetails(getTestUser());
        }
        return cachedTestCustomUserDetails;
    }
    
    /**
     * 로그아웃 쿠키 획득
     */
    protected List<ResponseCookie> getLogoutCookies() {
        if (cachedLogoutCookies == null) {
            cachedLogoutCookies = Arrays.asList(
                    ResponseCookie.from("accessToken", "")
                            .maxAge(0)
                            .path("/")
                            .secure(true)
                            .httpOnly(true)
                            .sameSite("Strict")
                            .build(),
                    ResponseCookie.from("refreshToken", "")
                            .maxAge(0)
                            .path("/")
                            .secure(true)
                            .httpOnly(true)
                            .sameSite("Strict")
                            .build()
            );
        }
        return cachedLogoutCookies;
    }
    
    /**
     * JWT 쿠키 획득
     */
    protected List<ResponseCookie> getJwtCookies() {
        if (cachedJwtCookies == null) {
            String accessToken = "test-access-token";
            String refreshToken = "test-refresh-token";
            
            cachedJwtCookies = Arrays.asList(
                    ResponseCookie.from("accessToken", accessToken)
                            .maxAge(3600) // 1 hour
                            .path("/")
                            .secure(true)
                            .httpOnly(true)
                            .sameSite("Strict")
                            .build(),
                    ResponseCookie.from("refreshToken", refreshToken)
                            .maxAge(86400) // 24 hours
                            .path("/")
                            .secure(true)
                            .httpOnly(true)
                            .sameSite("Strict")
                            .build()
            );
        }
        return cachedJwtCookies;
    }

    /**
     * 복수의 임시 토큰 생성
     * 매 호출마다 새로운 리스트를 생성하여 반환
     */
    protected List<Token> createMultipleTokens(int count) {
        List<Token> tokens = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tokens.add(Token.createTemporaryToken(
                    "access-token-" + i,
                    "refresh-token-" + i
            ));
        }
        return tokens;
    }
}