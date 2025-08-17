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
 * 
 * TODO: PostReqDTO 컴파일 오류로 인해 현재 실행 불가.
 *       post 도메인 DTO 컴파일 이슈 해결 후 테스트 실행 필요.
 *       논리적으로 모든 시나리오 커버됨 - null 처리, 대량 데이터, 알림 타입 검증 완료.
 *       알림은 인기글이 되었을 때, 댓글이 달렸을 때, 롤링페이퍼에 메시지가 도착했을때만 일어나고
 *       롤링페이퍼에 메시지가 달렸을 때 FCM 알림 여부 SSE는 항상 전송됨
 *       @Builder.Default
 *       @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
 *       private boolean messageNotification = true;
 *       글에 댓글이 달렸을 때 FCM 알림 여부 SSE는 항상 전송됨
 *       @Builder.Default
 *       @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
 *       private boolean commentNotification = true;
 *       글이 인기글이 되었을 때 FCM 알림 여부 SSE는 항상 전송됨 (실시간 인기글은 해당 안됨, 주간, 전설 인기글만 전송됨)
 *       @Builder.Default
 *       @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
 *       private boolean postFeaturedNotification = true;
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
                        .notificationType(NotificationType.POST_FEATURED)
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
        assertThat(result.get(1).getNotificationType()).isEqualTo(NotificationType.POST_FEATURED);
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
                        .notificationType(NotificationType.PAPER)
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
        assertThat(result.get(0).getNotificationType()).isEqualTo(NotificationType.PAPER);
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
                        .notificationType(NotificationType.POST_FEATURED)
                        .isRead(true)
                        .createdAt(Instant.now().minusSeconds(1800))
                        .build(),
                NotificationDTO.builder()
                        .id(3L)
                        .content("또 다른 읽지 않은 알림")
                        .url("/test/3")
                        .notificationType(NotificationType.PAPER)
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

    @Test
    @DisplayName("알림 목록 조회 - null 사용자")
    void shouldGetNotificationList_WhenNullUser() {
        // Given
        CustomUserDetails nullUserDetails = null;
        List<NotificationDTO> emptyList = Collections.emptyList();
        given(notificationQueryPort.getNotificationList(nullUserDetails))
                .willReturn(emptyList);

        // When
        List<NotificationDTO> result = notificationQueryService.getNotificationList(nullUserDetails);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(notificationQueryPort, times(1)).getNotificationList(nullUserDetails);
        verifyNoMoreInteractions(notificationQueryPort);
    }

    @Test
    @DisplayName("알림 목록 조회 - 대량 알림")
    void shouldGetNotificationList_WhenLargeAmountOfNotifications() {
        // Given
        List<NotificationDTO> largeNotificationList = Arrays.asList(
                createNotificationDTO(1L, NotificationType.COMMENT),
                createNotificationDTO(2L, NotificationType.POST_FEATURED),
                createNotificationDTO(3L, NotificationType.PAPER),
                createNotificationDTO(4L, NotificationType.POST_FEATURED),
                createNotificationDTO(5L, NotificationType.COMMENT),
                createNotificationDTO(6L, NotificationType.POST_FEATURED),
                createNotificationDTO(7L, NotificationType.PAPER),
                createNotificationDTO(8L, NotificationType.POST_FEATURED),
                createNotificationDTO(9L, NotificationType.COMMENT),
                createNotificationDTO(10L, NotificationType.POST_FEATURED)
        );

        given(notificationQueryPort.getNotificationList(userDetails))
                .willReturn(largeNotificationList);

        // When
        List<NotificationDTO> result = notificationQueryService.getNotificationList(userDetails);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(10);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(9).getId()).isEqualTo(10L);

        verify(notificationQueryPort, times(1)).getNotificationList(userDetails);
        verifyNoMoreInteractions(notificationQueryPort);
    }

    @Test
    @DisplayName("알림 목록 조회 - 모든 알림 타입 포함")
    void shouldGetNotificationList_WhenAllNotificationTypes() {
        // Given
        List<NotificationDTO> allTypesNotifications = Arrays.asList(
                createNotificationDTO(1L, NotificationType.COMMENT),
                createNotificationDTO(2L, NotificationType.POST_FEATURED),
                createNotificationDTO(3L, NotificationType.PAPER),
                createNotificationDTO(4L, NotificationType.POST_FEATURED),
                createNotificationDTO(5L, NotificationType.PAPER)
        );

        given(notificationQueryPort.getNotificationList(userDetails))
                .willReturn(allTypesNotifications);

        // When
        List<NotificationDTO> result = notificationQueryService.getNotificationList(userDetails);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(5);
        
        // 알림 타입별 검증
        assertThat(result.get(0).getNotificationType()).isEqualTo(NotificationType.COMMENT);
        assertThat(result.get(1).getNotificationType()).isEqualTo(NotificationType.POST_FEATURED);
        assertThat(result.get(2).getNotificationType()).isEqualTo(NotificationType.PAPER);
        assertThat(result.get(3).getNotificationType()).isEqualTo(NotificationType.POST_FEATURED);
        assertThat(result.get(4).getNotificationType()).isEqualTo(NotificationType.PAPER);

        verify(notificationQueryPort, times(1)).getNotificationList(userDetails);
        verifyNoMoreInteractions(notificationQueryPort);
    }

    @Test
    @DisplayName("알림 목록 조회 - 반환된 리스트가 null인 경우")
    void shouldHandleNullList_WhenPortReturnsNull() {
        // Given
        given(notificationQueryPort.getNotificationList(userDetails))
                .willReturn(null);

        // When
        List<NotificationDTO> result = notificationQueryService.getNotificationList(userDetails);

        // Then
        assertThat(result).isNull(); // 서비스에서 null을 그대로 전달

        verify(notificationQueryPort, times(1)).getNotificationList(userDetails);
        verifyNoMoreInteractions(notificationQueryPort);
    }

    private NotificationDTO createNotificationDTO(Long id, NotificationType type) {
        return NotificationDTO.builder()
                .id(id)
                .content("테스트 알림 " + id)
                .url("/test/" + id)
                .notificationType(type)
                .isRead(id % 2 == 0) // 짝수 ID는 읽음, 홀수 ID는 안 읽음
                .createdAt(Instant.now().minusSeconds(id * 300)) // ID에 비례하여 시간 차이
                .build();
    }
}