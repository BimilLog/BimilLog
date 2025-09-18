package jaeik.bimillog.domain.notification.service;

import jaeik.bimillog.domain.notification.application.port.out.NotificationCommandPort;
import jaeik.bimillog.domain.notification.application.service.NotificationCommandService;
import jaeik.bimillog.domain.notification.entity.NotificationUpdateVO;
import jaeik.bimillog.domain.notification.exception.NotificationCustomException;
import jaeik.bimillog.domain.notification.exception.NotificationErrorCode;
import jaeik.bimillog.infrastructure.adapter.auth.out.auth.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * <h2>NotificationCommandService 테스트</h2>
 * <p>알림 명령 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationCommandService 테스트")
class NotificationCommandServiceTest {

    @Mock
    private NotificationCommandPort notificationCommandPort;

    @Mock
    private CustomUserDetails userDetails;

    @InjectMocks
    private NotificationCommandService notificationCommandService;

    @Test
    @DisplayName("알림 일괄 업데이트 - 성공")
    void shouldBatchUpdate_WhenValidInput() {
        // Given
        when(userDetails.getUserId()).thenReturn(1L);
        List<Long> readIds = Arrays.asList(1L, 2L, 3L);
        List<Long> deletedIds = Arrays.asList(4L, 5L);
        NotificationUpdateVO updateCommand = NotificationUpdateVO.of(readIds, deletedIds);

        // When
        notificationCommandService.batchUpdate(userDetails, updateCommand);

        // Then
        verify(notificationCommandPort, times(1)).batchUpdate(eq(1L), any(NotificationUpdateVO.class));
        verifyNoMoreInteractions(notificationCommandPort);
    }

    @Test
    @DisplayName("알림 일괄 업데이트 - 읽기 ID만 있는 경우")
    void shouldBatchUpdate_WhenOnlyReadIds() {
        // Given
        when(userDetails.getUserId()).thenReturn(1L);
        List<Long> readIds = Arrays.asList(1L, 2L);
        NotificationUpdateVO updateCommand = NotificationUpdateVO.of(readIds, null);

        // When
        notificationCommandService.batchUpdate(userDetails, updateCommand);

        // Then
        verify(notificationCommandPort, times(1)).batchUpdate(eq(1L), any(NotificationUpdateVO.class));
        verifyNoMoreInteractions(notificationCommandPort);
    }

    @Test
    @DisplayName("알림 일괄 업데이트 - 삭제 ID만 있는 경우")
    void shouldBatchUpdate_WhenOnlyDeletedIds() {
        // Given
        when(userDetails.getUserId()).thenReturn(1L);
        List<Long> deletedIds = Arrays.asList(3L, 4L, 5L);
        NotificationUpdateVO updateCommand = NotificationUpdateVO.of(null, deletedIds);

        // When
        notificationCommandService.batchUpdate(userDetails, updateCommand);

        // Then
        verify(notificationCommandPort, times(1)).batchUpdate(eq(1L), any(NotificationUpdateVO.class));
        verifyNoMoreInteractions(notificationCommandPort);
    }

    @Test
    @DisplayName("알림 일괄 업데이트 - null 사용자")
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
}