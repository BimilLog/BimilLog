package jaeik.bimillog.integration.api.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.Token;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.user.out.persistence.user.token.TokenRepository;
import jaeik.bimillog.infrastructure.adapter.user.out.persistence.user.user.UserRepository;
import jaeik.bimillog.global.dto.UserDTO;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * <h2>사용자 조회 컨트롤러 통합 테스트</h2>
 * <p>@SpringBootTest를 사용한 실제 User Query API 통합 테스트</p>
 * <p>TestContainers를 사용하여 실제 MySQL 환경에서 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@Import({TestContainersConfiguration.class, TestSocialLoginPortConfig.class})
@Transactional
@DisplayName("사용자 조회 컨트롤러 통합 테스트")
class UserQueryControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenRepository tokenRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("닉네임 중복 확인 통합 테스트 - 사용 가능한 닉네임")
    void checkUserName_Available_IntegrationTest() throws Exception {
        // Given
        String availableUserName = "사용가능한닉네임";

        // When & Then
        mockMvc.perform(get("/api/user/username/check")
                        .param("userName", availableUserName))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("닉네임 중복 확인 통합 테스트 - 중복된 닉네임")
    void checkUserName_Duplicate_IntegrationTest() throws Exception {
        // Given - 기존 사용자 생성 및 저장
        User existingUser = createTestUser();
        userRepository.save(existingUser);

        // When & Then
        mockMvc.perform(get("/api/user/username/check")
                        .param("userName", existingUser.getUserName()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    @DisplayName("사용자 설정 조회 통합 테스트 - 성공")
    void getSetting_IntegrationTest_Success() throws Exception {
        // Given - 테스트 사용자 생성 및 저장
        User testUser = createTestUser();
        userRepository.save(testUser);
        
        CustomUserDetails userDetails = createCustomUserDetails(testUser);

        // When & Then
        mockMvc.perform(get("/api/user/setting")
                        .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.messageNotification").value(true))
                .andExpect(jsonPath("$.commentNotification").value(true))
                .andExpect(jsonPath("$.postFeaturedNotification").value(true));
    }

    @Test
    @DisplayName("사용자 작성 게시글 목록 조회 통합 테스트 - 성공")
    void getUserPosts_IntegrationTest_Success() throws Exception {
        // Given - 테스트 사용자 생성 및 저장
        User testUser = createTestUser();
        userRepository.save(testUser);
        
        CustomUserDetails userDetails = createCustomUserDetails(testUser);

        // When & Then
        mockMvc.perform(get("/api/user/posts")
                        .param("page", "0")
                        .param("size", "10")
                        .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber());
    }

    @Test
    @DisplayName("사용자 추천한 게시글 목록 조회 통합 테스트 - 성공")
    void getUserLikedPosts_IntegrationTest_Success() throws Exception {
        // Given - 테스트 사용자 생성 및 저장
        User testUser = createTestUser();
        userRepository.save(testUser);
        
        CustomUserDetails userDetails = createCustomUserDetails(testUser);

        // When & Then
        mockMvc.perform(get("/api/user/likeposts")
                        .param("page", "0")
                        .param("size", "10")
                        .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber());
    }

    @Test
    @DisplayName("사용자 작성 댓글 목록 조회 통합 테스트 - 성공")
    void getUserComments_IntegrationTest_Success() throws Exception {
        // Given - 테스트 사용자 생성 및 저장
        User testUser = createTestUser();
        userRepository.save(testUser);
        
        CustomUserDetails userDetails = createCustomUserDetails(testUser);

        // When & Then
        mockMvc.perform(get("/api/user/comments")
                        .param("page", "0")
                        .param("size", "10")
                        .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber());
    }

    @Test
    @DisplayName("사용자 추천한 댓글 목록 조회 통합 테스트 - 성공")
    void getUserLikedComments_IntegrationTest_Success() throws Exception {
        // Given - 테스트 사용자 생성 및 저장
        User testUser = createTestUser();
        userRepository.save(testUser);
        
        CustomUserDetails userDetails = createCustomUserDetails(testUser);

        // When & Then
        mockMvc.perform(get("/api/user/likecomments")
                        .param("page", "0")
                        .param("size", "10")
                        .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber());
    }


    @Test
    @DisplayName("사용자 설정 조회 - 인증되지 않은 사용자 - 403 Forbidden")
    void getSetting_Unauthenticated_Forbidden() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/user/setting"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("사용자 게시글 조회 - 인증되지 않은 사용자 - 403 Forbidden")
    void getUserPosts_Unauthenticated_Forbidden() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/user/posts")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }


    @Test
    @DisplayName("닉네임 중복 확인 - 파라미터 누락 - 500 Internal Server Error")
    void checkUserName_MissingParameter_InternalServerError() throws Exception {
        // When & Then - @RequestParam 필수 파라미터 누락시 GlobalExceptionHandler에서 500 처리
        mockMvc.perform(get("/api/user/username/check"))
                .andDo(print())
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("카카오 친구 목록 조회 - 정상 케이스 (Mock 환경)")
    void getKakaoFriendList_Success() throws Exception {
        // Given - 테스트용 사용자 생성 및 저장
        User user = createTestUser();
        User savedUser = userRepository.save(user);
        
        // 테스트용 토큰 생성 및 저장
        Token token = Token.createToken("test-access-token", "test-refresh-token", savedUser);
        Token savedToken = tokenRepository.save(token);
        
        CustomUserDetails userDetails = createCustomUserDetailsWithToken(savedUser, savedToken.getId());

        // When & Then
        mockMvc.perform(get("/api/user/friendlist")
                        .param("offset", "0")
                        .param("limit", "10")
                        .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk());
                // Note: 실제 카카오 API 호출은 Mock으로 처리되므로 응답 구조 검증은 제한적
    }

    @Test
    @DisplayName("카카오 친구 목록 조회 - 인증되지 않은 사용자 - 403 Forbidden")
    void getKakaoFriendList_Unauthenticated_Forbidden() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/user/friendlist")
                        .param("offset", "0")
                        .param("limit", "10"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("카카오 친구 목록 조회 - 기본 파라미터 사용")
    void getKakaoFriendList_WithDefaultParameters() throws Exception {
        // Given
        User user = createTestUser();
        User savedUser = userRepository.save(user);
        
        // 테스트용 토큰 생성 및 저장
        Token token = Token.createToken("test-access-token", "test-refresh-token", savedUser);
        Token savedToken = tokenRepository.save(token);
        
        CustomUserDetails userDetails = createCustomUserDetailsWithToken(savedUser, savedToken.getId());

        // When & Then - offset과 limit을 지정하지 않으면 기본값 사용
        mockMvc.perform(get("/api/user/friendlist")
                        .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("카카오 친구 목록 조회 - 비동기 응답 처리")
    void getKakaoFriendList_AsyncResponse() throws Exception {
        // Given
        User user = createTestUser();
        User savedUser = userRepository.save(user);
        
        // 테스트용 토큰 생성 및 저장
        Token token = Token.createToken("test-access-token", "test-refresh-token", savedUser);
        Token savedToken = tokenRepository.save(token);
        
        CustomUserDetails userDetails = createCustomUserDetailsWithToken(savedUser, savedToken.getId());

        // When & Then - Mono 응답이 정상적으로 처리되는지 확인
        mockMvc.perform(get("/api/user/friendlist")
                        .param("offset", "0")
                        .param("limit", "3")
                        .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk());
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
                .tokenId(12345L) // 카카오 친구 목록 API용
                .build();

        return new CustomUserDetails(userDTO);
    }

    /**
     * 테스트용 CustomUserDetails 생성 (토큰 ID 지정)
     */
    private CustomUserDetails createCustomUserDetailsWithToken(User user, Long tokenId) {
        UserDTO userDTO = UserDTO.builder()
                .userId(user.getId())
                .settingId(user.getSetting().getId())
                .socialId(user.getSocialId())
                .socialNickname(user.getSocialNickname())
                .thumbnailImage(user.getThumbnailImage())
                .userName(user.getUserName())
                .provider(user.getProvider())
                .role(user.getRole())
                .tokenId(tokenId)
                .build();

        return new CustomUserDetails(userDTO);
    }
}