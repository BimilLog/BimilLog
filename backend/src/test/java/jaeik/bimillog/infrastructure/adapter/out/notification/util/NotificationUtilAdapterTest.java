package jaeik.bimillog.infrastructure.adapter.out.notification.util;

import jaeik.bimillog.BimilLogApplication;
import jaeik.bimillog.domain.notification.entity.FcmToken;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.testutil.TestContainersConfiguration;
import jaeik.bimillog.testutil.TestSettings;
import jaeik.bimillog.testutil.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>NotificationUtilAdapter 통합 테스트</h2>
 * <p>알림 유틸리티 어댑터의 실제 데이터베이스 연동 동작 검증</p>
 * <p>TestContainers를 통한 실제 MySQL 환경에서 알림 자격 확인 및 FCM 토큰 조회 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DataJpaTest(
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = BimilLogApplication.class
        )
)
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({NotificationUtilAdapter.class, TestContainersConfiguration.class})
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create"
})
class NotificationUtilAdapterTest {

    @Autowired
    private NotificationUtilAdapter notificationUtilAdapter;

    @Autowired
    private TestEntityManager testEntityManager;

    private User enabledUser;
    private User disabledUser;
    private Long enabledUserId;
    private Long disabledUserId;

    @BeforeEach
    void setUp() {
        // Given: 알림이 활성화된 사용자 설정
        enabledUser = TestUsers.copyWithId(TestUsers.USER1, null);
        enabledUser = testEntityManager.persistAndFlush(enabledUser);
        enabledUserId = enabledUser.getId();

        // Given: 알림이 비활성화된 사용자 설정
        disabledUser = User.builder()
                .socialId(TestUsers.USER2.getSocialId())
                .provider(TestUsers.USER2.getProvider())
                .userName(TestUsers.USER2.getUserName())
                .socialNickname(TestUsers.USER2.getSocialNickname())
                .role(TestUsers.USER2.getRole())
                .setting(TestSettings.copyWithId(TestSettings.ALL_DISABLED, null))
                .build();
        disabledUser = testEntityManager.persistAndFlush(disabledUser);
        disabledUserId = disabledUser.getId();
    }

