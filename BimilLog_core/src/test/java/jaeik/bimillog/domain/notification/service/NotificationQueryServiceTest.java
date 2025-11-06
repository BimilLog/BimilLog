package jaeik.bimillog.domain.notification.service;

import jaeik.bimillog.domain.notification.application.port.out.NotificationQueryPort;
import jaeik.bimillog.domain.notification.entity.Notification;
import jaeik.bimillog.out.auth.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>NotificationQueryService 테스트</h2>
 * <p>알림 조회 서비스의 핵심 null 안전성을 검증하는 단위 테스트</p>
 * <p>CLAUDE.md 가이드라인: 단순 위임이 아닌 null 처리 로직만 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationQueryService 테스트")
@Tag("unit")
class NotificationQueryServiceTest {

    @Mock
    private NotificationQueryPort notificationQueryPort;

    @InjectMocks
    private NotificationQueryService notificationQueryService;

    @Test
    @DisplayName("알림 목록 조회 - null 사용자 안전 처리")
    void shouldReturnEmptyList_WhenNullUser() {
        // Given
        CustomUserDetails nullUserDetails = null;

        // When
        List<Notification> result = notificationQueryService.getNotificationList(nullUserDetails);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verifyNoInteractions(notificationQueryPort);
    }

    @Test
    @DisplayName("알림 목록 조회 - 포트가 null 반환시 빈 리스트 반환")
    void shouldReturnEmptyList_WhenPortReturnsNull() {
        // Given
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        given(userDetails.getMemberId()).willReturn(1L);
        given(notificationQueryPort.getNotificationList(1L)).willReturn(null);

        // When
        List<Notification> result = notificationQueryService.getNotificationList(userDetails);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(notificationQueryPort).getNotificationList(1L);
    }
}