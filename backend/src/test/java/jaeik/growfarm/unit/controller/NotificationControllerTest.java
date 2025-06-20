package jaeik.growfarm.unit.controller;

import jaeik.growfarm.controller.NotificationController;
import jaeik.growfarm.dto.notification.NotificationDTO;
import jaeik.growfarm.dto.notification.UpdateNotificationDTO;
import jaeik.growfarm.entity.notification.NotificationType;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.service.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@SpringBootTest
public class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        // Setup mock data
        userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUserId()).thenReturn(1L);
        when(userDetails.getTokenId()).thenReturn(1L);
    }

    @Test
    @DisplayName("알림 리스트 조회 테스트")
    void testGetNotifications() {
        // Given
        NotificationDTO notificationDTO = NotificationDTO.builder()
                .id(1L)
                .data("Test notification")
                .url("/test/url")
                .type(NotificationType.COMMENT)
                .isRead(false)
                .createdAt(Instant.now())
                .build();

        List<NotificationDTO> notificationDTOList = new ArrayList<>();
        notificationDTOList.add(notificationDTO);

        when(notificationService.getNotificationList(any())).thenReturn(notificationDTOList);

        // When
        ResponseEntity<List<NotificationDTO>> response = notificationController.getNotifications(userDetails);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("Test notification", response.getBody().getFirst().getData());
        verify(notificationService, times(1)).getNotificationList(userDetails);
    }

    @Test
    @DisplayName("알림 읽음/삭제 처리 테스트")
    void testMarkAsRead() {
        // Given
        UpdateNotificationDTO updateNotificationDTO = new UpdateNotificationDTO();
        updateNotificationDTO.setReadIds(List.of(1L));
        updateNotificationDTO.setDeletedIds(List.of(2L));

        doNothing().when(notificationService).batchUpdate(any(), any());

        // When
        ResponseEntity<Void> response = notificationController.markAsRead(userDetails, updateNotificationDTO);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(notificationService, times(1)).batchUpdate(userDetails, updateNotificationDTO);
    }

    @Test
    @DisplayName("SSE 구독 테스트")
    void testSubscribe() {
        // Given
        SseEmitter mockEmitter = new SseEmitter();
        when(notificationService.subscribe(anyLong(), anyLong())).thenReturn(mockEmitter);

        // When
        SseEmitter result = notificationController.subscribe(userDetails);

        // Then
        assertNotNull(result);
        verify(notificationService, times(1)).subscribe(userDetails.getUserId(), userDetails.getTokenId());
    }
}
