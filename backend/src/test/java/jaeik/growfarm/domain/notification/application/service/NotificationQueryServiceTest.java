package jaeik.growfarm.domain.notification.application.service;

import jaeik.growfarm.domain.notification.application.port.out.NotificationQueryPort;
import jaeik.growfarm.domain.notification.entity.NotificationType;
import jaeik.growfarm.infrastructure.adapter.notification.in.web.dto.NotificationDTO;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
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
        List<NotificationDTO> expectedNotifications = Arrays.asList(
                NotificationDTO.builder()
                        .id(1L)
                        .content("새로운 댓글이 달렸습니다.")
                        .url("/post/123")
                        .notificationType(NotificationType.COMMENT)
                        .isRead(false)
                        .createdAt(Instant.now())
                        .build(),
                NotificationDTO.builder()
                        .id(2L)
                        .content("게시글에 좋아요가 추가되었습니다.")
                        .url("/post/456")
                        .notificationType(NotificationType.LIKE)
                        .isRead(true)
                        .createdAt(Instant.now().minusSeconds(3600))
                        .build()
        );

        given(notificationQueryPort.getNotificationList(userDetails))
                .willReturn(expectedNotifications);

        // When
        List<NotificationDTO> result = notificationQueryService.getNotificationList(userDetails);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getContent()).isEqualTo("새로운 댓글이 달렸습니다.");
        assertThat(result.get(0).getNotificationType()).isEqualTo(NotificationType.COMMENT);
        assertThat(result.get(0).isRead()).isFalse();
        
        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(1).getContent()).isEqualTo("게시글에 좋아요가 추가되었습니다.");
        assertThat(result.get(1).getNotificationType()).isEqualTo(NotificationType.LIKE);
        assertThat(result.get(1).isRead()).isTrue();

        verify(notificationQueryPort, times(1)).getNotificationList(userDetails);
        verifyNoMoreInteractions(notificationQueryPort);
    }

    @Test
    @DisplayName("알림 목록 조회 - 빈 목록")
    void shouldGetNotificationList_WhenNoNotifications() {
        // Given
        List<NotificationDTO> emptyList = Collections.emptyList();
        given(notificationQueryPort.getNotificationList(userDetails))
                .willReturn(emptyList);

        // When
        List<NotificationDTO> result = notificationQueryService.getNotificationList(userDetails);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(notificationQueryPort, times(1)).getNotificationList(userDetails);
        verifyNoMoreInteractions(notificationQueryPort);
    }

    @Test
    @DisplayName("알림 목록 조회 - 단일 알림")
    void shouldGetNotificationList_WhenSingleNotification() {
        // Given
        List<NotificationDTO> singleNotification = Arrays.asList(
                NotificationDTO.builder()
                        .id(1L)
                        .content("새로운 메시지가 도착했습니다.")
                        .url("/paper/test")
                        .notificationType(NotificationType.MESSAGE)
                        .isRead(false)
                        .createdAt(Instant.now())
                        .build()
        );

        given(notificationQueryPort.getNotificationList(userDetails))
                .willReturn(singleNotification);

        // When
        List<NotificationDTO> result = notificationQueryService.getNotificationList(userDetails);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getContent()).isEqualTo("새로운 메시지가 도착했습니다.");
        assertThat(result.get(0).getNotificationType()).isEqualTo(NotificationType.MESSAGE);
        assertThat(result.get(0).isRead()).isFalse();

        verify(notificationQueryPort, times(1)).getNotificationList(userDetails);
        verifyNoMoreInteractions(notificationQueryPort);
    }

    @Test
    @DisplayName("알림 목록 조회 - 읽은 알림과 안읽은 알림 혼재")
    void shouldGetNotificationList_WhenMixedReadStatus() {
        // Given
        List<NotificationDTO> mixedNotifications = Arrays.asList(
                NotificationDTO.builder()
                        .id(1L)
                        .content("읽지 않은 알림")
                        .url("/test/1")
                        .notificationType(NotificationType.COMMENT)
                        .isRead(false)
                        .createdAt(Instant.now())
                        .build(),
                NotificationDTO.builder()
                        .id(2L)
                        .content("읽은 알림")
                        .url("/test/2")
                        .notificationType(NotificationType.LIKE)
                        .isRead(true)
                        .createdAt(Instant.now().minusSeconds(1800))
                        .build(),
                NotificationDTO.builder()
                        .id(3L)
                        .content("또 다른 읽지 않은 알림")
                        .url("/test/3")
                        .notificationType(NotificationType.MESSAGE)
                        .isRead(false)
                        .createdAt(Instant.now().minusSeconds(900))
                        .build()
        );

        given(notificationQueryPort.getNotificationList(userDetails))
                .willReturn(mixedNotifications);

        // When
        List<NotificationDTO> result = notificationQueryService.getNotificationList(userDetails);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        
        // 읽지 않은 알림 확인
        long unreadCount = result.stream().filter(n -> !n.isRead()).count();
        assertThat(unreadCount).isEqualTo(2);
        
        // 읽은 알림 확인
        long readCount = result.stream().filter(NotificationDTO::isRead).count();
        assertThat(readCount).isEqualTo(1);

        verify(notificationQueryPort, times(1)).getNotificationList(userDetails);
        verifyNoMoreInteractions(notificationQueryPort);
    }
}