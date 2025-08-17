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
        updateDto.setReadIds(List.of());
        updateDto.setDeletedIds(List.of());

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

    @Test
    @DisplayName("알림 일괄 업데이트 - null 사용자")
    void shouldBatchUpdate_WhenNullUser() {
        // Given
        CustomUserDetails nullUserDetails = null;
        UpdateNotificationDTO updateDto = new UpdateNotificationDTO();
        updateDto.setReadIds(Arrays.asList(1L, 2L));
        updateDto.setDeletedIds(List.of(3L));

        // When
        notificationCommandService.batchUpdate(nullUserDetails, updateDto);

        // Then
        verify(notificationCommandPort, times(1)).batchUpdate(nullUserDetails, updateDto);
        verifyNoMoreInteractions(notificationCommandPort);
    }

    @Test
    @DisplayName("알림 일괄 업데이트 - 대량 데이터")
    void shouldBatchUpdate_WhenLargeDataSet() {
        // Given
        UpdateNotificationDTO updateDto = new UpdateNotificationDTO();
        List<Long> largeReadIds = Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
        List<Long> largeDeletedIds = Arrays.asList(11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L);
        updateDto.setReadIds(largeReadIds);
        updateDto.setDeletedIds(largeDeletedIds);

        // When
        notificationCommandService.batchUpdate(userDetails, updateDto);

        // Then
        verify(notificationCommandPort, times(1)).batchUpdate(userDetails, updateDto);
        verifyNoMoreInteractions(notificationCommandPort);
    }

    @Test
    @DisplayName("알림 일괄 업데이트 - 음수 ID 값")
    void shouldBatchUpdate_WhenNegativeIds() {
        // Given
        UpdateNotificationDTO updateDto = new UpdateNotificationDTO();
        List<Long> readIds = Arrays.asList(-1L, -2L);
        List<Long> deletedIds = Arrays.asList(-3L, -4L);
        updateDto.setReadIds(readIds);
        updateDto.setDeletedIds(deletedIds);

        // When
        notificationCommandService.batchUpdate(userDetails, updateDto);

        // Then
        verify(notificationCommandPort, times(1)).batchUpdate(userDetails, updateDto);
        verifyNoMoreInteractions(notificationCommandPort);
    }

    @Test
    @DisplayName("알림 일괄 업데이트 - 0 값 ID")
    void shouldBatchUpdate_WhenZeroIds() {
        // Given
        UpdateNotificationDTO updateDto = new UpdateNotificationDTO();
        List<Long> readIds = List.of(0L);
        List<Long> deletedIds = List.of(0L);
        updateDto.setReadIds(readIds);
        updateDto.setDeletedIds(deletedIds);

        // When
        notificationCommandService.batchUpdate(userDetails, updateDto);

        // Then
        verify(notificationCommandPort, times(1)).batchUpdate(userDetails, updateDto);
        verifyNoMoreInteractions(notificationCommandPort);
    }

    @Test
    @DisplayName("알림 일괄 업데이트 - 매우 큰 ID 값")
    void shouldBatchUpdate_WhenVeryLargeIds() {
        // Given
        UpdateNotificationDTO updateDto = new UpdateNotificationDTO();
        List<Long> readIds = Arrays.asList(Long.MAX_VALUE, Long.MAX_VALUE - 1);
        List<Long> deletedIds = Arrays.asList(Long.MAX_VALUE - 2, Long.MAX_VALUE - 3);
        updateDto.setReadIds(readIds);
        updateDto.setDeletedIds(deletedIds);

        // When
        notificationCommandService.batchUpdate(userDetails, updateDto);

        // Then
        verify(notificationCommandPort, times(1)).batchUpdate(userDetails, updateDto);
        verifyNoMoreInteractions(notificationCommandPort);
    }

    @Test
    @DisplayName("알림 일괄 업데이트 - 중복 ID 값")
    void shouldBatchUpdate_WhenDuplicateIds() {
        // Given
        UpdateNotificationDTO updateDto = new UpdateNotificationDTO();
        List<Long> readIds = Arrays.asList(1L, 1L, 2L, 2L, 3L);
        List<Long> deletedIds = Arrays.asList(4L, 4L, 5L);
        updateDto.setReadIds(readIds);
        updateDto.setDeletedIds(deletedIds);

        // When
        notificationCommandService.batchUpdate(userDetails, updateDto);

        // Then
        verify(notificationCommandPort, times(1)).batchUpdate(userDetails, updateDto);
        verifyNoMoreInteractions(notificationCommandPort);
    }

    @Test
    @DisplayName("알림 일괄 업데이트 - read와 delete ID가 겹치는 경우")
    void shouldBatchUpdate_WhenReadAndDeleteIdsOverlap() {
        // Given
        UpdateNotificationDTO updateDto = new UpdateNotificationDTO();
        List<Long> readIds = Arrays.asList(1L, 2L, 3L);
        List<Long> deletedIds = Arrays.asList(2L, 3L, 4L); // 2L, 3L이 중복
        updateDto.setReadIds(readIds);
        updateDto.setDeletedIds(deletedIds);

        // When
        notificationCommandService.batchUpdate(userDetails, updateDto);

        // Then
        verify(notificationCommandPort, times(1)).batchUpdate(userDetails, updateDto);
        verifyNoMoreInteractions(notificationCommandPort);
    }

    @Test
    @DisplayName("알림 일괄 업데이트 - 둘 다 null인 경우")
    void shouldBatchUpdate_WhenBothListsAreNull() {
        // Given
        UpdateNotificationDTO updateDto = new UpdateNotificationDTO();
        updateDto.setReadIds(null);
        updateDto.setDeletedIds(null);

        // When
        notificationCommandService.batchUpdate(userDetails, updateDto);

        // Then
        verify(notificationCommandPort, times(1)).batchUpdate(userDetails, updateDto);
        verifyNoMoreInteractions(notificationCommandPort);
    }
}