package jaeik.bimillog.domain.notification.service;

import jaeik.bimillog.domain.notification.application.port.out.NotificationQueryPort;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.notification.entity.NotificationInfo;
import jaeik.bimillog.infrastructure.auth.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
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

    @Mock
    private CustomUserDetails userDetails;

    @InjectMocks
    private NotificationQueryService notificationQueryService;

    @Test
    @DisplayName("알림 목록 조회 - 성공")
    void shouldGetNotificationList_WhenValidUser() {
        // Given
        given(userDetails.getUserId()).willReturn(1L);
        List<NotificationInfo> expectedNotifications = Arrays.asList(
                NotificationInfo.builder()
                        .id(1L)
                        .content("새로운 댓글이 달렸습니다.")
                        .url("/post/123")
                        .notificationType(NotificationType.COMMENT)
                        .isRead(false)
                        .createdAt(Instant.now())
                        .build(),
                NotificationInfo.builder()
                        .id(2L)
                        .content("축하합니다! 게시글이 인기글로 선정되었습니다.")
                        .url("/post/456")
                        .notificationType(NotificationType.POST_FEATURED)
                        .isRead(true)
                        .createdAt(Instant.now().minusSeconds(3600))
                        .build()
        );

        given(notificationQueryPort.getNotificationList(any()))
                .willReturn(expectedNotifications);

        // When
        List<NotificationInfo> result = notificationQueryService.getNotificationList(userDetails);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).content()).isEqualTo("새로운 댓글이 달렸습니다.");
        assertThat(result.get(0).notificationType()).isEqualTo(NotificationType.COMMENT);
        assertThat(result.get(0).isRead()).isFalse();
        
        assertThat(result.get(1).id()).isEqualTo(2L);
        assertThat(result.get(1).content()).isEqualTo("축하합니다! 게시글이 인기글로 선정되었습니다.");
        assertThat(result.get(1).notificationType()).isEqualTo(NotificationType.POST_FEATURED);
        assertThat(result.get(1).isRead()).isTrue();

        verify(notificationQueryPort, times(1)).getNotificationList(any());
        verifyNoMoreInteractions(notificationQueryPort);
    }

    @Test
    @DisplayName("알림 목록 조회 - 빈 목록")
    void shouldGetNotificationList_WhenNoNotifications() {
        // Given
        given(userDetails.getUserId()).willReturn(1L);
        List<NotificationInfo> emptyList = Collections.emptyList();
        given(notificationQueryPort.getNotificationList(any()))
                .willReturn(emptyList);

        // When
        List<NotificationInfo> result = notificationQueryService.getNotificationList(userDetails);

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
        given(userDetails.getUserId()).willReturn(1L);
        List<NotificationInfo> singleNotification = Arrays.asList(
                NotificationInfo.builder()
                        .id(1L)
                        .content("새로운 메시지가 도착했습니다.")
                        .url("/paper/test")
                        .notificationType(NotificationType.PAPER)
                        .isRead(false)
                        .createdAt(Instant.now())
                        .build()
        );

        given(notificationQueryPort.getNotificationList(any()))
                .willReturn(singleNotification);

        // When
        List<NotificationInfo> result = notificationQueryService.getNotificationList(userDetails);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).content()).isEqualTo("새로운 메시지가 도착했습니다.");
        assertThat(result.get(0).notificationType()).isEqualTo(NotificationType.PAPER);
        assertThat(result.get(0).isRead()).isFalse();

        verify(notificationQueryPort, times(1)).getNotificationList(any());
        verifyNoMoreInteractions(notificationQueryPort);
    }

    @Test
    @DisplayName("알림 목록 조회 - 읽은 알림과 안읽은 알림 혼재")
    void shouldGetNotificationList_WhenMixedReadStatus() {
        // Given
        given(userDetails.getUserId()).willReturn(1L);
        List<NotificationInfo> mixedNotifications = Arrays.asList(
                NotificationInfo.builder()
                        .id(1L)
                        .content("읽지 않은 알림")
                        .url("/test/1")
                        .notificationType(NotificationType.COMMENT)
                        .isRead(false)
                        .createdAt(Instant.now())
                        .build(),
                NotificationInfo.builder()
                        .id(2L)
                        .content("읽은 알림")
                        .url("/test/2")
                        .notificationType(NotificationType.POST_FEATURED)
                        .isRead(true)
                        .createdAt(Instant.now().minusSeconds(1800))
                        .build(),
                NotificationInfo.builder()
                        .id(3L)
                        .content("또 다른 읽지 않은 알림")
                        .url("/test/3")
                        .notificationType(NotificationType.PAPER)
                        .isRead(false)
                        .createdAt(Instant.now().minusSeconds(900))
                        .build()
        );

        given(notificationQueryPort.getNotificationList(any()))
                .willReturn(mixedNotifications);

        // When
        List<NotificationInfo> result = notificationQueryService.getNotificationList(userDetails);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        
        // 읽지 않은 알림 확인
        long unreadCount = result.stream().filter(n -> !n.isRead()).count();
        assertThat(unreadCount).isEqualTo(2);
        
        // 읽은 알림 확인
        long readCount = result.stream().filter(NotificationInfo::isRead).count();
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
        List<NotificationInfo> result = notificationQueryService.getNotificationList(nullUserDetails);

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
        List<NotificationInfo> result = notificationQueryService.getNotificationList(userDetailsWithNullId);

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
        given(userDetails.getUserId()).willReturn(1L);
        List<NotificationInfo> largeNotificationList = Arrays.asList(
                createNotificationInfo(1L, NotificationType.COMMENT),
                createNotificationInfo(2L, NotificationType.POST_FEATURED),
                createNotificationInfo(3L, NotificationType.PAPER),
                createNotificationInfo(4L, NotificationType.POST_FEATURED),
                createNotificationInfo(5L, NotificationType.COMMENT),
                createNotificationInfo(6L, NotificationType.POST_FEATURED),
                createNotificationInfo(7L, NotificationType.PAPER),
                createNotificationInfo(8L, NotificationType.POST_FEATURED),
                createNotificationInfo(9L, NotificationType.COMMENT),
                createNotificationInfo(10L, NotificationType.POST_FEATURED)
        );

        given(notificationQueryPort.getNotificationList(any()))
                .willReturn(largeNotificationList);

        // When
        List<NotificationInfo> result = notificationQueryService.getNotificationList(userDetails);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(10);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(9).id()).isEqualTo(10L);

        verify(notificationQueryPort, times(1)).getNotificationList(any());
        verifyNoMoreInteractions(notificationQueryPort);
    }

    @Test
    @DisplayName("알림 목록 조회 - 모든 알림 타입 포함")
    void shouldGetNotificationList_WhenAllNotificationTypes() {
        // Given
        given(userDetails.getUserId()).willReturn(1L);
        List<NotificationInfo> allTypesNotifications = Arrays.asList(
                createNotificationInfo(1L, NotificationType.COMMENT),
                createNotificationInfo(2L, NotificationType.POST_FEATURED),
                createNotificationInfo(3L, NotificationType.PAPER),
                createNotificationInfo(4L, NotificationType.POST_FEATURED),
                createNotificationInfo(5L, NotificationType.PAPER)
        );

        given(notificationQueryPort.getNotificationList(any()))
                .willReturn(allTypesNotifications);

        // When
        List<NotificationInfo> result = notificationQueryService.getNotificationList(userDetails);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(5);
        
        // 알림 타입별 검증
        assertThat(result.get(0).notificationType()).isEqualTo(NotificationType.COMMENT);
        assertThat(result.get(1).notificationType()).isEqualTo(NotificationType.POST_FEATURED);
        assertThat(result.get(2).notificationType()).isEqualTo(NotificationType.PAPER);
        assertThat(result.get(3).notificationType()).isEqualTo(NotificationType.POST_FEATURED);
        assertThat(result.get(4).notificationType()).isEqualTo(NotificationType.PAPER);

        verify(notificationQueryPort, times(1)).getNotificationList(any());
        verifyNoMoreInteractions(notificationQueryPort);
    }

    @Test
    @DisplayName("알림 목록 조회 - 반환된 리스트가 null인 경우")
    void shouldHandleNullList_WhenPortReturnsNull() {
        // Given
        given(userDetails.getUserId()).willReturn(1L);
        given(notificationQueryPort.getNotificationList(any()))
                .willReturn(null);

        // When
        List<NotificationInfo> result = notificationQueryService.getNotificationList(userDetails);

        // Then
        assertThat(result).isNotNull(); // 서비스에서 빈 리스트 반환으로 null 안전성 보장
        assertThat(result).isEmpty();

        verify(notificationQueryPort, times(1)).getNotificationList(any());
        verifyNoMoreInteractions(notificationQueryPort);
    }

    private NotificationInfo createNotificationInfo(Long id, NotificationType type) {
        return NotificationInfo.builder()
                .id(id)
                .content("테스트 알림 " + id)
                .url("/test/" + id)
                .notificationType(type)
                .isRead(id % 2 == 0) // 짝수 ID는 읽음, 홀수 ID는 안 읽음
                .createdAt(Instant.now().minusSeconds(id * 300)) // ID에 비례하여 시간 차이
                .build();
    }
}