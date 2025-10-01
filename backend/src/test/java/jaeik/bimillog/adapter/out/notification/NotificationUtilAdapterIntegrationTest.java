package jaeik.bimillog.adapter.out.notification;

import jaeik.bimillog.BimilLogApplication;
import jaeik.bimillog.domain.notification.entity.FcmToken;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.member.entity.Setting;
import jaeik.bimillog.domain.member.entity.member.Member;
import jaeik.bimillog.infrastructure.adapter.out.notification.NotificationUtilAdapter;
import jaeik.bimillog.testutil.H2TestConfiguration;
import jaeik.bimillog.testutil.TestMembers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>NotificationUtilAdapter 통합 테스트</h2>
 * <p>알림 유틸리티 어댑터의 데이터베이스 연동 동작 검증</p>
 * <p>H2 데이터베이스를 사용하여 알림 자격 확인 및 FCM 토큰 조회 테스트</p>
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
@ActiveProfiles("h2test")
@Import({NotificationUtilAdapter.class, H2TestConfiguration.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("integration")
class NotificationUtilAdapterIntegrationTest {

    @Autowired
    private NotificationUtilAdapter notificationUtilAdapter;

    @Autowired
    private TestEntityManager testEntityManager;

    private Member enabledMember;
    private Member disabledMember;
    private Long enabledMemberId;
    private Long disabledMemberId;

    @BeforeEach
    void setUp() {
        // Given: 알림이 활성화된 사용자 설정
        enabledMember = TestMembers.copyWithId(TestMembers.MEMBER_1, null);
        enabledMember = testEntityManager.persistAndFlush(enabledMember);
        enabledMemberId = enabledMember.getId();

        // Given: 알림이 비활성화된 사용자 설정
        Setting disabledSetting = TestMembers.createAllDisabledSetting();
        disabledSetting = testEntityManager.persistAndFlush(disabledSetting);

        Member sourceMember = TestMembers.MEMBER_2;
        disabledMember = Member.createMember(
            sourceMember.getSocialId(),
            sourceMember.getProvider(),
            sourceMember.getSocialNickname(),
            sourceMember.getThumbnailImage(),
            sourceMember.getMemberName(),
            disabledSetting,
            null
        );
        disabledMember = testEntityManager.persistAndFlush(disabledMember);
        disabledMemberId = disabledMember.getId();
    }

    @Test
    @DisplayName("정상 케이스 - PAPER 알림 수신 자격 확인 (활성화된 사용자)")
    @Transactional
    void shouldReturnTrue_WhenMemberEligibleForPaperNotification() {
        // When: PAPER 알림 수신 자격 확인
        boolean result = notificationUtilAdapter.SseEligibleForNotification(enabledMemberId, NotificationType.PAPER);

        // Then: 활성화된 사용자는 수신 가능해야 함
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("정상 케이스 - COMMENT 알림 수신 자격 확인 (활성화된 사용자)")
    @Transactional
    void shouldReturnTrue_WhenMemberEligibleForCommentNotification() {
        // When: COMMENT 알림 수신 자격 확인
        boolean result = notificationUtilAdapter.SseEligibleForNotification(enabledMemberId, NotificationType.COMMENT);

        // Then: 활성화된 사용자는 수신 가능해야 함
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("정상 케이스 - POST_FEATURED 알림 수신 자격 확인 (활성화된 사용자)")
    @Transactional
    void shouldReturnTrue_WhenMemberEligibleForPostFeaturedNotification() {
        // When: POST_FEATURED 알림 수신 자격 확인
        boolean result = notificationUtilAdapter.SseEligibleForNotification(enabledMemberId, NotificationType.POST_FEATURED);

        // Then: 활성화된 사용자는 수신 가능해야 함
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("정상 케이스 - PAPER 알림 수신 자격 확인 (비활성화된 사용자)")
    @Transactional
    void shouldReturnFalse_WhenMemberNotEligibleForPaperNotification() {
        // When: PAPER 알림 수신 자격 확인
        boolean result = notificationUtilAdapter.SseEligibleForNotification(disabledMemberId, NotificationType.PAPER);

        // Then: 비활성화된 사용자는 수신 불가능해야 함
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("정상 케이스 - ADMIN 알림은 설정과 관계없이 항상 수신 가능")
    @Transactional
    void shouldAlwaysReturnTrue_WhenAdminNotification() {
        // When: ADMIN 알림 수신 자격 확인 (비활성화된 사용자도)
        boolean enabledMemberResult = notificationUtilAdapter.SseEligibleForNotification(enabledMemberId, NotificationType.ADMIN);
        boolean disabledMemberResult = notificationUtilAdapter.SseEligibleForNotification(disabledMemberId, NotificationType.ADMIN);

        // Then: 설정과 관계없이 모든 사용자가 수신 가능해야 함
        assertThat(enabledMemberResult).isTrue();
        assertThat(disabledMemberResult).isTrue();
    }

    @Test
    @DisplayName("정상 케이스 - INITIATE 알림은 설정과 관계없이 항상 수신 가능")
    @Transactional
    void shouldAlwaysReturnTrue_WhenInitiateNotification() {
        // When: INITIATE 알림 수신 자격 확인 (비활성화된 사용자도)
        boolean enabledMemberResult = notificationUtilAdapter.SseEligibleForNotification(enabledMemberId, NotificationType.INITIATE);
        boolean disabledMemberResult = notificationUtilAdapter.SseEligibleForNotification(disabledMemberId, NotificationType.INITIATE);

        // Then: 설정과 관계없이 모든 사용자가 수신 가능해야 함
        assertThat(enabledMemberResult).isTrue();
        assertThat(disabledMemberResult).isTrue();
    }

    @Test
    @DisplayName("정상 케이스 - FCM 토큰 조회 (수신 자격이 있는 사용자)")
    @Transactional
    void shouldReturnFcmTokens_WhenMemberEligibleForNotification() {
        // Given: FCM 토큰 생성
        FcmToken fcmToken1 = FcmToken.create(enabledMember, "fcm-TemporaryToken-1");
        FcmToken fcmToken2 = FcmToken.create(enabledMember, "fcm-TemporaryToken-2");

        testEntityManager.persistAndFlush(fcmToken1);
        testEntityManager.persistAndFlush(fcmToken2);
        testEntityManager.flush();
        testEntityManager.clear();

        // When: FCM 토큰 조회 (PAPER 알림 타입)
        List<FcmToken> result = notificationUtilAdapter.FcmEligibleFcmTokens(enabledMemberId, NotificationType.PAPER);

        // Then: FCM 토큰들이 조회되어야 함
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(FcmToken::getFcmRegistrationToken)
                .containsExactlyInAnyOrder("fcm-TemporaryToken-1", "fcm-TemporaryToken-2");
    }

    @Test
    @DisplayName("정상 케이스 - FCM 토큰 조회 (수신 자격이 없는 사용자)")
    @Transactional
    void shouldReturnEmptyList_WhenMemberNotEligibleForNotification() {
        // Given: 비활성화된 사용자의 FCM 토큰 생성
        FcmToken fcmToken = FcmToken.create(disabledMember, "fcm-TemporaryToken-disabled");
        testEntityManager.persistAndFlush(fcmToken);
        testEntityManager.flush();
        testEntityManager.clear();

        // When: FCM 토큰 조회 (PAPER 알림 타입)
        List<FcmToken> result = notificationUtilAdapter.FcmEligibleFcmTokens(disabledMemberId, NotificationType.PAPER);

        // Then: 빈 목록이 반환되어야 함 (알림 설정이 비활성화됨)
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("경계 케이스 - 존재하지 않는 사용자 ID")
    @Transactional
    void shouldReturnFalse_WhenMemberNotFound() {
        // Given: 존재하지 않는 사용자 ID
        Long nonExistentMemberId = 999999L;

        // When: 알림 수신 자격 확인
        boolean result = notificationUtilAdapter.SseEligibleForNotification(nonExistentMemberId, NotificationType.PAPER);

        // Then: 수신 불가능해야 함
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("경계 케이스 - FCM 토큰이 없는 사용자")
    @Transactional
    void shouldReturnEmptyList_WhenMemberHasNoFcmTokens() {
        // When: FCM 토큰 조회 (토큰이 없는 활성화된 사용자)
        List<FcmToken> result = notificationUtilAdapter.FcmEligibleFcmTokens(enabledMemberId, NotificationType.PAPER);

        // Then: 빈 목록이 반환되어야 함
        assertThat(result).isEmpty();
    }
}
