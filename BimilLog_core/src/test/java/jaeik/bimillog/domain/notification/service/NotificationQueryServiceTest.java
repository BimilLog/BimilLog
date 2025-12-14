package jaeik.bimillog.domain.notification.service;

import jaeik.bimillog.domain.global.entity.CustomUserDetails;
import jaeik.bimillog.domain.notification.entity.Notification;
import jaeik.bimillog.domain.notification.out.NotificationRepository;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.fixtures.AuthTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
@DisplayName("NotificationQueryService 테스트")
@Tag("unit")
class NotificationQueryServiceTest extends BaseUnitTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationQueryService notificationQueryService;

    @Test
    @DisplayName("알림 목록 조회 - null 사용자 예외 발생")
    void shouldThrowException_WhenNullUser() {
        // Given
        CustomUserDetails nullUserDetails = null;

        // When & Then
        assertThatThrownBy(() -> notificationQueryService.getNotificationList(nullUserDetails))
                .isInstanceOf(NullPointerException.class);

        verifyNoInteractions(notificationRepository);
    }

    @Test
    @DisplayName("알림 목록 조회 - Repository가 빈 리스트 반환")
    void shouldReturnEmptyList_WhenRepositoryReturnsEmptyList() {
        // Given
        CustomUserDetails userDetails = AuthTestFixtures.createCustomUserDetails(getTestMember());
        given(notificationRepository.getNotificationList(userDetails.getMemberId()))
                .willReturn(List.of());

        // When
        List<Notification> result = notificationQueryService.getNotificationList(userDetails);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(notificationRepository).getNotificationList(userDetails.getMemberId());
    }
}
