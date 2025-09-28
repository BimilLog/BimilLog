package jaeik.bimillog.domain.notification.service;

import jaeik.bimillog.domain.notification.application.port.out.NotificationCommandPort;
import jaeik.bimillog.domain.notification.application.service.NotificationCommandService;
import jaeik.bimillog.domain.notification.entity.NotificationUpdateVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * <h2>NotificationCommandService 테스트</h2>
 * <p>알림 명령 서비스의 핵심 비즈니스 규칙을 검증하는 단위 테스트</p>
 * <p>CLAUDE.md 가이드라인: 단순 위임이 아닌 핵심 비즈니스 검증만 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationCommandService 테스트")
@Tag("test")
class NotificationCommandServiceTest {

    @Mock
    private NotificationCommandPort notificationCommandPort;

    @InjectMocks
    private NotificationCommandService notificationCommandService;

    @Test
    @DisplayName("알림 일괄 업데이트 - null 사용자 처리")
    void shouldDelegateToPort_WhenNullUser() {
        // Given
        Long nullUserId = null;
        NotificationUpdateVO updateCommand = NotificationUpdateVO.of(Arrays.asList(1L, 2L), List.of(3L));

        // When
        notificationCommandService.batchUpdate(nullUserId, updateCommand);

        // Then
        verify(notificationCommandPort).batchUpdate(nullUserId, updateCommand);
    }

    @Test
    @DisplayName("알림 일괄 업데이트 - 정상 플로우")
    void shouldDelegateToPort_WhenUserPresent() {
        // Given
        Long userId = 42L;
        NotificationUpdateVO updateCommand = NotificationUpdateVO.of(List.of(10L), List.of(20L));

        // When
        notificationCommandService.batchUpdate(userId, updateCommand);

        // Then
        verify(notificationCommandPort).batchUpdate(userId, updateCommand);
    }
}
