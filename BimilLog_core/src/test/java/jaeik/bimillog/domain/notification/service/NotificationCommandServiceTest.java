package jaeik.bimillog.domain.notification.service;

import jaeik.bimillog.domain.notification.entity.Notification;
import jaeik.bimillog.domain.notification.out.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
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
@Tag("unit")
class NotificationCommandServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationCommandService notificationCommandService;

    @Test
    @DisplayName("알림 일괄 업데이트 - null 사용자 처리")
    void shouldDelegateToPort_WhenNullUser() {
        // Given
        Long nullUserId = null;
        NotificationUpdateVO updateCommand = NotificationUpdateVO.of(Arrays.asList(1L, 2L), List.of(3L));
        given(notificationRepository.findAllByIdInAndMember_Id(anyList(), any())).willReturn(Collections.emptyList());

        // When
        notificationCommandService.batchUpdate(nullUserId, updateCommand);

        // Then
        verify(notificationRepository).deleteAllByIdInAndMember_Id(anyList(), any());
        verify(notificationRepository).findAllByIdInAndMember_Id(anyList(), any());
    }

    @Test
    @DisplayName("알림 일괄 업데이트 - 정상 플로우")
    void shouldDelegateToPort_WhenUserPresent() {
        // Given
        Long memberId = 42L;
        NotificationUpdateVO updateCommand = NotificationUpdateVO.of(List.of(10L), List.of(20L));
        Notification notification = mock(Notification.class);
        given(notificationRepository.findAllByIdInAndMember_Id(anyList(), anyLong())).willReturn(List.of(notification));

        // When
        notificationCommandService.batchUpdate(memberId, updateCommand);

        // Then
        verify(notificationRepository).deleteAllByIdInAndMember_Id(anyList(), anyLong());
        verify(notificationRepository).findAllByIdInAndMember_Id(anyList(), anyLong());
        verify(notification).markAsRead();
    }
}
