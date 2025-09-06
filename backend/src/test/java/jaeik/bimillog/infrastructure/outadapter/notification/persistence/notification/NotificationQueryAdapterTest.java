package jaeik.bimillog.infrastructure.outadapter.notification.persistence.notification;

import jaeik.bimillog.BimilLogApplication;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.notification.entity.Notification;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.notification.out.persistence.notification.NotificationQueryAdapter;
import jaeik.bimillog.testutil.TestContainersConfiguration;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>NotificationQueryAdapter 통합 테스트</h2>
 * <p>알림 쿼리 어댑터의 실제 데이터베이스 연동 동작 검증</p>
 * <p>TestContainers를 통한 실제 MySQL 환경에서 알림 조회 작업 테스트</p>
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
@Import({NotificationQueryAdapter.class, TestContainersConfiguration.class})
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create"
})
class NotificationQueryAdapterTest {

    @Autowired
    private NotificationQueryAdapter notificationQueryAdapter;

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private EntityManager entityManager;

    private User testUser;
    private User otherUser;
    private Long testUserId;

    @BeforeEach
    void setUp() {
        // Given: 테스트용 사용자들 설정 및 저장
        Setting testSetting = Setting.builder()
                .messageNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(true)
                .build();

        testUser = User.builder()
                .socialId("12345")
                .provider(SocialProvider.KAKAO)
                .userName("테스트유저")
                .role(UserRole.USER)
                .setting(testSetting)
                .build();

        testUser = testEntityManager.persistAndFlush(testUser);

        Setting otherSetting = Setting.builder()
                .messageNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(true)
                .build();

        otherUser = User.builder()
                .socialId("67890")
                .provider(SocialProvider.KAKAO)
                .userName("다른유저")
                .role(UserRole.USER)
                .setting(otherSetting)
                .build();

        otherUser = testEntityManager.persistAndFlush(otherUser);

        // 테스트용 사용자 ID 저장
        testUserId = testUser.getId();

        testEntityManager.flush();
        testEntityManager.clear();
    }

    @Test
    @DisplayName("정상 케이스 - 사용자의 알림 목록 조회")
    @Transactional
    void shouldReturnNotificationList_WhenUserHasNotifications() {
        // Given: 테스트용 알림 여러 개 생성 (최신순 정렬 확인을 위해 시간 간격 두기)
        createAndSaveNotification(testUser, NotificationType.COMMENT, "첫 번째 댓글 알림", "/post/1", false);

        // 시간 간격을 위한 잠시 대기 (실제로는 createdAt이 다르게 저장됨)
        try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        createAndSaveNotification(testUser, NotificationType.PAPER, "두 번째 메시지 알림", "/paper/2", true);

        try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        createAndSaveNotification(testUser, NotificationType.POST_FEATURED, "세 번째 인기글 알림", "/post/3", false);

        // 다른 사용자의 알림도 생성 (결과에 포함되지 않아야 함)
        createAndSaveNotification(otherUser, NotificationType.ADMIN, "다른 사용자 관리자 알림", "/admin/1", false);

        testEntityManager.flush();
        testEntityManager.clear();

        // When: 알림 목록 조회
        List<Notification> result = notificationQueryAdapter.getNotificationList(testUserId);

        // Then: 조회 결과 검증
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3); // 현재 사용자의 알림만 3개

        // 최신순 정렬 확인 (가장 나중에 생성된 것이 먼저)
        Notification firstNotification = result.get(0);
        assertThat(firstNotification.getNotificationType()).isEqualTo(NotificationType.POST_FEATURED);
        assertThat(firstNotification.getContent()).isEqualTo("세 번째 인기글 알림");
        assertThat(firstNotification.getUrl()).isEqualTo("/post/3");
        assertThat(firstNotification.isRead()).isFalse();
        assertThat(firstNotification.getCreatedAt()).isNotNull();

        Notification secondNotification = result.get(1);
        assertThat(secondNotification.getNotificationType()).isEqualTo(NotificationType.PAPER);
        assertThat(secondNotification.getContent()).isEqualTo("두 번째 메시지 알림");
        assertThat(secondNotification.getUrl()).isEqualTo("/paper/2");
        assertThat(secondNotification.isRead()).isTrue();

