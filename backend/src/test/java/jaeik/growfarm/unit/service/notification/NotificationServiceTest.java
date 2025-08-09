package jaeik.growfarm.unit.service.notification;

import jaeik.growfarm.dto.notification.EventDTO;
import jaeik.growfarm.dto.notification.NotificationDTO;
import jaeik.growfarm.dto.notification.UpdateNotificationDTO;
import jaeik.growfarm.entity.notification.NotificationType;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.repository.notification.EmitterRepository;
import jaeik.growfarm.repository.notification.NotificationRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jaeik.growfarm.service.notification.NotificationService;
import jaeik.growfarm.service.notification.NotificationUpdateService;
import jaeik.growfarm.util.NotificationUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <h2>NotificationService 단위 테스트</h2>
 * <p>
 * NotificationService의 비즈니스 로직을 테스트합니다.
 * </p>
 * @version 1.0.0
 * @author Jaeik
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmitterRepository emitterRepository;

    @Mock
    private NotificationUtil notificationUtil;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationUpdateService notificationUpdateService;

    @InjectMocks
    private NotificationService notificationService;

    private CustomUserDetails userDetails;
    private Users user;
    private SseEmitter sseEmitter;
    private UpdateNotificationDTO updateNotificationDTO;
    private EventDTO eventDTO;

    @BeforeEach
    void setUp() {
        // Setup mock data
        userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUserId()).thenReturn(1L);

        user = mock(Users.class);
        when(user.getId()).thenReturn(1L);

        sseEmitter = mock(SseEmitter.class);

        // Create NotificationDTO list
        NotificationDTO notificationDTO = mock(NotificationDTO.class);
        when(notificationDTO.getId()).thenReturn(1L);
        when(notificationDTO.getData()).thenReturn("Test notification");
        when(notificationDTO.getType()).thenReturn(NotificationType.COMMENT);
        when(notificationDTO.isRead()).thenReturn(false);

        List<NotificationDTO> notificationDTOList = List.of(notificationDTO);

        // Create UpdateNotificationDTO
        updateNotificationDTO = new UpdateNotificationDTO();
        updateNotificationDTO.setReadIds(List.of(1L));
        updateNotificationDTO.setDeletedIds(List.of(2L));

        // Create EventDTO
        eventDTO = new EventDTO();
        eventDTO.setType(NotificationType.COMMENT);
        eventDTO.setData("Test notification");
        eventDTO.setUrl("/test/url");

        // Setup mock repositories
        when(userRepository.getReferenceById(anyLong())).thenReturn(user);
        when(notificationRepository.findNotificationsByUserIdOrderByLatest(anyLong())).thenReturn(notificationDTOList);
    }

    @Test
    @DisplayName("SSE 구독 테스트")
    void testSubscribe() {
        // Given
        String emitterId = "user_1_token_1_timestamp";
        when(notificationUtil.makeTimeIncludeId(anyLong(), anyLong())).thenReturn(emitterId);
        when(emitterRepository.save(anyString(), any(SseEmitter.class))).thenReturn(sseEmitter);
        doNothing().when(sseEmitter).onCompletion(any(Runnable.class));
        doNothing().when(sseEmitter).onTimeout(any(Runnable.class));

        // When
        SseEmitter result = notificationService.subscribe(1L, 1L);

        // Then
        assertNotNull(result);
        verify(emitterRepository, times(1)).save(eq(emitterId), any(SseEmitter.class));
        verify(sseEmitter, times(1)).onCompletion(any(Runnable.class));
        verify(sseEmitter, times(1)).onTimeout(any(Runnable.class));
    }

    @Test
    @DisplayName("알림 발송 테스트")
    void testSend() {
        // Given
        Map<String, SseEmitter> emitters = new HashMap<>();
        emitters.put("emitter1", sseEmitter);
        when(emitterRepository.findAllEmitterByUserId(anyLong())).thenReturn(emitters);
        doNothing().when(notificationUpdateService).saveNotification(any(), any(), anyString(), anyString());

        // When
        notificationService.send(1L, eventDTO);

        // Then
        verify(notificationUpdateService, times(1)).saveNotification(
                eq(user),
                eq(NotificationType.COMMENT),
                eq("Test notification"),
                eq("/test/url"));
        verify(emitterRepository, times(1)).findAllEmitterByUserId(eq(1L));
    }

    @Test
    @DisplayName("알림 리스트 조회 테스트")
    void testGetNotificationList() {
        // When
        List<NotificationDTO> result = notificationService.getNotificationList(userDetails);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test notification", result.getFirst().getData());
        verify(notificationRepository, times(1)).findNotificationsByUserIdOrderByLatest(eq(1L));
    }

    @Test
    @DisplayName("알림 읽음/삭제 처리 테스트")
    void testBatchUpdate() {
        // Given
        doNothing().when(notificationUpdateService).deleteNotifications(anyList(), anyLong());
        doNothing().when(notificationUpdateService).markNotificationsAsRead(anyList(), anyLong());

        // When
        notificationService.batchUpdate(userDetails, updateNotificationDTO);

        // Then
        verify(notificationUpdateService, times(1)).deleteNotifications(eq(List.of(2L)), eq(1L));
        verify(notificationUpdateService, times(1)).markNotificationsAsRead(eq(List.of(1L)), eq(1L));
    }

    // FCM 메시지 전송 테스트는 Firebase 설정이 필요하므로 제외
}
