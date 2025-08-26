package jaeik.growfarm.domain.notification.application.service;

import jaeik.growfarm.domain.notification.application.port.out.NotificationCommandPort;
import jaeik.growfarm.domain.notification.entity.NotificationUpdateCommand;
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
        List<Long> readIds = Arrays.asList(1L, 2L, 3L);
        List<Long> deletedIds = Arrays.asList(4L, 5L);
        NotificationUpdateCommand updateCommand = NotificationUpdateCommand.of(readIds, deletedIds);

        // When
        notificationCommandService.batchUpdate(userDetails, updateCommand);

        // Then
        verify(notificationCommandPort, times(1)).batchUpdate(userDetails, updateCommand);
        verifyNoMoreInteractions(notificationCommandPort);
    }

    @Test
    @DisplayName("알림 일괄 업데이트 - 읽기 ID만 있는 경우")
    void shouldBatchUpdate_WhenOnlyReadIds() {
        // Given
        List<Long> readIds = Arrays.asList(1L, 2L);
        NotificationUpdateCommand updateCommand = NotificationUpdateCommand.of(readIds, null);

        // When
        notificationCommandService.batchUpdate(userDetails, updateCommand);

        // Then
        verify(notificationCommandPort, times(1)).batchUpdate(userDetails, updateCommand);
        verifyNoMoreInteractions(notificationCommandPort);
    }

    @Test
    @DisplayName("알림 일괄 업데이트 - 삭제 ID만 있는 경우")
    void shouldBatchUpdate_WhenOnlyDeletedIds() {
        // Given
        List<Long> deletedIds = Arrays.asList(3L, 4L, 5L);
        NotificationUpdateCommand updateCommand = NotificationUpdateCommand.of(null, deletedIds);

        // When
        notificationCommandService.batchUpdate(userDetails, updateCommand);

        // Then
        verify(notificationCommandPort, times(1)).batchUpdate(userDetails, updateCommand);
        verifyNoMoreInteractions(notificationCommandPort);
    }

    @Test
    @DisplayName("알림 일괄 업데이트 - 빈 리스트인 경우")
    void shouldBatchUpdate_WhenEmptyLists() {
        // Given
        NotificationUpdateCommand updateCommand = NotificationUpdateCommand.of(List.of(), List.of());

        // When
        notificationCommandService.batchUpdate(userDetails, updateCommand);

        // Then
        verify(notificationCommandPort, times(1)).batchUpdate(userDetails, updateCommand);
        verifyNoMoreInteractions(notificationCommandPort);
    }

    @Test
    @DisplayName("알림 일괄 업데이트 - null DTO인 경우")
    void shouldBatchUpdate_WhenNullDto() {
        // Given
        NotificationUpdateCommand updateCommand = null;

        // When
        notificationCommandService.batchUpdate(userDetails, updateCommand);

        // Then
        verify(notificationCommandPort, times(1)).batchUpdate(userDetails, updateCommand);
        verifyNoMoreInteractions(notificationCommandPort);
    }

    @Test
    @DisplayName("알림 일괄 업데이트 - null 사용자")
    void shouldBatchUpdate_WhenNullUser() {
        // Given
        CustomUserDetails nullUserDetails = null;
        NotificationUpdateCommand updateCommand = NotificationUpdateCommand.of(Arrays.asList(1L, 2L), List.of(3L));

        // When
        notificationCommandService.batchUpdate(nullUserDetails, updateCommand);

        // Then
        verify(notificationCommandPort, times(1)).batchUpdate(nullUserDetails, updateCommand);
        verifyNoMoreInteractions(notificationCommandPort);
    }

    @Test
    @DisplayName("알림 일괄 업데이트 - 대량 데이터")
    void shouldBatchUpdate_WhenLargeDataSet() {
        // Given
        List<Long> largeReadIds = Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
        List<Long> largeDeletedIds = Arrays.asList(11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L);
        NotificationUpdateCommand updateCommand = NotificationUpdateCommand.of(largeReadIds, largeDeletedIds);

        // When
        notificationCommandService.batchUpdate(userDetails, updateCommand);

        // Then
        verify(notificationCommandPort, times(1)).batchUpdate(userDetails, updateCommand);
        verifyNoMoreInteractions(notificationCommandPort);
    }

    @Test
    @DisplayName("알림 일괄 업데이트 - 음수 ID 값")
    void shouldBatchUpdate_WhenNegativeIds() {
        // Given
        List<Long> readIds = Arrays.asList(-1L, -2L);
        List<Long> deletedIds = Arrays.asList(-3L, -4L);
        NotificationUpdateCommand updateCommand = NotificationUpdateCommand.of(readIds, deletedIds);

        // When
        notificationCommandService.batchUpdate(userDetails, updateCommand);

        // Then
        verify(notificationCommandPort, times(1)).batchUpdate(userDetails, updateCommand);
        verifyNoMoreInteractions(notificationCommandPort);
    }

    @Test
    @DisplayName("알림 일괄 업데이트 - 0 값 ID")
    void shouldBatchUpdate_WhenZeroIds() {
        // Given
        List<Long> readIds = List.of(0L);
        List<Long> deletedIds = List.of(0L);
        NotificationUpdateCommand updateCommand = NotificationUpdateCommand.of(readIds, deletedIds);

        // When
        notificationCommandService.batchUpdate(userDetails, updateCommand);

        // Then
        verify(notificationCommandPort, times(1)).batchUpdate(userDetails, updateCommand);
        verifyNoMoreInteractions(notificationCommandPort);
    }

    @Test
    @DisplayName("알림 일괄 업데이트 - 매우 큰 ID 값")
    void shouldBatchUpdate_WhenVeryLargeIds() {
        // Given
        List<Long> readIds = Arrays.asList(Long.MAX_VALUE, Long.MAX_VALUE - 1);
        List<Long> deletedIds = Arrays.asList(Long.MAX_VALUE - 2, Long.MAX_VALUE - 3);
        NotificationUpdateCommand updateCommand = NotificationUpdateCommand.of(readIds, deletedIds);

        // When
        notificationCommandService.batchUpdate(userDetails, updateCommand);

        // Then
        verify(notificationCommandPort, times(1)).batchUpdate(userDetails, updateCommand);
        verifyNoMoreInteractions(notificationCommandPort);
    }

    @Test
    @DisplayName("알림 일괄 업데이트 - 중복 ID 값")
    void shouldBatchUpdate_WhenDuplicateIds() {
        // Given
        List<Long> readIds = Arrays.asList(1L, 1L, 2L, 2L, 3L);
        List<Long> deletedIds = Arrays.asList(4L, 4L, 5L);
        NotificationUpdateCommand updateCommand = NotificationUpdateCommand.of(readIds, deletedIds);

        // When
        notificationCommandService.batchUpdate(userDetails, updateCommand);

        // Then
        verify(notificationCommandPort, times(1)).batchUpdate(userDetails, updateCommand);
        verifyNoMoreInteractions(notificationCommandPort);
    }

    @Test
    @DisplayName("알림 일괄 업데이트 - read와 delete ID가 겹치는 경우")
    void shouldBatchUpdate_WhenReadAndDeleteIdsOverlap() {
        // Given
        List<Long> readIds = Arrays.asList(1L, 2L, 3L);
        List<Long> deletedIds = Arrays.asList(2L, 3L, 4L); // 2L, 3L이 중복
        NotificationUpdateCommand updateCommand = NotificationUpdateCommand.of(readIds, deletedIds);

        // When
        notificationCommandService.batchUpdate(userDetails, updateCommand);

        // Then
        verify(notificationCommandPort, times(1)).batchUpdate(userDetails, updateCommand);
        verifyNoMoreInteractions(notificationCommandPort);
    }

    @Test
    @DisplayName("알림 일괄 업데이트 - 둘 다 null인 경우")
    void shouldBatchUpdate_WhenBothListsAreNull() {
        // Given
        NotificationUpdateCommand updateCommand = NotificationUpdateCommand.of(null, null);

        // When
        notificationCommandService.batchUpdate(userDetails, updateCommand);

        // Then
        verify(notificationCommandPort, times(1)).batchUpdate(userDetails, updateCommand);
        verifyNoMoreInteractions(notificationCommandPort);
    }
}