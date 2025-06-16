package jaeik.growfarm.integration;

import jaeik.growfarm.controller.NotificationController;
import jaeik.growfarm.dto.notification.NotificationDTO;
import jaeik.growfarm.dto.notification.UpdateNotificationDTO;
import jaeik.growfarm.entity.notification.DeviceType;
import jaeik.growfarm.entity.notification.Notification;
import jaeik.growfarm.entity.notification.NotificationType;
import jaeik.growfarm.entity.user.Setting;
import jaeik.growfarm.entity.user.Token;
import jaeik.growfarm.entity.user.UserRole;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.repository.notification.NotificationRepository;
import jaeik.growfarm.repository.user.SettingRepository;
import jaeik.growfarm.repository.token.TokenRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jaeik.growfarm.service.notification.NotificationService;
import jaeik.growfarm.util.UserUtil;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.TestConstructor;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

/**
 * <h2>NotificationController 통합 테스트</h2>
 * <p>실제 데이터베이스와 서비스를 사용하여 NotificationController의 전체 API를 테스트합니다.</p>
 * <p>SSE 관련 기능은 테스트에서 제외함.</p>
 * @since 2025.05.17
 */
@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(PER_CLASS)
@Commit
@Transactional
public class NotificationControllerIntegrationTest {

    private final NotificationController notificationController;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final SettingRepository settingRepository;
    private final TokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final UserUtil userUtil;

    private Users testUser;
    private Notification testNotification;

    public NotificationControllerIntegrationTest(NotificationController notificationController,
                                                NotificationService notificationService,
                                                NotificationRepository notificationRepository,
                                                SettingRepository settingRepository,
                                                TokenRepository tokenRepository,
                                                UserRepository userRepository,
                                                UserUtil userUtil) {
        this.notificationController = notificationController;
        this.notificationService = notificationService;
        this.notificationRepository = notificationRepository;
        this.settingRepository = settingRepository;
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.userUtil = userUtil;
    }

    /**
     * <h3>테스트 데이터 초기화</h3>
     * 사용자, 알림 데이터 생성
     *
     * @since 2025.05.17
     */
    @BeforeAll
    void setUp() {
        // 사용자 설정 생성
        Setting setting = Setting.builder()
                .farmNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(true)
                .commentFeaturedNotification(true)
                .build();
        settingRepository.save(setting);

        // 토큰 생성
        Token token = Token.builder()
                .jwtRefreshToken("testRefreshToken")
                .kakaoAccessToken("testKakaoAccessToken")
                .kakaoRefreshToken("testKakaoRefreshToken")
                .build();
        tokenRepository.save(token);

        // 사용자 생성
        Users user = Users.builder()
                .kakaoId(1234567890L)
                .kakaoNickname("testNickname")
                .thumbnailImage("testImage")
                .userName("testFarm")
                .role(UserRole.USER)
                .setting(setting)
                .token(token)
                .build();
        testUser = userRepository.save(user);

        // 알림 생성
        Notification notification = Notification.builder()
                .users(testUser)
                .data("Test Notification Content")
                .notificationType(NotificationType.COMMENT)
                .isRead(false)
                .build();
        testNotification = notificationRepository.save(notification);
    }

    /**
     * <h3>알림 리스트 조회 통합 테스트</h3>
     * @since 2025.05.17
     */
    @Test
    @DisplayName("알림 리스트 조회 통합 테스트")
    void testGetNotifications() {
        // 인증 설정
        CustomUserDetails userDetails = new CustomUserDetails(userUtil.UserToDTO(testUser));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );

        // When
        ResponseEntity<List<NotificationDTO>> response = notificationController.getNotifications(userDetails);

        // Then
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(testNotification.getData(), response.getBody().get(0).getData());
    }

    /**
     * <h3>알림 읽음, 삭제 처리 통합 테스트</h3>
     * @since 2025.05.17
     */
    @Test
    @DisplayName("알림 읽음, 삭제 처리 통합 테스트")
    void testMarkAsRead() {
        // Given
        UpdateNotificationDTO updateNotificationDTO = new UpdateNotificationDTO();
        updateNotificationDTO.setReadIds(Arrays.asList(testNotification.getId()));
        updateNotificationDTO.setDeletedIds(List.of());

        // 인증 설정
        CustomUserDetails userDetails = new CustomUserDetails(userUtil.UserToDTO(testUser));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );

        // When
        ResponseEntity<Void> response = notificationController.markAsRead(userDetails, updateNotificationDTO);

        // Then
        assertEquals(200, response.getStatusCodeValue());
    }

    /**
     * <h3>FCM 토큰 등록 통합 테스트</h3>
     * @since 2025.05.17
     */
    @Test
    @DisplayName("FCM 토큰 등록 통합 테스트")
    void testRegisterFcmToken() {
        // Given
        String fcmToken = "test-fcm-token";
        DeviceType deviceType = DeviceType.MOBILE;

        // 인증 설정
        CustomUserDetails userDetails = new CustomUserDetails(userUtil.UserToDTO(testUser));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );

        // When
        ResponseEntity<String> response = notificationController.registerFcmToken(userDetails, fcmToken, deviceType);

        // Then
        assertEquals(200, response.getStatusCodeValue());
    }
}
