package jaeik.bimillog.adapter.in.notification;

import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.member.entity.member.Member;
import jaeik.bimillog.infrastructure.adapter.out.notification.NotificationRepository;
import jaeik.bimillog.testutil.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * <h2>알림 Query 컨트롤러 통합 테스트</h2>
 * <p>@SpringBootTest + H2 인메모리 데이터베이스 환경에서 알림 조회 API를 검증합니다.</p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@ActiveProfiles("h2test")
@Import({H2TestConfiguration.class, TestSocialLoginPortConfig.class})
@DisplayName("알림 Query 컨트롤러 통합 테스트")
@Tag("integration")
class NotificationQueryControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Override
    protected void setUpChild() {
        // 테스트용 알림들 생성
        createTestNotifications();
    }

    @Test
    @DisplayName("로그인된 사용자의 알림 목록 조회 - 성공")
    void getNotifications_AuthenticatedUser_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/notification/list")
                        .with(user(testUserDetails)))
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
        Member memberWithoutNotifications = TestMembers.createUniqueWithPrefix("anotheruser");
        userRepository.save(memberWithoutNotifications);

        // When & Then - otherUser를 사용하는 것이 더 적절하지만, 새 사용자가 필요한 경우
        mockMvc.perform(get("/api/notification/list")
                        .with(user(createCustomUserDetails(memberWithoutNotifications))))
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
        // When & Then
        mockMvc.perform(get("/api/notification/list")
                        .with(user(testUserDetails)))
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
        // When & Then
        mockMvc.perform(get("/api/notification/list")
                        .with(user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.read == true)]").exists())
                .andExpect(jsonPath("$[?(@.read == false)]").exists());
    }


    /**
     * 테스트용 알림들 생성
     */
    private void createTestNotifications() {
        // 다양한 타입의 알림 생성 - TestDataBuilder 사용
        notificationRepository.save(
                NotificationTestDataBuilder.aPaperMessageNotification(testMember)
                        .asUnread()
                        .build()
        );
        notificationRepository.save(
                NotificationTestDataBuilder.aCommentNotification(testMember, 1L)
                        .asRead()
                        .build()
        );
        notificationRepository.save(
                NotificationTestDataBuilder.aLikeNotification(testMember, 2L)
                        .asUnread()
                        .build()
        );
        notificationRepository.save(
                NotificationTestDataBuilder.anAdminNotification(testMember, "공지사항이 등록되었습니다")
                        .asRead()
                        .build()
        );
        notificationRepository.save(
                NotificationTestDataBuilder.aNotification()
                        .withReceiver(testMember)
                        .withType(NotificationType.INITIATE)
                        .withMessage("초기화 알림")
                        .asUnread()
                        .build()
        );
    }
}