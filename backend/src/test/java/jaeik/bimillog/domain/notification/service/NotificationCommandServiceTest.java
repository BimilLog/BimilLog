package jaeik.bimillog.domain.notification.service;

import jaeik.bimillog.domain.notification.application.port.out.NotificationCommandPort;
import jaeik.bimillog.domain.notification.application.service.NotificationCommandService;
import jaeik.bimillog.domain.notification.entity.NotificationUpdateVO;
import jaeik.bimillog.domain.notification.exception.NotificationCustomException;
import jaeik.bimillog.domain.notification.exception.NotificationErrorCode;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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
@Tag("fast")
class NotificationCommandServiceTest {

    @Mock
    private NotificationCommandPort notificationCommandPort;

    @InjectMocks
    private NotificationCommandService notificationCommandService;

    @Test
    @DisplayName("알림 일괄 업데이트 - null 사용자 예외 검증")
    void shouldThrowException_WhenNullUser() {
        // Given
        CustomUserDetails nullUserDetails = null;
        NotificationUpdateVO updateCommand = NotificationUpdateVO.of(Arrays.asList(1L, 2L), List.of(3L));

        // When & Then
        assertThatThrownBy(() -> notificationCommandService.batchUpdate(nullUserDetails, updateCommand))
                .isInstanceOf(NotificationCustomException.class)
                .hasFieldOrPropertyWithValue("notificationErrorCode", NotificationErrorCode.INVALID_USER_CONTEXT);

        verifyNoInteractions(notificationCommandPort);
    }

    @Test
    @DisplayName("알림 일괄 업데이트 - 정상 플로우")
    void shouldDelegateToPort_WhenUserPresent() {
        // Given
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        NotificationUpdateVO updateCommand = NotificationUpdateVO.of(List.of(10L), List.of(20L));
        given(userDetails.getUserId()).willReturn(42L);

        // When
        notificationCommandService.batchUpdate(userDetails, updateCommand);

        // Then
        verify(notificationCommandPort).batchUpdate(42L, updateCommand);
    }
}
