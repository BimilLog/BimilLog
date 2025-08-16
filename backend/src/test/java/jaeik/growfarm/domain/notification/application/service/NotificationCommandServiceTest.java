package jaeik.growfarm.domain.notification.application.service;

import jaeik.growfarm.domain.notification.application.port.out.NotificationCommandPort;
import jaeik.growfarm.infrastructure.adapter.notification.in.web.dto.UpdateNotificationDTO;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.mockito.BDDMockito.given;
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
        UpdateNotificationDTO updateDto = new UpdateNotificationDTO();
        List<Long> readIds = Arrays.asList(1L, 2L, 3L);
        List<Long> deletedIds = Arrays.asList(4L, 5L);
        updateDto.setReadIds(readIds);
        updateDto.setDeletedIds(deletedIds);

        // When
        notificationCommandService.batchUpdate(userDetails, updateDto);

        // Then
        verify(notificationCommandPort, times(1)).batchUpdate(userDetails, updateDto);
        verifyNoMoreInteractions(notificationCommandPort);
    }

    @Test
    @DisplayName("알림 일괄 업데이트 - 읽기 ID만 있는 경우")
    void shouldBatchUpdate_WhenOnlyReadIds() {
        // Given
        UpdateNotificationDTO updateDto = new UpdateNotificationDTO();
        List<Long> readIds = Arrays.asList(1L, 2L);
        updateDto.setReadIds(readIds);
        updateDto.setDeletedIds(null);

        // When
        notificationCommandService.batchUpdate(userDetails, updateDto);

        // Then
        verify(notificationCommandPort, times(1)).batchUpdate(userDetails, updateDto);
        verifyNoMoreInteractions(notificationCommandPort);
    }

    @Test
    @DisplayName("알림 일괄 업데이트 - 삭제 ID만 있는 경우")
    void shouldBatchUpdate_WhenOnlyDeletedIds() {
        // Given
        UpdateNotificationDTO updateDto = new UpdateNotificationDTO();
        List<Long> deletedIds = Arrays.asList(3L, 4L, 5L);
        updateDto.setReadIds(null);
        updateDto.setDeletedIds(deletedIds);

        // When
        notificationCommandService.batchUpdate(userDetails, updateDto);

        // Then
        verify(notificationCommandPort, times(1)).batchUpdate(userDetails, updateDto);
        verifyNoMoreInteractions(notificationCommandPort);
    }

    @Test
    @DisplayName("알림 일괄 업데이트 - 빈 리스트인 경우")
    void shouldBatchUpdate_WhenEmptyLists() {
        // Given
        UpdateNotificationDTO updateDto = new UpdateNotificationDTO();
        updateDto.setReadIds(Arrays.asList());
        updateDto.setDeletedIds(Arrays.asList());

        // When
        notificationCommandService.batchUpdate(userDetails, updateDto);

        // Then
        verify(notificationCommandPort, times(1)).batchUpdate(userDetails, updateDto);
        verifyNoMoreInteractions(notificationCommandPort);
    }

    @Test
    @DisplayName("알림 일괄 업데이트 - null DTO인 경우")
    void shouldBatchUpdate_WhenNullDto() {
        // Given
        UpdateNotificationDTO updateDto = null;

        // When
        notificationCommandService.batchUpdate(userDetails, updateDto);

        // Then
        verify(notificationCommandPort, times(1)).batchUpdate(userDetails, updateDto);
        verifyNoMoreInteractions(notificationCommandPort);
    }
}