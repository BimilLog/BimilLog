package jaeik.bimillog.integration.api.notification;

import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <h2>알림 SSE 컨트롤러 통합 테스트</h2>
 * <p>@SpringBootTest를 사용한 실제 Notification SSE API 통합 테스트</p>
 * <p>TestContainers를 사용하여 실제 MySQL 환경에서 테스트</p>
 * <p>SSE 구독 API 동작을 검증</p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@Import({TestContainersConfiguration.class, TestSocialLoginPortConfig.class})
@Transactional
@DisplayName("알림 SSE 컨트롤러 통합 테스트")
class NotificationSseControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;
    
    @Autowired
    private UserRepository userRepository;
    
    private MockMvc mockMvc;
    private User testUser;
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        
        // 테스트용 사용자 생성
        testUser = createTestUser();
        userRepository.save(testUser);
    }
    
    @Test
    @DisplayName("로그인된 사용자의 SSE 구독 - 성공")
    void subscribe_AuthenticatedUser_Success() throws Exception {
        // Given
        CustomUserDetails userDetails = createUserDetails(testUser);
        
        // When & Then
        mockMvc.perform(get("/api/notification/subscribe")
                .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_EVENT_STREAM_VALUE));
    }
    
    @Test
    @DisplayName("비로그인 사용자의 SSE 구독 - 실패")
    void subscribe_Unauthenticated_Unauthorized() throws Exception {
        // When & Then - 인증 없이 요청
        mockMvc.perform(get("/api/notification/subscribe"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }
    
    @Test
    @DisplayName("잘못된 HTTP 메서드로 SSE 구독 - 실패 (CSRF 보안으로 403)")
    void subscribe_WrongHttpMethod_Forbidden() throws Exception {
        // Given
        CustomUserDetails userDetails = createUserDetails(testUser);
        
        // When & Then - POST 메서드로 요청 (Spring Security CSRF가 먼저 차단)
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/notification/subscribe")
                .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isForbidden()); // CSRF 보안으로 403 반환
    }
    
    @Test
    @DisplayName("다른 사용자들의 동시 SSE 구독 - 성공")
    void subscribe_MultipleUsers_Success() throws Exception {
        // Given - 추가 사용자들 생성
        User user2 = createTestUser("user2", "67890");
        User user3 = createTestUser("user3", "11111");
        userRepository.save(user2);
        userRepository.save(user3);
        
        CustomUserDetails userDetails1 = createUserDetails(testUser);
        CustomUserDetails userDetails2 = createUserDetails(user2);
        CustomUserDetails userDetails3 = createUserDetails(user3);
        
        // When & Then - 각각 구독 가능해야 함
        mockMvc.perform(get("/api/notification/subscribe")
                .with(user(userDetails1)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_EVENT_STREAM_VALUE));
                
        mockMvc.perform(get("/api/notification/subscribe")
                .with(user(userDetails2)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_EVENT_STREAM_VALUE));
                
        mockMvc.perform(get("/api/notification/subscribe")
                .with(user(userDetails3)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_EVENT_STREAM_VALUE));
    }
    
    @Test
    @DisplayName("Accept 헤더 확인 - text/listener-stream")
    void subscribe_CheckAcceptHeader_Success() throws Exception {
        // Given
        CustomUserDetails userDetails = createUserDetails(testUser);
        
        // When & Then
        mockMvc.perform(get("/api/notification/subscribe")
                .header("Accept", "text/listener-stream")
                .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_EVENT_STREAM_VALUE));
    }
    
    /**
     * 테스트용 기본 사용자 생성
     */
    private User createTestUser() {
        return createTestUser("testuser", "12345");
    }
    
    /**
     * 테스트용 사용자 생성 (파라미터 지정)
     */
    private User createTestUser(String userName, String socialId) {
        Setting setting = Setting.builder()
                .messageNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(true)
                .build();
        
        return User.builder()
                .socialId(socialId)
                .socialNickname("테스트사용자")
                .thumbnailImage("test-profile.jpg")
                .userName(userName)
                .provider(SocialProvider.KAKAO)
                .role(UserRole.USER)
                .setting(setting)
                .build();
    }
    
    /**
     * 테스트용 CustomUserDetails 생성
     */
    private CustomUserDetails createUserDetails(User user) {
        UserDTO userDTO = UserDTO.builder()
                .userId(user.getId())
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