package jaeik.growfarm.infrastructure.adapter.auth.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.domain.user.entity.Setting;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.user.entity.UserRole;
import jaeik.growfarm.infrastructure.adapter.user.out.persistence.user.user.UserRepository;
import jaeik.growfarm.infrastructure.adapter.user.out.social.dto.UserDTO;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
import jaeik.growfarm.util.TestContainersConfiguration;
import jaeik.growfarm.util.TestSocialLoginPortConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * <h2>인증 조회 컨트롤러 통합 테스트</h2>
 * <p>@SpringBootTest를 사용한 실제 Auth Query API 통합 테스트</p>
 * <p>TestContainers를 사용하여 실제 MySQL 환경에서 테스트</p>
 * <p>실제 사용자 데이터를 DB에 저장하여 완전한 통합 테스트 수행</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@Import({TestContainersConfiguration.class, TestSocialLoginPortConfig.class})
@Transactional
@DisplayName("인증 조회 컨트롤러 통합 테스트")
class AuthQueryControllerIntegrationTest {

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
    @DisplayName("현재 사용자 정보 조회 통합 테스트 - 일반 사용자")
    void getCurrentUser_RegularUser_IntegrationTest() throws Exception {
        // Given - 실제 사용자 데이터 저장
        User testUser = createTestUser("통합테스트사용자", UserRole.USER);
        User savedUser = userRepository.save(testUser);
        
        CustomUserDetails userDetails = createCustomUserDetails(savedUser);

        // When & Then
        mockMvc.perform(get("/api/auth/me")
                        .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.userId").value(savedUser.getId()))
                .andExpect(jsonPath("$.settingId").value(savedUser.getSetting().getId()))
                .andExpect(jsonPath("$.socialNickname").value(savedUser.getSocialNickname()))
                .andExpect(jsonPath("$.thumbnailImage").value(savedUser.getThumbnailImage()))
                .andExpect(jsonPath("$.userName").value(savedUser.getUserName()))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("현재 사용자 정보 조회 통합 테스트 - 관리자 사용자")
    void getCurrentUser_AdminUser_IntegrationTest() throws Exception {
        // Given - 관리자 사용자 데이터 저장
        User adminUser = createTestUser("관리자", UserRole.ADMIN);
        User savedAdmin = userRepository.save(adminUser);
        
        CustomUserDetails adminUserDetails = createCustomUserDetails(savedAdmin);

        // When & Then
        mockMvc.perform(get("/api/auth/me")
                        .with(user(adminUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.userId").value(savedAdmin.getId()))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.userName").value("관리자"));
    }

    @Test
    @DisplayName("현재 사용자 정보 조회 - 다양한 소셜 제공자별 통합 테스트")
    void getCurrentUser_VariousProviders_IntegrationTest() throws Exception {
        // Given - NAVER 사용자
        User naverUser = createTestUserWithProvider("네이버사용자", SocialProvider.NAVER);
        User savedNaverUser = userRepository.save(naverUser);
        
        CustomUserDetails naverUserDetails = createCustomUserDetails(savedNaverUser);

        // When & Then
        mockMvc.perform(get("/api/auth/me")
                        .with(user(naverUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("네이버사용자"))
                .andExpect(jsonPath("$.socialNickname").value(containsString("네이버")));
    }

    @Test
    @DisplayName("서버 헬스체크 통합 테스트")
    void healthCheck_IntegrationTest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/auth/health"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("OK"))
                .andExpect(content().contentType("text/plain;charset=UTF-8"));
    }

    @Test
    @DisplayName("인증이 필요한 API에 비인증 접근 - 401 Unauthorized")
    void getCurrentUser_Unauthenticated_IntegrationTest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/auth/me"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("사용자 정보 조회 - 빈 프로필 이미지 처리")
    void getCurrentUser_EmptyThumbnailImage_IntegrationTest() throws Exception {
        // Given - 프로필 이미지가 빈 사용자
        User userWithoutImage = createTestUserWithoutImage("이미지없는사용자");
        User savedUser = userRepository.save(userWithoutImage);
        
        CustomUserDetails userDetails = createCustomUserDetails(savedUser);

        // When & Then
        mockMvc.perform(get("/api/auth/me")
                        .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(savedUser.getId()))
                .andExpect(jsonPath("$.thumbnailImage").value(""))
                .andExpect(jsonPath("$.userName").value("이미지없는사용자"));
    }

    @Test
    @DisplayName("사용자 정보 조회 - 응답 형식 검증")
    void getCurrentUser_ResponseFormat_IntegrationTest() throws Exception {
        // Given
        User testUser = createTestUser("형식검증사용자", UserRole.USER);
        User savedUser = userRepository.save(testUser);
        
        CustomUserDetails userDetails = createCustomUserDetails(savedUser);

        // When & Then
        mockMvc.perform(get("/api/auth/me")
                        .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isMap())
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.settingId").isNumber())
                .andExpect(jsonPath("$.socialNickname").isString())
                .andExpect(jsonPath("$.thumbnailImage").isString())
                .andExpect(jsonPath("$.userName").isString())
                .andExpect(jsonPath("$.role").isString());
    }

    @Test
    @DisplayName("헬스체크 응답 헤더 검증")
    void healthCheck_ResponseHeaders_IntegrationTest() throws Exception {
        // When & Then
        // TODO: Spring Security CSRF 보호로 XSRF-TOKEN 쿠키 자동 생성 (정상 동작)
        // 보안을 위해 CSRF 토큰이 설정되는 것은 올바른 동작
        mockMvc.perform(get("/api/auth/health"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/plain;charset=UTF-8"))
                .andExpect(header().exists("Set-Cookie"));
    }

    @Test
    @DisplayName("존재하지 않는 엔드포인트 - 403 Forbidden")
    void nonExistentEndpoint_Forbidden_IntegrationTest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/auth/nonexistent"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    /**
     * 테스트용 User 엔티티 생성 (기본 KAKAO 제공자)
     */
    private User createTestUser(String userName, UserRole role) {
        return createTestUserWithProvider(userName, role, SocialProvider.KAKAO);
    }

    /**
     * 테스트용 User 엔티티 생성 (소셜 제공자 지정)
     */
    private User createTestUserWithProvider(String userName, SocialProvider provider) {
        return createTestUserWithProvider(userName, UserRole.USER, provider);
    }

    /**
     * 테스트용 User 엔티티 생성 (모든 옵션 지정)
     */
    private User createTestUserWithProvider(String userName, UserRole role, SocialProvider provider) {
        Setting setting = Setting.builder()
                .messageNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(true)
                .build();

        return User.builder()
                .socialId("integration-test-" + userName + "-" + System.currentTimeMillis())
                .socialNickname(userName + "_소셜닉네임_" + provider.name())
                .thumbnailImage("http://example.com/" + userName.toLowerCase() + ".jpg")
                .userName(userName)
                .provider(provider)
                .role(role)
                .setting(setting)
                .build();
    }

    /**
     * 테스트용 User 엔티티 생성 (프로필 이미지 없음)
     */
    private User createTestUserWithoutImage(String userName) {
        Setting setting = Setting.builder()
                .messageNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(true)
                .build();

        return User.builder()
                .socialId("no-image-test-" + System.currentTimeMillis())
                .socialNickname(userName + "_소셜닉네임")
                .thumbnailImage("") // 빈 이미지
                .userName(userName)
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