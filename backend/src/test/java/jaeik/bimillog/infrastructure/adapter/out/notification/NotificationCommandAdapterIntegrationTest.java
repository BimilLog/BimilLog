package jaeik.bimillog.infrastructure.adapter.out.notification;

import jaeik.bimillog.BimilLogApplication;
import jaeik.bimillog.domain.notification.entity.Notification;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.notification.entity.NotificationUpdateVO;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.adapter.out.notification.jpa.NotificationRepository;
import jaeik.bimillog.testutil.NotificationTestDataBuilder;
import jaeik.bimillog.testutil.TestContainersConfiguration;
import jaeik.bimillog.testutil.TestUsers;
import jakarta.persistence.EntityManager;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>NotificationCommandAdapter 통합 테스트</h2>
 * <p>알림 명령 어댑터의 실제 데이터베이스 연동 동작 검증</p>
 * <p>TestContainers를 통한 실제 MySQL 환경에서 알림 CRUD 작업 테스트</p>
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
@Import({NotificationCommandAdapter.class, TestContainersConfiguration.class})
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create"
})
class NotificationCommandAdapterIntegrationTest {

    @Autowired
    private NotificationCommandAdapter notificationCommandAdapter;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private EntityManager entityManager;

    private User testUser;
    private Long testUserId;

    @BeforeEach
    void setUp() {
        // Given: 테스트용 사용저 설정 및 저장
        testUser = TestUsers.copyWithId(TestUsers.USER1, null);
        testUser = testEntityManager.persistAndFlush(testUser);
        testUserId = testUser.getId();
    }

    @Test
    @DisplayName("정상 케이스 - 알림 저장")
    void shouldSaveNotification_WhenValidParametersProvided() {
        // Given: 알림 저장에 필요한 파라미터
        NotificationType type = NotificationType.COMMENT;
        String content = "새로운 댓글이 달렸습니다";
        String url = "/post/123";

        // When: 알림 저장
        notificationCommandAdapter.save(testUser, type, content, url);

        // Then: 알림이 정상적으로 저장되었는지 검증
        List<Notification> savedNotifications = notificationRepository.findAll();
        assertThat(savedNotifications).hasSize(1);

        Notification savedNotification = savedNotifications.get(0);
        assertThat(savedNotification.getUsers()).isEqualTo(testUser);
        assertThat(savedNotification.getNotificationType()).isEqualTo(type);
        assertThat(savedNotification.getContent()).isEqualTo(content);
        assertThat(savedNotification.getUrl()).isEqualTo(url);
        assertThat(savedNotification.isRead()).isFalse(); // 기본값은 false
        assertThat(savedNotification.getCreatedAt()).isNotNull();
        // BaseEntity의 modifiedAt 확인
        assertThat(savedNotification.getModifiedAt()).isNotNull();
    }

    @Test
    @DisplayName("정상 케이스 - 알림 일괄 삭제")
    @Transactional
    void shouldBatchDeleteNotifications_WhenDeleteIdsProvided() {
        // Given: 테스트용 알림 여러 개 저장
        Notification notification1 = NotificationTestDataBuilder.aCommentNotification(testUser, 1L).build();
        Notification notification2 = NotificationTestDataBuilder.aPaperMessageNotification(testUser).build();
        Notification notification3 = NotificationTestDataBuilder.aLikeNotification(testUser, 3L).build();
        
        notification1 = testEntityManager.persistAndFlush(notification1);
        notification2 = testEntityManager.persistAndFlush(notification2);
        notification3 = testEntityManager.persistAndFlush(notification3);

        testEntityManager.flush();
        testEntityManager.clear();

        List<Long> deleteIds = Arrays.asList(notification1.getId(), notification3.getId());
        NotificationUpdateVO updateCommand = NotificationUpdateVO.of(Collections.emptyList(), deleteIds);

        // When: 일괄 삭제 실행
        notificationCommandAdapter.batchUpdate(testUserId, updateCommand);

        testEntityManager.flush();
        testEntityManager.clear();

        // Then: 지정된 알림들이 삭제되고 나머지는 유지되는지 검증
        List<Notification> remainingNotifications = notificationRepository.findAll();
        assertThat(remainingNotifications).hasSize(1);
        assertThat(remainingNotifications.get(0).getId()).isEqualTo(notification2.getId());
        assertThat(remainingNotifications.get(0).getContent()).isEqualTo("메시지 알림 2");
    }

