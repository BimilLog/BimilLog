package jaeik.bimillog.domain.notification.repository;

import jaeik.bimillog.BimilLogApplication;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.notification.entity.Notification;
import jaeik.bimillog.domain.notification.entity.NotificationUpdateVO;
import jaeik.bimillog.testutil.TestMembers;
import jaeik.bimillog.testutil.builder.NotificationTestDataBuilder;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>NotificationRepository 통합 테스트 - 일괄 업데이트</h2>
 * <p>알림 Repository의 데이터베이스 연동 동작 검증</p>
 * <p>H2 데이터베이스를 사용하여 알림 CRUD 작업 테스트</p>
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
@Import({H2TestConfiguration.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("integration")
class NotificationCommandRepositoryIntegrationTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private TestEntityManager testEntityManager;

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
    @DisplayName("정상 케이스 - 알림 일괄 삭제")
    @Transactional
    void shouldBatchDeleteNotifications_WhenDeleteIdsProvided() {
        // Given: 테스트용 알림 여러 개 저장
        Notification notification1 = NotificationTestDataBuilder.aCommentNotification(testMember, 1L).build();
        Notification notification2 = NotificationTestDataBuilder.aPaperMessageNotification(testMember).build();
        Notification notification3 = NotificationTestDataBuilder.aLikeNotification(testMember, 3L).build();
        
        notification1 = testEntityManager.persistAndFlush(notification1);
        notification2 = testEntityManager.persistAndFlush(notification2);
        notification3 = testEntityManager.persistAndFlush(notification3);

        testEntityManager.flush();
        testEntityManager.clear();

        List<Long> deleteIds = Arrays.asList(notification1.getId(), notification3.getId());
        NotificationUpdateVO updateCommand = NotificationUpdateVO.of(Collections.emptyList(), deleteIds);

        // When: 일괄 삭제 실행 (batchUpdate 로직을 직접 구현)
        if (deleteIds != null && !deleteIds.isEmpty()) {
            notificationRepository.deleteAllByIdInAndMember_Id(deleteIds, testMemberId);
        }

        testEntityManager.flush();
        testEntityManager.clear();

        // Then: 지정된 알림들이 삭제되고 나머지는 유지되는지 검증
        List<Notification> remainingNotifications = notificationRepository.findAll();
        assertThat(remainingNotifications).hasSize(1);
        assertThat(remainingNotifications.getFirst().getId()).isEqualTo(notification2.getId());
        assertThat(remainingNotifications.getFirst().getContent()).isEqualTo("새로운 롤링페이퍼 메시지가 도착했습니다.");
    }

    @Test
    @DisplayName("정상 케이스 - 알림 일괄 읽음 처리")
    @Transactional
    void shouldBatchMarkAsRead_WhenReadIdsProvided() {
        // Given: 테스트용 알림 여러 개 저장 (모두 읽지 않음 상태)
        Notification notification1 = NotificationTestDataBuilder.aCommentNotification(testMember, 1L).asUnread().build();
        Notification notification2 = NotificationTestDataBuilder.aPaperMessageNotification(testMember).asUnread().build();
        Notification notification3 = NotificationTestDataBuilder.aLikeNotification(testMember, 3L).asUnread().build();
        
        notification1 = testEntityManager.persistAndFlush(notification1);
        notification2 = testEntityManager.persistAndFlush(notification2);
        notification3 = testEntityManager.persistAndFlush(notification3);

        testEntityManager.flush();
        testEntityManager.clear();

        List<Long> readIds = Arrays.asList(notification1.getId(), notification2.getId());
        NotificationUpdateVO updateCommand = NotificationUpdateVO.of(readIds, Collections.emptyList());

        // When: 일괄 읽음 처리 실행 (batchUpdate 로직을 직접 구현)
        if (readIds != null && !readIds.isEmpty()) {
            List<Notification> notificationsToUpdate = notificationRepository.findAllByIdInAndMember_Id(readIds, testMemberId);
            notificationsToUpdate.forEach(Notification::markAsRead);
        }

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
        List<Notification> notifications = List.of(
                NotificationTestDataBuilder.aCommentNotification(testMember, 1L).build(),
                NotificationTestDataBuilder.aLikeNotification(testMember, 3L).build(),
                NotificationTestDataBuilder.aPaperMessageNotification(testMember).build(),
                NotificationTestDataBuilder.anAdminNotification(testMember, "시스템 점검 안내").build()
        );
        Notification notification1 = testEntityManager.persistAndFlush(notifications.get(0));
        Notification notification2 = testEntityManager.persistAndFlush(notifications.get(1));
        Notification notification3 = testEntityManager.persistAndFlush(notifications.get(2));
        Notification notification4 = testEntityManager.persistAndFlush(notifications.get(3));

        testEntityManager.flush();
        testEntityManager.clear();

        List<Long> deleteIds = Arrays.asList(notification1.getId(), notification2.getId());
        List<Long> readIds = Arrays.asList(notification3.getId(), notification4.getId());

        NotificationUpdateVO updateCommand = NotificationUpdateVO.of(readIds, deleteIds);

        // When: 일괄 삭제 및 읽음 처리 실행 (batchUpdate 로직을 직접 구현)
        if (deleteIds != null && !deleteIds.isEmpty()) {
            notificationRepository.deleteAllByIdInAndMember_Id(deleteIds, testMemberId);
        }
        if (readIds != null && !readIds.isEmpty()) {
            List<Notification> notificationsToUpdate = notificationRepository.findAllByIdInAndMember_Id(readIds, testMemberId);
            notificationsToUpdate.forEach(Notification::markAsRead);
        }

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
    @DisplayName("경계 케이스 - 빈/null ID 목록으로 일괄 업데이트")
    @Transactional
    void shouldDoNothing_WhenEmptyOrNullIdsProvided() {
        // Given: 테스트용 알림 저장
        testEntityManager.persistAndFlush(
                NotificationTestDataBuilder.aCommentNotification(testMember, 1L).build()
        );

        testEntityManager.flush();
        long beforeCount = notificationRepository.count();

        // When: 빈 목록으로 일괄 업데이트 실행 (batchUpdate 로직을 직접 구현)
        NotificationUpdateVO emptyCommand = NotificationUpdateVO.of(Collections.emptyList(), Collections.emptyList());
        List<Long> emptyDeleteIds = emptyCommand.deletedIds();
        List<Long> emptyReadIds = emptyCommand.readIds();
        if (emptyDeleteIds != null && !emptyDeleteIds.isEmpty()) {
            notificationRepository.deleteAllByIdInAndMember_Id(emptyDeleteIds, testMemberId);
        }
        if (emptyReadIds != null && !emptyReadIds.isEmpty()) {
            List<Notification> notifications = notificationRepository.findAllByIdInAndMember_Id(emptyReadIds, testMemberId);
            notifications.forEach(Notification::markAsRead);
        }

        // When: null 목록으로 일괄 업데이트 실행
        NotificationUpdateVO nullCommand = NotificationUpdateVO.of(null, null);
        List<Long> nullDeleteIds = nullCommand.deletedIds();
        List<Long> nullReadIds = nullCommand.readIds();
        if (nullDeleteIds != null && !nullDeleteIds.isEmpty()) {
            notificationRepository.deleteAllByIdInAndMember_Id(nullDeleteIds, testMemberId);
        }
        if (nullReadIds != null && !nullReadIds.isEmpty()) {
            List<Notification> notifications = notificationRepository.findAllByIdInAndMember_Id(nullReadIds, testMemberId);
            notifications.forEach(Notification::markAsRead);
        }

        testEntityManager.flush();

        // Then: 아무 변화가 없어야 함
        long afterCount = notificationRepository.count();
        assertThat(afterCount).isEqualTo(beforeCount);

        List<Notification> notifications = notificationRepository.findAll();
        assertThat(notifications).hasSize(1);
        assertThat(notifications.getFirst().isRead()).isFalse(); // 읽음 상태 변경되지 않음
    }

    @Test
    @DisplayName("보안 케이스 - 다른 사용자의 알림은 수정되지 않음")
    @Transactional
    void shouldNotUpdateOtherUsersNotifications_WhenDifferentUserProvided() {
        // Given: 다른 사용자와 그의 알림 생성
        Member tempOtherMember = TestMembers.copyWithId(TestMembers.MEMBER_2, null);
        testEntityManager.persistAndFlush(tempOtherMember.getSetting());
        testEntityManager.persistAndFlush(tempOtherMember.getSocialToken());

        Member otherMember = Member.createMember(
            tempOtherMember.getSocialId(),
            tempOtherMember.getProvider(),
            tempOtherMember.getSocialNickname(),
            tempOtherMember.getThumbnailImage(),
            tempOtherMember.getMemberName(),
            tempOtherMember.getSetting(),
            tempOtherMember.getSocialToken()
        );
        otherMember = testEntityManager.persistAndFlush(otherMember);

        // 현재 사용자와 다른 사용자의 알림 각각 생성
        Notification myNotification = testEntityManager.persistAndFlush(
                NotificationTestDataBuilder.aCommentNotification(testMember, 1L).build()
        );

        Notification otherNotification = testEntityManager.persistAndFlush(
                NotificationTestDataBuilder.aPaperMessageNotification(otherMember).build()
        );

        testEntityManager.flush();
        testEntityManager.clear();

        List<Long> deleteIds = Arrays.asList(myNotification.getId(), otherNotification.getId());
        NotificationUpdateVO updateCommand = NotificationUpdateVO.of(Collections.emptyList(), deleteIds);

        // When: 현재 사용자가 다른 사용자의 알림까지 포함해서 삭제 시도 (batchUpdate 로직을 직접 구현)
        if (deleteIds != null && !deleteIds.isEmpty()) {
            notificationRepository.deleteAllByIdInAndMember_Id(deleteIds, testMemberId);
        }

        testEntityManager.flush();
        testEntityManager.clear();

        // Then: 현재 사용자의 알림만 삭제되고, 다른 사용자의 알림은 보존되어야 함
        List<Notification> remainingNotifications = notificationRepository.findAll();
        assertThat(remainingNotifications).hasSize(1);
        assertThat(remainingNotifications.getFirst().getId()).isEqualTo(otherNotification.getId());
        assertThat(remainingNotifications.getFirst().getMember().getId()).isEqualTo(otherMember.getId());
    }


}