        Notification thirdNotification = result.get(2);
        assertThat(thirdNotification.getNotificationType()).isEqualTo(NotificationType.COMMENT);
        assertThat(thirdNotification.getContent()).isEqualTo("첫 번째 댓글 알림");
        assertThat(thirdNotification.getUrl()).isEqualTo("/post/1");
        assertThat(thirdNotification.isRead()).isFalse();
    }

    @Test
    @DisplayName("경계 케이스 - 알림이 없는 사용자의 조회")
    @Transactional
    void shouldReturnEmptyList_WhenUserHasNoNotifications() {
        // Given: 다른 사용자의 알림만 존재
        createAndSaveNotification(otherUser, NotificationType.ADMIN, "다른 사용자의 알림", "/admin/1", false);

        testEntityManager.flush();
        testEntityManager.clear();

        // When: 알림이 없는 사용자의 알림 목록 조회
        List<Notification> result = notificationQueryAdapter.getNotificationList(testUserId);

        // Then: 빈 목록 반환
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("정상 케이스 - 모든 알림 유형별 조회 확인")
    @Transactional
    void shouldReturnAllNotificationTypes_WhenDifferentTypesExist() {
        // Given: 모든 알림 유형별로 알림 생성
        createAndSaveNotification(testUser, NotificationType.COMMENT, "댓글 알림", "/post/1", false);
        createAndSaveNotification(testUser, NotificationType.PAPER, "메시지 알림", "/paper/2", false);
        createAndSaveNotification(testUser, NotificationType.POST_FEATURED, "인기글 알림", "/post/3", false);
        createAndSaveNotification(testUser, NotificationType.ADMIN, "관리자 알림", "/admin/4", true);
        createAndSaveNotification(testUser, NotificationType.INITIATE, "초기화 알림", "/init/5", true);

        testEntityManager.flush();
        testEntityManager.clear();

        // When: 알림 목록 조회
        List<Notification> result = notificationQueryAdapter.getNotificationList(testUserId);

        // Then: 모든 유형의 알림이 조회되는지 확인
        assertThat(result).hasSize(5);

        // 각 유형별로 존재 확인
        assertThat(result)
                .extracting(Notification::getNotificationType)
                .containsExactlyInAnyOrder(
                        NotificationType.COMMENT,
                        NotificationType.PAPER,
                        NotificationType.POST_FEATURED,
                        NotificationType.ADMIN,
                        NotificationType.INITIATE
                );

        // 읽음 상태 확인
        long unreadCount = result.stream().mapToLong(n -> n.isRead() ? 0 : 1).sum();
        long readCount = result.stream().mapToLong(n -> n.isRead() ? 1 : 0).sum();

        assertThat(unreadCount).isEqualTo(3); // COMMENT, PAPER, POST_FEATURED
        assertThat(readCount).isEqualTo(2); // ADMIN, INITIATE
    }

    @Test
    @DisplayName("정상 케이스 - 시간순 정렬 확인 (최신 순)")
    @Transactional
    void shouldReturnNotificationsInDescendingOrder_WhenMultipleNotificationsExist() {
        // Given: 시간차를 둔 알림 생성 (명확한 순서 확인을 위해)
        Notification oldest = createAndSaveNotification(testUser, NotificationType.COMMENT, "가장 오래된 알림", "/post/1", false);

        try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        Notification middle = createAndSaveNotification(testUser, NotificationType.PAPER, "중간 알림", "/paper/2", false);

        try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        Notification newest = createAndSaveNotification(testUser, NotificationType.POST_FEATURED, "가장 최신 알림", "/post/3", false);

        testEntityManager.flush();
        testEntityManager.clear();

        // When: 알림 목록 조회
        List<Notification> result = notificationQueryAdapter.getNotificationList(testUserId);

        // Then: 최신순으로 정렬되어 반환되는지 확인
        assertThat(result).hasSize(3);

        // 첫 번째가 가장 최신
        assertThat(result.get(0).getContent()).isEqualTo("가장 최신 알림");
        assertThat(result.get(1).getContent()).isEqualTo("중간 알림");
        assertThat(result.get(2).getContent()).isEqualTo("가장 오래된 알림");

        // 시간 순서 확인
        assertThat(result.get(0).getCreatedAt()).isAfter(result.get(1).getCreatedAt());
        assertThat(result.get(1).getCreatedAt()).isAfter(result.get(2).getCreatedAt());
    }

    @Test
    @DisplayName("보안 케이스 - 다른 사용자의 알림은 조회되지 않음")
    @Transactional
    void shouldNotReturnOtherUsersNotifications_WhenQueryingNotifications() {
        // Given: 현재 사용자와 다른 사용자의 알림 각각 생성
        createAndSaveNotification(testUser, NotificationType.COMMENT, "내 댓글 알림", "/post/1", false);
        createAndSaveNotification(testUser, NotificationType.PAPER, "내 메시지 알림", "/paper/2", false);

        createAndSaveNotification(otherUser, NotificationType.ADMIN, "다른 사용자의 관리자 알림", "/admin/1", false);
        createAndSaveNotification(otherUser, NotificationType.POST_FEATURED, "다른 사용자의 인기글 알림", "/post/100", true);

        testEntityManager.flush();
        testEntityManager.clear();

        // When: 현재 사용자의 알림 목록 조회
        List<Notification> result = notificationQueryAdapter.getNotificationList(testUserId);

        // Then: 현재 사용자의 알림만 조회되어야 함
        assertThat(result).hasSize(2);

        assertThat(result)
                .extracting(Notification::getContent)
                .containsExactlyInAnyOrder("내 댓글 알림", "내 메시지 알림");

        // 다른 사용자의 알림 내용은 포함되지 않아야 함
        assertThat(result)
                .extracting(Notification::getContent)
                .doesNotContain("다른 사용자의 관리자 알림", "다른 사용자의 인기글 알림");
    }

    @Test
    @DisplayName("경계 케이스 - 대용량 알림 데이터 조회 성능")
    @Transactional
    void shouldHandleLargeNotificationData_WhenManyNotificationsExist() throws InterruptedException {
        // Given: 대용량 알림 데이터 생성 (100개)
        for (int i = 1; i <= 100; i++) {
            Thread.sleep(10);
            NotificationType type = NotificationType.values()[i % NotificationType.values().length];
            boolean isRead = i % 3 == 0; // 3의 배수는 읽음 상태
            createAndSaveNotification(testUser, type, "알림 #" + i, "/url/" + i, isRead);
        }

        testEntityManager.flush();
        testEntityManager.clear();

        // When: 대용량 데이터 조회
        long startTime = System.currentTimeMillis();
        List<Notification> result = notificationQueryAdapter.getNotificationList(testUserId);
        long endTime = System.currentTimeMillis();

        // Then: 모든 데이터가 정상적으로 조회되는지 확인
        assertThat(result).hasSize(100);

        // 성능 확인 (1초 미만이어야 함)
        long executionTime = endTime - startTime;
        assertThat(executionTime).isLessThan(1000);

        // 최신순 정렬 확인 (마지막에 생성된 것이 첫 번째)
        assertThat(result.getFirst().getContent()).isEqualTo("알림 #100");
        assertThat(result.get(99).getContent()).isEqualTo("알림 #1");


        // 읽음 상태 분포 확인
        long readCount = result.stream().mapToLong(n -> n.isRead() ? 1 : 0).sum();
        assertThat(readCount).isEqualTo(33); // 3의 배수는 33개
    }

    /**
     * 테스트용 알림 생성 및 저장 헬퍼 메서드
     */
    private Notification createAndSaveNotification(User user, NotificationType type, String content, String url, boolean isRead) {
        Notification notification = Notification.create(user, type, content, url);

        // 읽음 상태 설정을 위한 리플렉션 (테스트 목적)
        if (isRead) {
            try {
                java.lang.reflect.Field readField = Notification.class.getDeclaredField("isRead");
                readField.setAccessible(true);
                readField.set(notification, true);
            } catch (Exception e) {
                // 리플렉션 실패 시 기본값(false) 유지
            }
        }

        return testEntityManager.persistAndFlush(notification);
    }
}