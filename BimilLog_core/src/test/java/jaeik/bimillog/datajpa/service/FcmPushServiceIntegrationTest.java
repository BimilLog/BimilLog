package jaeik.bimillog.datajpa.service;

import jaeik.bimillog.BimilLogApplication;
import jaeik.bimillog.domain.auth.entity.AuthToken;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.entity.Setting;
import jaeik.bimillog.domain.member.repository.MemberQueryRepository;
import jaeik.bimillog.domain.member.service.MemberQueryService;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.notification.adapter.NotificationToMemberAdapter;
import jaeik.bimillog.domain.notification.service.FcmPushService;
import jaeik.bimillog.infrastructure.api.fcm.FcmAdapter;
import jaeik.bimillog.testutil.TestMembers;
import jaeik.bimillog.testutil.config.H2TestConfiguration;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * <h2>FcmPushService 통합 테스트</h2>
 * <p>FCM 토큰이 없는 경우 서비스 레벨에서 empty 체크가 정상 동작하는지 검증</p>
 * <p>H2 데이터베이스를 사용하여 실제 DB 조회 로직과 통합 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("FcmPushService 통합 테스트")
@DataJpaTest(
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = BimilLogApplication.class
        ),
        includeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {
                        FcmPushService.class,
                        NotificationToMemberAdapter.class,
                        MemberQueryService.class,
                        MemberQueryRepository.class
                }
        )
)
@Tag("datajpa-h2")
@ActiveProfiles("h2test")
@Import({H2TestConfiguration.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FcmPushServiceIntegrationTest {

    @Autowired
    private FcmPushService fcmPushService;

    @MockitoBean
    private FcmAdapter fcmAdapter;

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private NotificationToMemberAdapter notificationToMemberAdapter;

    private Member testMember;
    private Long testMemberId;

    @BeforeEach
    void setUp() {
        // Given: 테스트용 회원 설정 및 저장 (연관 엔티티 먼저 저장)
        Member tempMember = TestMembers.copyWithId(TestMembers.MEMBER_1, null);
        testEntityManager.persistAndFlush(tempMember.getSetting());
        testEntityManager.persistAndFlush(tempMember.getSocialToken());

        testMember = Member.createMember(
                tempMember.getSocialId(),
                tempMember.getProvider(),
                tempMember.getSocialNickname(),
                tempMember.getThumbnailImage(),
                tempMember.getMemberName(),
                tempMember.getSetting(),
                tempMember.getSocialToken()
        );
        testMember = testEntityManager.persistAndFlush(testMember);
        testMemberId = testMember.getId();
    }

    @Test
    @DisplayName("fcmEligibleFcmTokens가 자료 없을 때 empty 반환하는지 검증 (null 아님)")
    void shouldReturnEmptyList_NotNull_WhenNoTokens() {
        // Given: FCM 토큰이 전혀 없는 회원
        testEntityManager.flush();
        testEntityManager.clear();

        // When: fcmEligibleFcmTokens 직접 호출 (NotificationToMemberAdapter 통해)
        List<String> tokens = notificationToMemberAdapter.fcmEligibleFcmTokens(
                testMemberId,
                NotificationType.COMMENT
        );

        // Then: null이 아닌 empty 리스트 반환 (QueryDSL fetch()의 보장된 동작)
        assertThat(tokens).isNotNull();
        assertThat(tokens).isEmpty();
    }

    @Test
    @DisplayName("FCM 토큰이 있지만 알림 비활성화 - 전송하지 않음")
    void shouldNotSendFcm_WhenNotificationDisabled() throws Exception {
        // Given: 댓글 알림 비활성화 회원 생성
        Setting disabledSetting = Setting.builder()
                .messageNotification(true)
                .commentNotification(false) // 댓글 알림 비활성화
                .postFeaturedNotification(true)
                .build();
        testEntityManager.persistAndFlush(disabledSetting);

        // 새 회원 생성 (댓글 알림 비활성화)
        Member tempMember2 = TestMembers.copyWithId(TestMembers.MEMBER_2, null);
        testEntityManager.persistAndFlush(tempMember2.getSocialToken());
        Member disabledMember = Member.createMember(
                tempMember2.getSocialId(),
                tempMember2.getProvider(),
                tempMember2.getSocialNickname(),
                tempMember2.getThumbnailImage(),
                tempMember2.getMemberName(),
                disabledSetting,
                tempMember2.getSocialToken()
        );
        disabledMember = testEntityManager.persistAndFlush(disabledMember);

        // FCM 토큰 추가
        AuthToken authToken = AuthToken.builder()
                .member(disabledMember)
                .refreshToken("test-refresh-token")
                .fcmRegistrationToken("test-fcm-token")
                .build();
        testEntityManager.persistAndFlush(authToken);

        testEntityManager.flush();
        testEntityManager.clear();

        // When: 댓글 알림 전송 시도
        fcmPushService.sendNotification(
                NotificationType.COMMENT,
                disabledMember.getId(),
                "테스터",
                null
        );

        // Then: fcmAdapter가 호출되지 않음 (알림 비활성화)
        verify(fcmAdapter, never()).sendMessageTo(anyString(), anyString(), anyString());
    }

}
