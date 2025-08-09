package jaeik.growfarm.integration.api.notification;

import jaeik.growfarm.controller.NotificationController;
import jaeik.growfarm.dto.notification.NotificationDTO;
import jaeik.growfarm.dto.notification.UpdateNotificationDTO;
import jaeik.growfarm.dto.user.ClientDTO;
import jaeik.growfarm.entity.notification.Notification;
import jaeik.growfarm.entity.notification.NotificationType;
import jaeik.growfarm.entity.user.Setting;
import jaeik.growfarm.entity.user.Token;
import jaeik.growfarm.entity.user.UserRole;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.repository.notification.NotificationRepository;
import jaeik.growfarm.repository.token.TokenRepository;
import jaeik.growfarm.repository.user.SettingRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestConstructor;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

/**
 * <h2>NotificationController 통합 테스트</h2>
 * <p>
 * 실제 데이터베이스와 서비스를 사용하여 NotificationController의 전체 API를 테스트합니다.
 * </p>
 * @version 1.0.0
 * @author Jaeik
 */
@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(PER_CLASS)
@Transactional
public class NotificationControllerIntegrationTest {

        private final NotificationController notificationController;
        private final NotificationRepository notificationRepository;
        private final SettingRepository settingRepository;
        private final TokenRepository tokenRepository;
        private final UserRepository userRepository;
        private final Random random = new Random();

        private Notification testNotification;
        private CustomUserDetails userDetails;

        public NotificationControllerIntegrationTest(NotificationController notificationController,
                        NotificationRepository notificationRepository,
                        SettingRepository settingRepository,
                        TokenRepository tokenRepository,
                        UserRepository userRepository) {
                this.notificationController = notificationController;
                this.notificationRepository = notificationRepository;
                this.settingRepository = settingRepository;
                this.tokenRepository = tokenRepository;
                this.userRepository = userRepository;
        }

        @BeforeAll
        void setUp() {
                // 고유한 값 생성을 위한 랜덤 값
                int uniqueId = random.nextInt(1000000);
                long timestamp = System.currentTimeMillis();

                // 사용자 설정 생성
                Setting setting = Setting.builder()
                                .messageNotification(true)
                                .commentNotification(true)
                                .postFeaturedNotification(true)
                                .build();
                settingRepository.save(setting);

                // 사용자 생성 (고유한 값 사용)
                Users user = Users.builder()
                                .kakaoId(timestamp + uniqueId)
                                .kakaoNickname("testNickname" + uniqueId)
                                .thumbnailImage("testImage")
                                .userName("testUser" + uniqueId)
                                .role(UserRole.USER)
                                .setting(setting)
                                .build();
                Users testUser = userRepository.save(user);

                // 토큰 생성
                Token token = Token.builder()
                                .users(testUser)
                                .jwtRefreshToken("testRefreshToken" + uniqueId)
                                .kakaoAccessToken("testKakaoAccessToken" + uniqueId)
                                .kakaoRefreshToken("testKakaoRefreshToken" + uniqueId)
                                .build();
                tokenRepository.save(token);

                // 알림 생성
                Notification notification = Notification.builder()
                                .data("Test Notification " + uniqueId)
                                .notificationType(NotificationType.COMMENT)
                                .users(testUser)
                                .isRead(false)
                                .build();
                testNotification = notificationRepository.save(notification);

                // ClientDTO 생성
                ClientDTO clientDTO = new ClientDTO(testUser, token.getId(), null);
                userDetails = new CustomUserDetails(clientDTO);
        }

        @Test
        @DisplayName("알림 목록 조회 통합 테스트")
        void testGetNotifications() {
                // Given
                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(userDetails, null,
                                                userDetails.getAuthorities()));

                // When
                ResponseEntity<List<NotificationDTO>> response = notificationController.getNotifications(userDetails);

                // Then
                assertEquals(200, response.getStatusCodeValue());
                assertNotNull(response.getBody());
        }

        @Test
        @DisplayName("알림 읽음 처리 통합 테스트")
        void testMarkAsRead() {
                // Given
                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(userDetails, null,
                                                userDetails.getAuthorities()));

                UpdateNotificationDTO updateDTO = new UpdateNotificationDTO();
                updateDTO.setReadIds(List.of(testNotification.getId()));
                updateDTO.setDeletedIds(List.of());

                // When
                ResponseEntity<Void> response = notificationController.markAsRead(userDetails, updateDTO);

                // Then
                assertEquals(200, response.getStatusCodeValue());
        }
}