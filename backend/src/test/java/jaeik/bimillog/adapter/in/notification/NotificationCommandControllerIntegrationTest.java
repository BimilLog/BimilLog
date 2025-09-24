package jaeik.bimillog.adapter.in.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.bimillog.domain.notification.entity.Notification;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.adapter.in.notification.dto.UpdateNotificationDTO;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
import jaeik.bimillog.infrastructure.adapter.out.notification.jpa.NotificationRepository;
import jaeik.bimillog.infrastructure.adapter.out.user.jpa.UserRepository;
import jaeik.bimillog.testutil.*;
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
        testUser = TestUsers.createUnique();
        userRepository.save(testUser);

        // 테스트용 알림들 생성
        testNotifications = createTestNotifications();
    }

    @Test
    @DisplayName("알림 읽음 처리 - 성공")
    void markAsRead_Success() throws Exception {
        // Given
        CustomUserDetails userDetails = TestFixtures.createCustomUserDetails(testUser);
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
        CustomUserDetails userDetails = TestFixtures.createCustomUserDetails(testUser);
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
        CustomUserDetails userDetails = TestFixtures.createCustomUserDetails(testUser);
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
        CustomUserDetails userDetails = TestFixtures.createCustomUserDetails(testUser);

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
        CustomUserDetails userDetails = TestFixtures.createCustomUserDetails(testUser);
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
        CustomUserDetails userDetails = TestFixtures.createCustomUserDetails(testUser);
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
     * 테스트용 알림들 생성
     */
    private List<Notification> createTestNotifications() {
        List<Notification> notifications = Arrays.asList(
                NotificationTestDataBuilder.aPaperMessageNotification(testUser)
                        .asUnread()
                        .build(),
                NotificationTestDataBuilder.aCommentNotification(testUser, 1L)
                        .asRead()
                        .build(),
                NotificationTestDataBuilder.aLikeNotification(testUser, 2L)
                        .asUnread()
                        .build(),
                NotificationTestDataBuilder.anAdminNotification(testUser, "공지사항이 등록되었습니다")
                        .asRead()
                        .build(),
                NotificationTestDataBuilder.aNotification()
                        .withReceiver(testUser)
                        .withType(NotificationType.INITIATE)
                        .withMessage("초기화 알림")
                        .asUnread()
                        .build()
        );

        return notificationRepository.saveAll(notifications);
    }
}