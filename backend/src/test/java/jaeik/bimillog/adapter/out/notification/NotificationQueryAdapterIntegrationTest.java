package jaeik.bimillog.adapter.out.notification;

import jaeik.bimillog.domain.notification.entity.Notification;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.adapter.out.notification.NotificationQueryAdapter;
import jaeik.bimillog.testutil.H2TestConfiguration;
import jaeik.bimillog.testutil.NotificationTestDataBuilder;
import jaeik.bimillog.testutil.TestFixtures;
import jaeik.bimillog.testutil.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
@Tag("integration")
class NotificationQueryAdapterIntegrationTest {

    @Autowired
    private NotificationQueryAdapter notificationQueryAdapter;

    @Autowired
    private TestEntityManager testEntityManager;

    private User targetUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        targetUser = TestUsers.copyWithId(TestUsers.USER1, null);
        otherUser = TestUsers.copyWithId(TestUsers.USER2, null);

        targetUser = testEntityManager.persistAndFlush(targetUser);
        otherUser = testEntityManager.persistAndFlush(otherUser);
    }

    @Test
    @DisplayName("사용자 알림이 생성일 역순으로 조회된다")
    void shouldReturnNotificationsOrderedByCreatedAtDesc() {
        Notification older = NotificationTestDataBuilder.aCommentNotification(targetUser, 1L).build();
        Notification newest = NotificationTestDataBuilder.aPaperMessageNotification(targetUser).build();
        Notification middle = NotificationTestDataBuilder.aLikeNotification(targetUser, 3L).build();

        older = testEntityManager.persist(older);
        middle = testEntityManager.persist(middle);
        newest = testEntityManager.persist(newest);

        Instant now = Instant.now();
        TestFixtures.setFieldValue(older, "createdAt", now.minusSeconds(120));
        TestFixtures.setFieldValue(middle, "createdAt", now.minusSeconds(60));
        TestFixtures.setFieldValue(newest, "createdAt", now);

        // 다른 사용자의 알림은 조회되지 않아야 한다
        Notification otherNotification = NotificationTestDataBuilder.anAdminNotification(otherUser, "관리자 메시지").build();
        otherNotification = testEntityManager.persist(otherNotification);
        TestFixtures.setFieldValue(otherNotification, "createdAt", now.minusSeconds(30));

        testEntityManager.flush();
        testEntityManager.clear();

        List<Notification> notifications = notificationQueryAdapter.getNotificationList(targetUser.getId());

        assertThat(notifications)
                .extracting(Notification::getNotificationType)
                .containsExactly(
                        NotificationType.PAPER,
                        NotificationType.POST_FEATURED,
                        NotificationType.COMMENT
                );
    }

    @Test
    @DisplayName("알림이 없는 사용자는 빈 목록이 반환된다")
    void shouldReturnEmptyListWhenUserHasNoNotification() {
        testEntityManager.flush();
        testEntityManager.clear();

        List<Notification> notifications = notificationQueryAdapter.getNotificationList(otherUser.getId());

        assertThat(notifications).isEmpty();
    }
}
