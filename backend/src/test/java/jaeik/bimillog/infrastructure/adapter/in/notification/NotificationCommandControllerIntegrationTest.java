package jaeik.bimillog.infrastructure.adapter.in.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.bimillog.domain.notification.entity.Notification;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.domain.user.entity.UserDetail;
import jaeik.bimillog.infrastructure.adapter.in.notification.dto.UpdateNotificationDTO;
import jaeik.bimillog.infrastructure.adapter.out.notification.jpa.NotificationRepository;
import jaeik.bimillog.infrastructure.adapter.out.user.jpa.UserRepository;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
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

import java.util.Arrays;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <h2>알림 Command 컨트롤러 통합 테스트</h2>
 * <p>@SpringBootTest를 사용한 실제 Notification Command API 통합 테스트</p>
 * <p>TestContainers를 사용하여 실제 MySQL 환경에서 테스트</p>
 * <p>알림 업데이트(읽음/삭제) API 동작을 검증</p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@Import({TestContainersConfiguration.class, TestSocialLoginPortConfig.class})
@Transactional
@DisplayName("알림 Command 컨트롤러 통합 테스트")
class NotificationCommandControllerIntegrationTest {

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
    private List<Notification> testNotifications;

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
        testNotifications = createTestNotifications();
    }

    @Test
    @DisplayName("알림 읽음 처리 - 성공")
    void markAsRead_Success() throws Exception {
        // Given
        CustomUserDetails userDetails = createUserDetails(testUser);
        List<Long> readIds = Arrays.asList(testNotifications.get(0).getId(), testNotifications.get(1).getId());

        UpdateNotificationDTO updateDTO = new UpdateNotificationDTO();
        updateDTO.setReadIds(readIds);
        updateDTO.setDeletedIds(Arrays.asList());

        String requestBody = objectMapper.writeValueAsString(updateDTO);

        // When & Then
        mockMvc.perform(post("/api/notification/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(user(userDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("알림 삭제 처리 - 성공")
    void deleteNotifications_Success() throws Exception {
        // Given
        CustomUserDetails userDetails = createUserDetails(testUser);
        List<Long> deletedIds = Arrays.asList(testNotifications.get(2).getId(), testNotifications.get(3).getId());

        UpdateNotificationDTO updateDTO = new UpdateNotificationDTO();
        updateDTO.setReadIds(Arrays.asList());
        updateDTO.setDeletedIds(deletedIds);

        String requestBody = objectMapper.writeValueAsString(updateDTO);

        // When & Then
        mockMvc.perform(post("/api/notification/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(user(userDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("알림 읽음과 삭제 동시 처리 - 성공")
    void markAsReadAndDelete_Success() throws Exception {
        // Given
        CustomUserDetails userDetails = createUserDetails(testUser);
        List<Long> readIds = Arrays.asList(testNotifications.get(0).getId());
        List<Long> deletedIds = Arrays.asList(testNotifications.get(4).getId());

        UpdateNotificationDTO updateDTO = new UpdateNotificationDTO();
        updateDTO.setReadIds(readIds);
        updateDTO.setDeletedIds(deletedIds);

        String requestBody = objectMapper.writeValueAsString(updateDTO);

        // When & Then
        mockMvc.perform(post("/api/notification/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(user(userDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("빈 리스트로 알림 업데이트 - 성공")
    void updateWithEmptyLists_Success() throws Exception {
        // Given
        CustomUserDetails userDetails = createUserDetails(testUser);

        UpdateNotificationDTO updateDTO = new UpdateNotificationDTO();
        updateDTO.setReadIds(Arrays.asList());
        updateDTO.setDeletedIds(Arrays.asList());

        String requestBody = objectMapper.writeValueAsString(updateDTO);

        // When & Then
        mockMvc.perform(post("/api/notification/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(user(userDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("존재하지 않는 알림 ID로 업데이트 - 성공 (무시됨)")
    void updateWithNonExistentIds_Success() throws Exception {
        // Given
        CustomUserDetails userDetails = createUserDetails(testUser);
        List<Long> nonExistentIds = Arrays.asList(999999L, 999998L);

        UpdateNotificationDTO updateDTO = new UpdateNotificationDTO();
        updateDTO.setReadIds(nonExistentIds);
        updateDTO.setDeletedIds(Arrays.asList());

        String requestBody = objectMapper.writeValueAsString(updateDTO);

        // When & Then
        mockMvc.perform(post("/api/notification/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(user(userDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("비로그인 사용자의 알림 업데이트 - 실패")
    void updateNotifications_Unauthenticated_Forbidden() throws Exception {
        // Given
        UpdateNotificationDTO updateDTO = new UpdateNotificationDTO();
        updateDTO.setReadIds(Arrays.asList(1L));
        updateDTO.setDeletedIds(Arrays.asList());

        String requestBody = objectMapper.writeValueAsString(updateDTO);

        // When & Then
        mockMvc.perform(post("/api/notification/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("잘못된 JSON 형식으로 알림 업데이트 - 실패")
    void updateWithInvalidJson_BadRequest() throws Exception {
        // Given
        CustomUserDetails userDetails = createUserDetails(testUser);
        String invalidJson = "{ invalid json }";

        // When & Then
        mockMvc.perform(post("/api/notification/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson)
                        .with(user(userDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().is5xxServerError());
    }

    /**
     * 테스트용 사용자 생성
     */
    private User createTestUser() {
        Setting setting = Setting.builder()
                .messageNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(true)
                .build();

        return User.builder()
                .socialId("12345")
                .socialNickname("테스트사용자")
                .thumbnailImage("test-profile.jpg")
                .userName("testuser")
                .provider(SocialProvider.KAKAO)
                .role(UserRole.USER)
                .setting(setting)
                .build();
    }

    /**
     * 테스트용 알림들 생성
     */
    private List<Notification> createTestNotifications() {
        List<Notification> notifications = Arrays.asList(
                createNotification("새 메시지가 도착했습니다", NotificationType.PAPER, false, "/paper/1"),
                createNotification("새 댓글이 작성되었습니다", NotificationType.COMMENT, true, "/post/1"),
                createNotification("게시글이 추천되었습니다", NotificationType.POST_FEATURED, false, "/post/2"),
                createNotification("공지사항이 등록되었습니다", NotificationType.ADMIN, true, "/notice/1"),
                createNotification("초기화 알림", NotificationType.INITIATE, false, "/system")
        );

        return notificationRepository.saveAll(notifications);
    }

    /**
     * 개별 알림 생성
     */
    private Notification createNotification(String content, NotificationType type, boolean isRead, String url) {
        return Notification.builder()
                .content(content)
                .notificationType(type)
                .isRead(isRead)
                .url(url)
                .users(testUser)
                .build();
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