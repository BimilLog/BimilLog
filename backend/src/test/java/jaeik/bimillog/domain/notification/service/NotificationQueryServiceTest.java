package jaeik.bimillog.domain.notification.service;

import jaeik.bimillog.domain.notification.application.port.out.NotificationQueryPort;
import jaeik.bimillog.domain.notification.application.service.NotificationQueryService;
import jaeik.bimillog.domain.notification.entity.Notification;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
import jaeik.bimillog.testutil.NotificationTestDataBuilder;
import jaeik.bimillog.testutil.TestFixtures;
import jaeik.bimillog.testutil.TestUsers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>NotificationQueryService 테스트</h2>
 * <p>알림 조회 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 *
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationQueryService 테스트")
class NotificationQueryServiceTest {

    @Mock
    private NotificationQueryPort notificationQueryPort;

    @InjectMocks
    private NotificationQueryService notificationQueryService;

    @Test
    @DisplayName("알림 목록 조회 - 성공")
    void shouldGetNotificationList_WhenValidUser() {
        // Given
        User user = TestUsers.USER1;
        CustomUserDetails userDetails = TestFixtures.createCustomUserDetails(user);
        given(userDetails.getUserId()).willReturn(1L);
        
        List<Notification> expectedNotifications = Arrays.asList(
                NotificationTestDataBuilder.aCommentNotification(user, 123L)
                        .withId(1L)
                        .asUnread()
                        .build(),
                NotificationTestDataBuilder.aLikeNotification(user, 456L)
                        .withId(2L)
                        .asRead()
                        .createdDaysAgo(1)
                        .build()
        );

        given(notificationQueryPort.getNotificationList(any()))
                .willReturn(expectedNotifications);

        // When
        List<Notification> result = notificationQueryService.getNotificationList(userDetails);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getNotificationType()).isEqualTo(NotificationType.COMMENT);
        assertThat(result.get(0).isRead()).isFalse();
        
        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(1).getNotificationType()).isEqualTo(NotificationType.POST_FEATURED);
        assertThat(result.get(1).isRead()).isTrue();

        verify(notificationQueryPort, times(1)).getNotificationList(any());
        verifyNoMoreInteractions(notificationQueryPort);
    }

    @Test
    @DisplayName("알림 목록 조회 - 빈 목록")
    void shouldGetNotificationList_WhenNoNotifications() {
        // Given
        CustomUserDetails userDetails = TestFixtures.createCustomUserDetails(TestUsers.USER2);
        given(userDetails.getUserId()).willReturn(2L);
        List<Notification> emptyList = Collections.emptyList();
        given(notificationQueryPort.getNotificationList(any()))
                .willReturn(emptyList);

        // When
        List<Notification> result = notificationQueryService.getNotificationList(userDetails);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(notificationQueryPort, times(1)).getNotificationList(any());
        verifyNoMoreInteractions(notificationQueryPort);
    }

    @Test
    @DisplayName("알림 목록 조회 - 단일 알림")
    void shouldGetNotificationList_WhenSingleNotification() {
        // Given
        User user = TestUsers.USER3;
        CustomUserDetails userDetails = TestFixtures.createCustomUserDetails(user);
        given(userDetails.getUserId()).willReturn(3L);
        
        List<Notification> singleNotification = Arrays.asList(
                NotificationTestDataBuilder.aPaperMessageNotification(user)
                        .withId(1L)
                        .asUnread()
                        .build()
        );

        given(notificationQueryPort.getNotificationList(any()))
                .willReturn(singleNotification);

        // When
        List<Notification> result = notificationQueryService.getNotificationList(userDetails);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getNotificationType()).isEqualTo(NotificationType.PAPER);
        assertThat(result.get(0).isRead()).isFalse();

        verify(notificationQueryPort, times(1)).getNotificationList(any());
        verifyNoMoreInteractions(notificationQueryPort);
    }

    @Test
    @DisplayName("알림 목록 조회 - 읽은 알림과 안읽은 알림 혼재")
    void shouldGetNotificationList_WhenMixedReadStatus() {
        // Given
        User user = TestUsers.withRole(UserRole.ADMIN);
        CustomUserDetails userDetails = TestFixtures.createCustomUserDetails(user);
        given(userDetails.getUserId()).willReturn(4L);
        
        List<Notification> mixedNotifications = new java.util.ArrayList<>();
        // 읽지 않은 알림
        for (int i = 1; i <= 2; i++) {
            mixedNotifications.add(NotificationTestDataBuilder.aNotification()
                    .withReceiver(user)
                    .withMessage("Unread notification " + i)
                    .asUnread()
                    .build());
        }
        // 읽은 알림
        for (int i = 1; i <= 1; i++) {
            mixedNotifications.add(NotificationTestDataBuilder.aNotification()
                    .withReceiver(user)
                    .withMessage("Read notification " + i)
                    .asRead()
                    .build());
        }

        given(notificationQueryPort.getNotificationList(any()))
                .willReturn(mixedNotifications);

        // When
        List<Notification> result = notificationQueryService.getNotificationList(userDetails);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        
        // 읽지 않은 알림 확인
        long unreadCount = result.stream().filter(n -> !n.isRead()).count();
        assertThat(unreadCount).isEqualTo(2);
        
        // 읽은 알림 확인
        long readCount = result.stream().filter(Notification::isRead).count();
        assertThat(readCount).isEqualTo(1);

        verify(notificationQueryPort, times(1)).getNotificationList(any());
        verifyNoMoreInteractions(notificationQueryPort);
    }

    @Test
    @DisplayName("알림 목록 조회 - null 사용자")
    void shouldGetNotificationList_WhenNullUser() {
        // Given
        CustomUserDetails nullUserDetails = null;

        // When
        List<Notification> result = notificationQueryService.getNotificationList(nullUserDetails);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        // null 사용자의 경우 포트 호출 없이 빈 리스트 반환
        verifyNoInteractions(notificationQueryPort);
    }

    @Test
    @DisplayName("알림 목록 조회 - userId가 null인 사용자")
    void shouldGetNotificationList_WhenUserIdIsNull() {
        // Given
        CustomUserDetails userDetailsWithNullId = mock(CustomUserDetails.class);
        given(userDetailsWithNullId.getUserId()).willReturn(null);

        // When
        List<Notification> result = notificationQueryService.getNotificationList(userDetailsWithNullId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        // userId가 null인 경우 포트 호출 없이 빈 리스트 반환
        verifyNoInteractions(notificationQueryPort);
    }

    @Test
    @DisplayName("알림 목록 조회 - 대량 알림")
    void shouldGetNotificationList_WhenLargeAmountOfNotifications() {
        // Given
        User user = TestUsers.USER1;
        CustomUserDetails userDetails = TestFixtures.createCustomUserDetails(user);
        given(userDetails.getUserId()).willReturn(1L);
        
        List<Notification> largeNotificationList = NotificationTestDataBuilder.createNotifications(10, user);

        given(notificationQueryPort.getNotificationList(any()))
                .willReturn(largeNotificationList);

        // When
        List<Notification> result = notificationQueryService.getNotificationList(userDetails);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(10);

        verify(notificationQueryPort, times(1)).getNotificationList(any());
        verifyNoMoreInteractions(notificationQueryPort);
    }

    @Test
    @DisplayName("알림 목록 조회 - 모든 알림 타입 포함")
    void shouldGetNotificationList_WhenAllNotificationTypes() {
        // Given
        User user = TestUsers.USER2;
        CustomUserDetails userDetails = TestFixtures.createCustomUserDetails(user);
        given(userDetails.getUserId()).willReturn(2L);
        
        List<Notification> allTypesNotifications = NotificationTestDataBuilder.createMixedNotifications(user);

        given(notificationQueryPort.getNotificationList(any()))
                .willReturn(allTypesNotifications);

        // When
        List<Notification> result = notificationQueryService.getNotificationList(userDetails);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(5);
        
        // 알림 타입별 검증
        assertThat(result).extracting(Notification::getNotificationType)
                .contains(NotificationType.COMMENT, NotificationType.POST_FEATURED, 
                         NotificationType.PAPER, NotificationType.ADMIN);

        verify(notificationQueryPort, times(1)).getNotificationList(any());
        verifyNoMoreInteractions(notificationQueryPort);
    }

    @Test
    @DisplayName("알림 목록 조회 - 반환된 리스트가 null인 경우")
    void shouldHandleNullList_WhenPortReturnsNull() {
        // Given
        CustomUserDetails userDetails = TestFixtures.createCustomUserDetails(TestUsers.USER3);
        given(userDetails.getUserId()).willReturn(3L);
        given(notificationQueryPort.getNotificationList(any()))
                .willReturn(null);

        // When
        List<Notification> result = notificationQueryService.getNotificationList(userDetails);

        // Then
        assertThat(result).isNotNull(); // 서비스에서 빈 리스트 반환으로 null 안전성 보장
        assertThat(result).isEmpty();

        verify(notificationQueryPort, times(1)).getNotificationList(any());
        verifyNoMoreInteractions(notificationQueryPort);
    }
}