    @Test
    @DisplayName("정상 케이스 - PAPER 알림 수신 자격 확인 (활성화된 사용자)")
    @Transactional
    void shouldReturnTrue_WhenUserEligibleForPaperNotification() {
        // When: PAPER 알림 수신 자격 확인
        boolean result = notificationUtilAdapter.SseEligibleForNotification(enabledUserId, NotificationType.PAPER);

        // Then: 활성화된 사용자는 수신 가능해야 함
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("정상 케이스 - COMMENT 알림 수신 자격 확인 (활성화된 사용자)")
    @Transactional
    void shouldReturnTrue_WhenUserEligibleForCommentNotification() {
        // When: COMMENT 알림 수신 자격 확인
        boolean result = notificationUtilAdapter.SseEligibleForNotification(enabledUserId, NotificationType.COMMENT);

        // Then: 활성화된 사용자는 수신 가능해야 함
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("정상 케이스 - POST_FEATURED 알림 수신 자격 확인 (활성화된 사용자)")
    @Transactional
    void shouldReturnTrue_WhenUserEligibleForPostFeaturedNotification() {
        // When: POST_FEATURED 알림 수신 자격 확인
        boolean result = notificationUtilAdapter.SseEligibleForNotification(enabledUserId, NotificationType.POST_FEATURED);

        // Then: 활성화된 사용자는 수신 가능해야 함
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("정상 케이스 - PAPER 알림 수신 자격 확인 (비활성화된 사용자)")
    @Transactional
    void shouldReturnFalse_WhenUserNotEligibleForPaperNotification() {
        // When: PAPER 알림 수신 자격 확인
        boolean result = notificationUtilAdapter.SseEligibleForNotification(disabledUserId, NotificationType.PAPER);

        // Then: 비활성화된 사용자는 수신 불가능해야 함
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("정상 케이스 - ADMIN 알림은 설정과 관계없이 항상 수신 가능")
    @Transactional
    void shouldAlwaysReturnTrue_WhenAdminNotification() {
        // When: ADMIN 알림 수신 자격 확인 (비활성화된 사용자도)
        boolean enabledUserResult = notificationUtilAdapter.SseEligibleForNotification(enabledUserId, NotificationType.ADMIN);
        boolean disabledUserResult = notificationUtilAdapter.SseEligibleForNotification(disabledUserId, NotificationType.ADMIN);

        // Then: 설정과 관계없이 모든 사용자가 수신 가능해야 함
        assertThat(enabledUserResult).isTrue();
        assertThat(disabledUserResult).isTrue();
    }

    @Test
    @DisplayName("정상 케이스 - INITIATE 알림은 설정과 관계없이 항상 수신 가능")
    @Transactional
    void shouldAlwaysReturnTrue_WhenInitiateNotification() {
        // When: INITIATE 알림 수신 자격 확인 (비활성화된 사용자도)
        boolean enabledUserResult = notificationUtilAdapter.SseEligibleForNotification(enabledUserId, NotificationType.INITIATE);
        boolean disabledUserResult = notificationUtilAdapter.SseEligibleForNotification(disabledUserId, NotificationType.INITIATE);

        // Then: 설정과 관계없이 모든 사용자가 수신 가능해야 함
        assertThat(enabledUserResult).isTrue();
        assertThat(disabledUserResult).isTrue();
    }

    @Test
    @DisplayName("정상 케이스 - FCM 토큰 조회 (수신 자격이 있는 사용자)")
    @Transactional
    void shouldReturnFcmTokens_WhenUserEligibleForNotification() {
        // Given: FCM 토큰 생성
        FcmToken fcmToken1 = FcmToken.create(enabledUser, "fcm-token-1");
        FcmToken fcmToken2 = FcmToken.create(enabledUser, "fcm-token-2");
        
        testEntityManager.persistAndFlush(fcmToken1);
        testEntityManager.persistAndFlush(fcmToken2);
        testEntityManager.flush();
        testEntityManager.clear();

        // When: FCM 토큰 조회 (PAPER 알림 타입)
        List<FcmToken> result = notificationUtilAdapter.FcmEligibleFcmTokens(enabledUserId, NotificationType.PAPER);

        // Then: FCM 토큰들이 조회되어야 함
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(FcmToken::getFcmRegistrationToken)
                .containsExactlyInAnyOrder("fcm-token-1", "fcm-token-2");
    }

    @Test
    @DisplayName("정상 케이스 - FCM 토큰 조회 (수신 자격이 없는 사용자)")
    @Transactional
    void shouldReturnEmptyList_WhenUserNotEligibleForNotification() {
        // Given: 비활성화된 사용자의 FCM 토큰 생성
        FcmToken fcmToken = FcmToken.create(disabledUser, "fcm-token-disabled");
        testEntityManager.persistAndFlush(fcmToken);
        testEntityManager.flush();
        testEntityManager.clear();

        // When: FCM 토큰 조회 (PAPER 알림 타입)
        List<FcmToken> result = notificationUtilAdapter.FcmEligibleFcmTokens(disabledUserId, NotificationType.PAPER);

        // Then: 빈 목록이 반환되어야 함 (알림 설정이 비활성화됨)
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("경계 케이스 - 존재하지 않는 사용자 ID")
    @Transactional
    void shouldReturnFalse_WhenUserNotFound() {
        // Given: 존재하지 않는 사용자 ID
        Long nonExistentUserId = 999999L;

        // When: 알림 수신 자격 확인
        boolean result = notificationUtilAdapter.SseEligibleForNotification(nonExistentUserId, NotificationType.PAPER);

        // Then: 수신 불가능해야 함
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("경계 케이스 - FCM 토큰이 없는 사용자")
    @Transactional
    void shouldReturnEmptyList_WhenUserHasNoFcmTokens() {
        // When: FCM 토큰 조회 (토큰이 없는 활성화된 사용자)
        List<FcmToken> result = notificationUtilAdapter.FcmEligibleFcmTokens(enabledUserId, NotificationType.PAPER);

        // Then: 빈 목록이 반환되어야 함
        assertThat(result).isEmpty();
    }
}