    @Test
    @DisplayName("정상 케이스 - 알림 일괄 읽음 처리")
    @Transactional
    void shouldBatchMarkAsRead_WhenReadIdsProvided() {
        // Given: 테스트용 알림 여러 개 저장 (모두 읽지 않음 상태)
        Notification notification1 = NotificationTestDataBuilder.aCommentNotification(testUser, 1L).asUnread().build();
        Notification notification2 = NotificationTestDataBuilder.aPaperMessageNotification(testUser).asUnread().build();
        Notification notification3 = NotificationTestDataBuilder.aLikeNotification(testUser, 3L).asUnread().build();
        
        notification1 = testEntityManager.persistAndFlush(notification1);
        notification2 = testEntityManager.persistAndFlush(notification2);
        notification3 = testEntityManager.persistAndFlush(notification3);

        testEntityManager.flush();
        testEntityManager.clear();

        List<Long> readIds = Arrays.asList(notification1.getId(), notification2.getId());
        NotificationUpdateVO updateCommand = NotificationUpdateVO.of(readIds, Collections.emptyList());

        // When: 일괄 읽음 처리 실행
        notificationCommandAdapter.batchUpdate(testUserId, updateCommand);

        testEntityManager.flush();
        testEntityManager.clear();

        // Then: 지정된 알림들이 읽음 상태로 변경되는지 검증
        List<Notification> updatedNotifications = notificationRepository.findAll();
        assertThat(updatedNotifications).hasSize(3);

        // ID로 찾아서 읽음 상태 확인
        updatedNotifications.forEach(notification -> {
            if (readIds.contains(notification.getId())) {
                assertThat(notification.isRead()).isTrue();
            } else {
                assertThat(notification.isRead()).isFalse();
            }
        });
    }

    @Test
    @DisplayName("정상 케이스 - 알림 일괄 삭제 및 읽음 처리 동시 실행")
    @Transactional
    void shouldBatchDeleteAndMarkAsRead_WhenBothIdsProvided() {
        // Given: 테스트용 알림 4개 저장
        List<Notification> notifications = NotificationTestDataBuilder.createMixedNotifications(testUser);
        Notification notification1 = testEntityManager.persistAndFlush(notifications.get(0));
        Notification notification2 = testEntityManager.persistAndFlush(notifications.get(1));
        Notification notification3 = testEntityManager.persistAndFlush(notifications.get(2));
        Notification notification4 = testEntityManager.persistAndFlush(notifications.get(3));

        testEntityManager.flush();
        testEntityManager.clear();

        List<Long> deleteIds = Arrays.asList(notification1.getId(), notification2.getId());
        List<Long> readIds = Arrays.asList(notification3.getId(), notification4.getId());

        NotificationUpdateVO updateCommand = NotificationUpdateVO.of(readIds, deleteIds);

        // When: 일괄 삭제 및 읽음 처리 실행
        notificationCommandAdapter.batchUpdate(testUserId, updateCommand);

        testEntityManager.flush();
        testEntityManager.clear();

        // Then: 삭제된 알림은 사라지고, 나머지는 읽음 상태로 변경되는지 검증
        List<Notification> remainingNotifications = notificationRepository.findAll();
        assertThat(remainingNotifications).hasSize(2);

        remainingNotifications.forEach(notification -> {
            assertThat(notification.isRead()).isTrue(); // 읽음 처리된 알림들만 남음
            assertThat(readIds).contains(notification.getId()); // 읽음 처리 대상이었던 알림들
        });
    }

