package jaeik.bimillog.integration.api.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.notification.entity.Notification;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.global.entity.UserDetail;
import jaeik.bimillog.infrastructure.adapter.notification.out.persistence.notification.NotificationRepository;
import jaeik.bimillog.infrastructure.adapter.user.out.persistence.user.user.UserRepository;
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
 * <h2>알림 Query 컨트롤러 통합 테스트</h2>
 * <p>@SpringBootTest를 사용한 실제 Notification Query API 통합 테스트</p>
 * <p>TestContainers를 사용하여 실제 MySQL 환경에서 테스트</p>
 * <p>알림 조회 API 동작을 검증</p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@Import({TestContainersConfiguration.class, TestSocialLoginPortConfig.class})
@Transactional
@DisplayName("알림 Query 컨트롤러 통합 테스트")
class NotificationQueryControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

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

        // 테스트용 알림들 생성
        createTestNotifications();
    }

    @Test
    @DisplayName("로그인된 사용자의 알림 목록 조회 - 성공")
    void getNotifications_AuthenticatedUser_Success() throws Exception {
        // Given
        CustomUserDetails userDetails = createUserDetails(testUser);

        // When & Then
        mockMvc.perform(get("/api/notification/list")
                        .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].content").exists())
                .andExpect(jsonPath("$[0].notificationType").exists())
                .andExpect(jsonPath("$[0].read").exists())
                .andExpect(jsonPath("$[0].createdAt").exists());
    }

    @Test
    @DisplayName("로그인된 사용자의 알림 목록 조회 - 알림 없음")
    void getNotifications_NoNotifications_Success() throws Exception {
        // Given - 알림이 없는 새로운 사용자
        User userWithoutNotifications = createTestUser("anotheruser", "another123");
        userRepository.save(userWithoutNotifications);

        CustomUserDetails userDetails = createUserDetails(userWithoutNotifications);

        // When & Then
        mockMvc.perform(get("/api/notification/list")
                        .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("비로그인 사용자의 알림 목록 조회 - 실패")
    void getNotifications_Unauthenticated_Forbidden() throws Exception {
        // When & Then - 인증 없이 요청
        mockMvc.perform(get("/api/notification/list"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("다양한 알림 타입 조회 - 성공")
    void getNotifications_VariousNotificationTypes_Success() throws Exception {
        // Given
        CustomUserDetails userDetails = createUserDetails(testUser);

        // When & Then
        mockMvc.perform(get("/api/notification/list")
                        .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.notificationType == 'PAPER')]").exists())
                .andExpect(jsonPath("$[?(@.notificationType == 'COMMENT')]").exists())
                .andExpect(jsonPath("$[?(@.notificationType == 'POST_FEATURED')]").exists())
                .andExpect(jsonPath("$[?(@.notificationType == 'ADMIN')]").exists())
                .andExpect(jsonPath("$[?(@.notificationType == 'INITIATE')]").exists());
    }

    @Test
    @DisplayName("읽음/안읽음 상태 확인 - 성공")
    void getNotifications_ReadStatusCheck_Success() throws Exception {
        // Given
        CustomUserDetails userDetails = createUserDetails(testUser);

        // When & Then
        mockMvc.perform(get("/api/notification/list")
                        .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.read == true)]").exists())
                .andExpect(jsonPath("$[?(@.read == false)]").exists());
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
     * 테스트용 알림들 생성
     */
    private void createTestNotifications() {
        // 다양한 타입의 알림 생성
        createNotification("새 메시지가 도착했습니다", NotificationType.PAPER, false, "/paper/1");
        createNotification("새 댓글이 작성되었습니다", NotificationType.COMMENT, true, "/post/1");
        createNotification("게시글이 추천되었습니다", NotificationType.POST_FEATURED, false, "/post/2");
        createNotification("공지사항이 등록되었습니다", NotificationType.ADMIN, true, "/notice/1");
        createNotification("초기화 알림", NotificationType.INITIATE, false, "/system");
    }

    /**
     * 개별 알림 생성
     */
    private void createNotification(String content, NotificationType type, boolean isRead, String url) {
        Notification notification = Notification.builder()
                .content(content)
                .notificationType(type)
                .isRead(isRead)
                .url(url)
                .users(testUser)
                .build();
        notificationRepository.save(notification);
    }

    /**
     * 테스트용 CustomUserDetails 생성
     */
    private CustomUserDetails createUserDetails(User user) {
        UserDetail userDetail = UserDetail.builder()
                .userId(user.getId())
                .socialId(user.getSocialId())
                .socialNickname(user.getSocialNickname())
                .thumbnailImage(user.getThumbnailImage())
                .userName(user.getUserName())
                .provider(user.getProvider())
                .role(user.getRole())
                .build();

        return new CustomUserDetails(userDetail);
    }
}