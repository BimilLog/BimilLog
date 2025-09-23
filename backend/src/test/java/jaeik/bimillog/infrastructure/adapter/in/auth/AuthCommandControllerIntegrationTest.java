package jaeik.bimillog.infrastructure.adapter.in.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.bimillog.domain.user.entity.ExistingUserDetail;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.adapter.in.auth.dto.SocialLoginRequestDTO;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
import jaeik.bimillog.infrastructure.adapter.out.user.jpa.UserRepository;
import jaeik.bimillog.testutil.TestContainersConfiguration;
import jaeik.bimillog.testutil.TestSocialLoginPortConfig;
import jaeik.bimillog.testutil.TestUsers;
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
        SocialLoginRequestDTO request = new SocialLoginRequestDTO("KAKAO", "new_user_code", "integration-test-fcm-token");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
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
        User existingUser = TestUsers.createUnique();
        userRepository.save(existingUser);

        SocialLoginRequestDTO request = new SocialLoginRequestDTO("KAKAO", "existing_user_code", null);

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(jsonPath("$.status").value("EXISTING_USER"))
                .andExpect(jsonPath("$.data.message").value("LOGIN_SUCCESS"));
    }


    @Test
    @DisplayName("로그아웃 통합 테스트 - 성공")
    void logout_IntegrationTest_Success() throws Exception {
        // Given - 인증된 사용자
        User testUser = TestUsers.createUnique();
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
                .andExpect(jsonPath("$.data.message").value("로그아웃 성공"));
    }

    @Test
    @DisplayName("회원탈퇴 통합 테스트 - 성공")
    void withdraw_IntegrationTest_Success() throws Exception {
        // Given - 인증된 사용자
        User testUser = TestUsers.createUnique();
        userRepository.save(testUser);
        
        CustomUserDetails userDetails = createCustomUserDetails(testUser);

        // When & Then
        mockMvc.perform(delete("/api/user/withdraw")
                        .with(user(userDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.message").value("회원탈퇴 성공"));
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


    /**
     * 테스트용 CustomUserDetails 생성
     */
    private CustomUserDetails createCustomUserDetails(User user) {
        ExistingUserDetail userDetail = ExistingUserDetail.builder()
                .userId(user.getId())
                .settingId(user.getSetting().getId())
                .socialId(user.getSocialId())
                .socialNickname(user.getSocialNickname())
                .thumbnailImage(user.getThumbnailImage())
                .userName(user.getUserName())
                .provider(user.getProvider())
                .role(user.getRole())
                .tokenId(null)
                .fcmTokenId(null)
                .build();

        return new CustomUserDetails(userDetail);
    }
}