    @Test
    @DisplayName("경계 케이스 - 빈 ID 목록으로 일괄 업데이트")
    @Transactional
    void shouldDoNothing_WhenEmptyIdsProvided() {
        // Given: 테스트용 알림 저장
        testEntityManager.persistAndFlush(
                NotificationTestDataBuilder.aCommentNotification(testUser, 1L).build()
        );

        testEntityManager.flush();
        long beforeCount = notificationRepository.count();

        NotificationUpdateVO updateCommand = NotificationUpdateVO.of(Collections.emptyList(), Collections.emptyList());

        // When: 빈 목록으로 일괄 업데이트 실행
        notificationCommandAdapter.batchUpdate(testUserId, updateCommand);

        testEntityManager.flush();

        // Then: 아무 변화가 없어야 함
        long afterCount = notificationRepository.count();
        assertThat(afterCount).isEqualTo(beforeCount);

        List<Notification> notifications = notificationRepository.findAll();
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).isRead()).isFalse(); // 읽음 상태 변경되지 않음
    }

    @Test
    @DisplayName("경계 케이스 - null ID 목록으로 일괄 업데이트")
    @Transactional
    void shouldDoNothing_WhenNullIdsProvided() {
        // Given: 테스트용 알림 저장
        testEntityManager.persistAndFlush(
                NotificationTestDataBuilder.aCommentNotification(testUser, 1L).build()
        );

        testEntityManager.flush();
        long beforeCount = notificationRepository.count();

        NotificationUpdateVO updateCommand = NotificationUpdateVO.of(null, null);

        // When: null 목록으로 일괄 업데이트 실행
        notificationCommandAdapter.batchUpdate(testUserId, updateCommand);

        testEntityManager.flush();

        // Then: 아무 변화가 없어야 함
        long afterCount = notificationRepository.count();
        assertThat(afterCount).isEqualTo(beforeCount);

        List<Notification> notifications = notificationRepository.findAll();
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).isRead()).isFalse(); // 읽음 상태 변경되지 않음
    }

    @Test
    @DisplayName("보안 케이스 - 다른 사용자의 알림은 수정되지 않음")
    @Transactional
    void shouldNotUpdateOtherUsersNotifications_WhenDifferentUserProvided() {
        // Given: 다른 사용자와 그의 알림 생성
        User otherUser = TestUsers.copyWithId(TestUsers.USER2, null);
        otherUser = testEntityManager.persistAndFlush(otherUser);

        // 현재 사용자와 다른 사용자의 알림 각각 생성
        Notification myNotification = testEntityManager.persistAndFlush(
                NotificationTestDataBuilder.aCommentNotification(testUser, 1L).build()
        );

        Notification otherNotification = testEntityManager.persistAndFlush(
                NotificationTestDataBuilder.aPaperMessageNotification(otherUser).build()
        );

        testEntityManager.flush();
        testEntityManager.clear();

        List<Long> deleteIds = Arrays.asList(myNotification.getId(), otherNotification.getId());
        NotificationUpdateVO updateCommand = NotificationUpdateVO.of(Collections.emptyList(), deleteIds);

        // When: 현재 사용자가 다른 사용자의 알림까지 포함해서 삭제 시도
        notificationCommandAdapter.batchUpdate(testUserId, updateCommand);

        testEntityManager.flush();
        testEntityManager.clear();

        // Then: 현재 사용자의 알림만 삭제되고, 다른 사용자의 알림은 보존되어야 함
        List<Notification> remainingNotifications = notificationRepository.findAll();
        assertThat(remainingNotifications).hasSize(1);
        assertThat(remainingNotifications.get(0).getId()).isEqualTo(otherNotification.getId());
        assertThat(remainingNotifications.get(0).getUsers().getId()).isEqualTo(otherUser.getId());
    }


}