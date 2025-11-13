package jaeik.bimillog.adapter.out.notification;

import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.notification.entity.Notification;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.notification.out.NotificationQueryAdapter;
import jaeik.bimillog.testutil.config.H2TestConfiguration;
import jaeik.bimillog.testutil.builder.NotificationTestDataBuilder;
import jaeik.bimillog.testutil.fixtures.TestFixtures;
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

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>NotificationQueryAdapter 통합 테스트</h2>
 * <p>알림 조회 쿼리의 정렬 및 사용자 필터링 동작을 검증합니다.</p>
 */
@DataJpaTest(
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = jaeik.bimillog.BimilLogApplication.class
        )
)
@ActiveProfiles("h2test")
@Import({NotificationQueryAdapter.class, H2TestConfiguration.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("integration")
class NotificationQueryAdapterIntegrationTest {

    @Autowired
    private NotificationQueryAdapter notificationQueryAdapter;

    @Autowired
    private TestEntityManager testEntityManager;

    private Member targetMember;
    private Member otherMember;

    @BeforeEach
    void setUp() {
        Member tempTargetMember = TestMembers.copyWithId(TestMembers.MEMBER_1, null);
        testEntityManager.persistAndFlush(tempTargetMember.getSetting());
        testEntityManager.persistAndFlush(tempTargetMember.getSocialToken());
        targetMember = Member.createMember(
            tempTargetMember.getSocialId(),
            tempTargetMember.getProvider(),
            tempTargetMember.getSocialNickname(),
            tempTargetMember.getThumbnailImage(),
            tempTargetMember.getMemberName(),
            tempTargetMember.getSetting(),
            tempTargetMember.getSocialToken()
        );
        targetMember = testEntityManager.persistAndFlush(targetMember);

        Member tempOtherMember = TestMembers.copyWithId(TestMembers.MEMBER_2, null);
        testEntityManager.persistAndFlush(tempOtherMember.getSetting());
        testEntityManager.persistAndFlush(tempOtherMember.getSocialToken());
        otherMember = Member.createMember(
            tempOtherMember.getSocialId(),
            tempOtherMember.getProvider(),
            tempOtherMember.getSocialNickname(),
            tempOtherMember.getThumbnailImage(),
            tempOtherMember.getMemberName(),
            tempOtherMember.getSetting(),
            tempOtherMember.getSocialToken()
        );
        otherMember = testEntityManager.persistAndFlush(otherMember);
    }

    @Test
    @DisplayName("사용자 알림이 생성일 역순으로 조회된다")
    void shouldReturnNotificationsOrderedByCreatedAtDesc() {
        Notification older = NotificationTestDataBuilder.aCommentNotification(targetMember, 1L).build();
        Notification newest = NotificationTestDataBuilder.aPaperMessageNotification(targetMember).build();
        Notification middle = NotificationTestDataBuilder.aLikeNotification(targetMember, 3L).build();

        older = testEntityManager.persist(older);
        middle = testEntityManager.persist(middle);
        newest = testEntityManager.persist(newest);

        Instant now = Instant.now();
        TestFixtures.setFieldValue(older, "createdAt", now.minusSeconds(120));
        TestFixtures.setFieldValue(middle, "createdAt", now.minusSeconds(60));
        TestFixtures.setFieldValue(newest, "createdAt", now);

        // 다른 사용자의 알림은 조회되지 않아야 한다
        Notification otherNotification = NotificationTestDataBuilder.anAdminNotification(otherMember, "관리자 메시지").build();
        otherNotification = testEntityManager.persist(otherNotification);
        TestFixtures.setFieldValue(otherNotification, "createdAt", now.minusSeconds(30));

        testEntityManager.flush();
        testEntityManager.clear();

        List<Notification> notifications = notificationQueryAdapter.getNotificationList(targetMember.getId());

        assertThat(notifications)
                .extracting(Notification::getNotificationType)
                .containsExactly(
                        NotificationType.MESSAGE,
                        NotificationType.POST_FEATURED,
                        NotificationType.COMMENT
                );
    }

    @Test
    @DisplayName("알림이 없는 사용자는 빈 목록이 반환된다")
    void shouldReturnEmptyListWhenUserHasNoNotification() {
        testEntityManager.flush();
        testEntityManager.clear();

        List<Notification> notifications = notificationQueryAdapter.getNotificationList(otherMember.getId());

        assertThat(notifications).isEmpty();
    }
}
