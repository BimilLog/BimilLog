package jaeik.bimillog.integration.api.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.user.out.persistence.user.user.UserRepository;
import jaeik.bimillog.infrastructure.adapter.user.out.social.dto.UserDTO;
import jaeik.bimillog.infrastructure.auth.CustomUserDetails;
import jaeik.bimillog.testutil.TestContainersConfiguration;
import jaeik.bimillog.testutil.TestSocialLoginPortConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * <h2>인증 명령 컨트롤러 통합 테스트</h2>
 * <p>@SpringBootTest를 사용한 실제 Auth Command API 통합 테스트</p>
 * <p>TestContainers를 사용하여 실제 MySQL 환경에서 테스트</p>
 * <p>Mock Social Login 설정을 통해 실제 외부 API 호출 없이 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@Import({TestContainersConfiguration.class, TestSocialLoginPortConfig.class})
@Transactional
@DisplayName("인증 명령 컨트롤러 통합 테스트")
class AuthCommandControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("소셜 로그인 통합 테스트 - 신규 사용자")
    void socialLogin_NewUser_IntegrationTest() throws Exception {
        // Given
        String provider = "KAKAO";
        String code = "new_user_code"; // TestSocialLoginPortConfig에서 신규 사용자로 처리
        String fcmToken = "integration-test-fcm-token";

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .param("provider", provider)
                        .param("code", code)
                        .param("fcmToken", fcmToken)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(jsonPath("$.uuid").exists())
                .andExpect(jsonPath("$.uuid").isString());
    }

    @Test
    @DisplayName("소셜 로그인 통합 테스트 - 기존 사용자")
    void socialLogin_ExistingUser_IntegrationTest() throws Exception {
        // Given - 기존 사용자 데이터 준비
        User existingUser = createTestUser();
        userRepository.save(existingUser);

        String provider = "KAKAO";
        String code = "existing_user_code"; // TestSocialLoginPortConfig에서 기존 사용자로 처리

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .param("provider", provider)
                        .param("code", code)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(jsonPath("$.status").value("EXISTING_USER"))
                .andExpect(jsonPath("$.data.message").value("LOGIN_SUCCESS"));
    }

    @Test
    @DisplayName("회원가입 통합 테스트 - 성공")
    void signUp_IntegrationTest_Success() throws Exception {
        // Given
        String userName = "통합테스트사용자";
        String uuid = "integration-test-uuid-12345";

        // When & Then
        mockMvc.perform(post("/api/auth/signup")
                        .param("userName", userName)
                        .cookie(new jakarta.servlet.http.Cookie("uuid", uuid))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.message").value("SIGNUP_SUCCESS"));
    }

    @Test
    @DisplayName("로그아웃 통합 테스트 - 성공")
    void logout_IntegrationTest_Success() throws Exception {
        // Given - 인증된 사용자
        User testUser = createTestUser();
        userRepository.save(testUser);
        
        CustomUserDetails userDetails = createCustomUserDetails(testUser);

        // When & Then
        mockMvc.perform(post("/api/auth/logout")
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.message").value("LOGOUT_SUCCESS"));
    }

    @Test
    @DisplayName("회원탈퇴 통합 테스트 - 성공")
    void withdraw_IntegrationTest_Success() throws Exception {
        // Given - 인증된 사용자
        User testUser = createTestUser();
        userRepository.save(testUser);
        
        CustomUserDetails userDetails = createCustomUserDetails(testUser);

        // When & Then
        mockMvc.perform(delete("/api/auth/withdraw")
                        .with(user(userDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.message").value("WITHDRAW_SUCCESS"));
    }

    @Test
    @DisplayName("소셜 로그인 - 잘못된 provider 통합 테스트")
    void socialLogin_InvalidProvider_IntegrationTest() throws Exception {
        // Given
        String provider = "INVALID_PROVIDER";
        String code = "test-code";

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .param("provider", provider)
                        .param("code", code)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CSRF 토큰 없이 POST 요청 - 403 Forbidden")
    void postWithoutCsrf_Forbidden() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .param("provider", "KAKAO")
                        .param("code", "test-code")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("인증이 필요한 API에 비인증 요청 - 403 Forbidden")
    void authenticatedApiWithoutAuth_Forbidden() throws Exception {
        // When & Then
        // TODO: Spring Security가 401 대신 403 Forbidden 응답 (정상 동작)
        // 인증되지 않은 사용자가 보호된 API 접근 시 403 응답이 올바른 보안 정책
        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("소셜 로그인 응답 헤더 검증")
    void socialLogin_ResponseHeaders_IntegrationTest() throws Exception {
        // Given
        String provider = "KAKAO";
        String code = "new_user_code";

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .param("provider", provider)
                        .param("code", code)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(content().contentType("application/json"));
    }

    /**
     * 테스트용 User 엔티티 생성
     */
    private User createTestUser() {
        Setting setting = Setting.builder()
                .messageNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(true)
                .build();

        return User.builder()
                .socialId("test-social-id-12345")
                .socialNickname("통합테스트소셜닉네임")
                .thumbnailImage("http://example.com/integration-test.jpg")
                .userName("통합테스트사용자")
                .provider(SocialProvider.KAKAO)
                .role(UserRole.USER)
                .setting(setting)
                .build();
    }

    /**
     * 테스트용 CustomUserDetails 생성
     */
    private CustomUserDetails createCustomUserDetails(User user) {
        UserDTO userDTO = UserDTO.builder()
                .userId(user.getId())
                .settingId(user.getSetting().getId())
                .socialId(user.getSocialId())
                .socialNickname(user.getSocialNickname())
                .thumbnailImage(user.getThumbnailImage())
                .userName(user.getUserName())
                .provider(user.getProvider())
                .role(user.getRole())
                .build();

        return new CustomUserDetails(userDTO);
    }
}