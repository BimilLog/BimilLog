package jaeik.bimillog.adapter.in.notification;

import jaeik.bimillog.domain.notification.entity.Notification;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.infrastructure.adapter.in.notification.dto.UpdateNotificationDTO;
import jaeik.bimillog.infrastructure.adapter.out.notification.jpa.NotificationRepository;
import jaeik.bimillog.testutil.BaseIntegrationTest;
import jaeik.bimillog.testutil.H2TestConfiguration;
import jaeik.bimillog.testutil.NotificationTestDataBuilder;
import jaeik.bimillog.testutil.TestSocialLoginPortConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <h2>알림 Command 컨트롤러 통합 테스트</h2>
 * <p>@SpringBootTest + H2 인메모리 데이터베이스 환경에서 알림 명령 API를 검증합니다.</p>
 * <p>알림 업데이트(읽음/삭제) 시나리오를 집중적으로 테스트합니다.</p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@ActiveProfiles("h2test")
@Import({H2TestConfiguration.class, TestSocialLoginPortConfig.class})
@DisplayName("알림 Command 컨트롤러 통합 테스트")
@Tag("integration")
class NotificationCommandControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private NotificationRepository notificationRepository;

    private List<Notification> testNotifications;

    @Override
    protected void setUpChild() {
        // 테스트용 알림들 생성
        testNotifications = createTestNotifications();
    }

    @Test
    @DisplayName("알림 읽음 처리 - 성공")
    void markAsRead_Success() throws Exception {
        // Given
        List<Long> readIds = Arrays.asList(testNotifications.get(0).getId(), testNotifications.get(1).getId());

        UpdateNotificationDTO updateDTO = new UpdateNotificationDTO();
        updateDTO.setReadIds(readIds);
        updateDTO.setDeletedIds(List.of());

        String requestBody = objectMapper.writeValueAsString(updateDTO);

        // When & Then
        mockMvc.perform(post("/api/notification/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(user(testUserDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("알림 삭제 처리 - 성공")
    void deleteNotifications_Success() throws Exception {
        // Given
        List<Long> deletedIds = Arrays.asList(testNotifications.get(2).getId(), testNotifications.get(3).getId());

        UpdateNotificationDTO updateDTO = new UpdateNotificationDTO();
        updateDTO.setReadIds(List.of());
        updateDTO.setDeletedIds(deletedIds);

        String requestBody = objectMapper.writeValueAsString(updateDTO);

        // When & Then
        mockMvc.perform(post("/api/notification/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(user(testUserDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("알림 읽음과 삭제 동시 처리 - 성공")
    void markAsReadAndDelete_Success() throws Exception {
        // Given
        List<Long> readIds = Collections.singletonList(testNotifications.get(0).getId());
        List<Long> deletedIds = Collections.singletonList(testNotifications.get(4).getId());

        UpdateNotificationDTO updateDTO = new UpdateNotificationDTO();
        updateDTO.setReadIds(readIds);
        updateDTO.setDeletedIds(deletedIds);

        String requestBody = objectMapper.writeValueAsString(updateDTO);

        // When & Then
        mockMvc.perform(post("/api/notification/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(user(testUserDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("빈 리스트로 알림 업데이트 - 실패 (최소 1개 작업 필요)")
    void updateWithEmptyLists_FailOnValidation() throws Exception {
        // Given
        UpdateNotificationDTO updateDTO = new UpdateNotificationDTO();
        updateDTO.setReadIds(List.of());
        updateDTO.setDeletedIds(List.of());

        String requestBody = objectMapper.writeValueAsString(updateDTO);

        // When & Then
        mockMvc.perform(post("/api/notification/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(user(testUserDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("존재하지 않는 알림 ID로 업데이트 - 성공 (무시됨)")
    void updateWithNonExistentIds_Success() throws Exception {
        // Given
        List<Long> nonExistentIds = Arrays.asList(999999L, 999998L);

        UpdateNotificationDTO updateDTO = new UpdateNotificationDTO();
        updateDTO.setReadIds(nonExistentIds);
        updateDTO.setDeletedIds(List.of());

        String requestBody = objectMapper.writeValueAsString(updateDTO);

        // When & Then
        mockMvc.perform(post("/api/notification/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(user(testUserDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("비로그인 사용자의 알림 업데이트 - 실패")
    void updateNotifications_Unauthenticated_Forbidden() throws Exception {
        // Given
        UpdateNotificationDTO updateDTO = new UpdateNotificationDTO();
        updateDTO.setReadIds(List.of(1L));
        updateDTO.setDeletedIds(List.of());

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
        String invalidJson = "{ invalid json }";

        // When & Then
        mockMvc.perform(post("/api/notification/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson)
                        .with(user(testUserDetails))
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