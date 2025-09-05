package jaeik.bimillog.integration.event.post;

import jaeik.bimillog.domain.notification.application.port.in.NotificationFcmUseCase;
import jaeik.bimillog.domain.notification.application.port.in.NotificationSseUseCase;
import jaeik.bimillog.domain.post.event.PostFeaturedEvent;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

/**
 * <h2>게시글 인기글 등극 이벤트 워크플로우 통합 테스트</h2>
 * <p>게시글이 인기글로 선정될 때 발생하는 모든 후속 처리를 검증하는 통합 테스트</p>
 * <p>비동기 이벤트 처리와 실제 스프링 컨텍스트를 사용하여 전체 워크플로우를 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest
@Testcontainers
@Transactional
@DisplayName("게시글 인기글 등극 이벤트 워크플로우 통합 테스트")
public class PostFeaturedEventIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @MockitoBean
    private NotificationSseUseCase notificationSseUseCase;

    @MockitoBean
    private NotificationFcmUseCase notificationFcmUseCase;

    @Test
    @DisplayName("인기글 등극 이벤트 워크플로우 - SSE와 FCM 알림까지 완료")
    void postFeaturedEventWorkflow_ShouldCompleteNotifications() {
        // Given
        Long userId = 1L;
        String sseMessage = "축하합니다! 회원님의 게시글이 주간 인기글에 선정되었습니다!";
        Long postId = 100L;
        String fcmTitle = "🎉 인기글 선정!";
        String fcmBody = "축하합니다! 회원님의 게시글이 인기글에 선정되었어요!";
        
        PostFeaturedEvent event = new PostFeaturedEvent(userId, sseMessage, postId, fcmTitle, fcmBody);

        // When
        eventPublisher.publishEvent(event);

        // Then - 비동기 처리를 고려하여 Awaitility 사용
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(notificationSseUseCase).sendPostFeaturedNotification(
                            eq(userId), eq(sseMessage), eq(postId));
                    verify(notificationFcmUseCase).sendPostFeaturedNotification(
                            eq(userId), eq(fcmTitle), eq(fcmBody));
                });
    }

    @Test
    @DisplayName("여러 게시글 인기글 등극 이벤트 동시 처리")
    void multiplePostFeaturedEvents_ShouldProcessConcurrently() {
        // Given
        PostFeaturedEvent event1 = new PostFeaturedEvent(
                1L, "게시글 1이 인기글에 선정되었습니다!", 101L, "인기글 선정", "축하합니다!");
        PostFeaturedEvent event2 = new PostFeaturedEvent(
                2L, "게시글 2가 명예의 전당에 등록되었습니다!", 102L, "명예의 전당", "대단합니다!");
        PostFeaturedEvent event3 = new PostFeaturedEvent(
                3L, "게시글 3이 주간 베스트에 선정되었습니다!", 103L, "주간 베스트", "훌륭합니다!");

        // When - 동시에 여러 인기글 등극 이벤트 발행
        eventPublisher.publishEvent(event1);
        eventPublisher.publishEvent(event2);
        eventPublisher.publishEvent(event3);

        // Then - 모든 이벤트가 독립적으로 알림 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(notificationSseUseCase).sendPostFeaturedNotification(
                            eq(1L), eq("게시글 1이 인기글에 선정되었습니다!"), eq(101L));
                    verify(notificationSseUseCase).sendPostFeaturedNotification(
                            eq(2L), eq("게시글 2가 명예의 전당에 등록되었습니다!"), eq(102L));
                    verify(notificationSseUseCase).sendPostFeaturedNotification(
                            eq(3L), eq("게시글 3이 주간 베스트에 선정되었습니다!"), eq(103L));
                    
                    verify(notificationFcmUseCase).sendPostFeaturedNotification(
                            eq(1L), eq("인기글 선정"), eq("축하합니다!"));
                    verify(notificationFcmUseCase).sendPostFeaturedNotification(
                            eq(2L), eq("명예의 전당"), eq("대단합니다!"));
                    verify(notificationFcmUseCase).sendPostFeaturedNotification(
                            eq(3L), eq("주간 베스트"), eq("훌륭합니다!"));
                });
    }

    @Test
    @DisplayName("동일 사용자의 여러 게시글 인기글 등극")
    void multiplePostFeaturedEventsForSameUser_ShouldProcessAll() {
        // Given - 동일 사용자의 여러 게시글이 인기글로 선정
        Long userId = 1L;
        PostFeaturedEvent event1 = new PostFeaturedEvent(
                userId, "첫 번째 게시글이 인기글에 선정!", 101L, "인기글 1", "축하해요!");
        PostFeaturedEvent event2 = new PostFeaturedEvent(
                userId, "두 번째 게시글도 인기글에 선정!", 102L, "인기글 2", "대단해요!");
        PostFeaturedEvent event3 = new PostFeaturedEvent(
                userId, "세 번째 게시글까지 인기글 선정!", 103L, "인기글 3", "놀라워요!");

        // When
        eventPublisher.publishEvent(event1);
        eventPublisher.publishEvent(event2);
        eventPublisher.publishEvent(event3);

        // Then - 모든 게시글에 대해 개별 알림이 발송되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(notificationSseUseCase).sendPostFeaturedNotification(
                            eq(userId), eq("첫 번째 게시글이 인기글에 선정!"), eq(101L));
                    verify(notificationSseUseCase).sendPostFeaturedNotification(
                            eq(userId), eq("두 번째 게시글도 인기글에 선정!"), eq(102L));
                    verify(notificationSseUseCase).sendPostFeaturedNotification(
                            eq(userId), eq("세 번째 게시글까지 인기글 선정!"), eq(103L));
                    
                    verify(notificationFcmUseCase).sendPostFeaturedNotification(
                            eq(userId), eq("인기글 1"), eq("축하해요!"));
                    verify(notificationFcmUseCase).sendPostFeaturedNotification(
                            eq(userId), eq("인기글 2"), eq("대단해요!"));
                    verify(notificationFcmUseCase).sendPostFeaturedNotification(
                            eq(userId), eq("인기글 3"), eq("놀라워요!"));
                });
    }

    @Test
    @DisplayName("인기글 이벤트 처리 성능 검증")
    void postFeaturedEventProcessingTime_ShouldCompleteWithinTimeout() {
        // Given
        PostFeaturedEvent event = new PostFeaturedEvent(
                1L, "성능 테스트 게시글이 인기글에 선정!", 999L, "성능 테스트", "빠른 처리!");

        long startTime = System.currentTimeMillis();

        // When
        eventPublisher.publishEvent(event);

        // Then - 3초 내에 처리 완료되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> {
                    verify(notificationSseUseCase).sendPostFeaturedNotification(
                            eq(1L), eq("성능 테스트 게시글이 인기글에 선정!"), eq(999L));
                    verify(notificationFcmUseCase).sendPostFeaturedNotification(
                            eq(1L), eq("성능 테스트"), eq("빠른 처리!"));

                    long endTime = System.currentTimeMillis();
                    long processingTime = endTime - startTime;
                    
                    // 처리 시간이 3초를 초과하지 않아야 함
                    assert processingTime < 3000L : "인기글 이벤트 처리 시간이 너무 오래 걸림: " + processingTime + "ms";
                });
    }

    @Test
    @DisplayName("대량 인기글 등극 이벤트 처리 성능")
    void massPostFeaturedEvents_ShouldProcessEfficiently() {
        // Given - 대량의 인기글 등극 이벤트 (50개)
        int eventCount = 50;
        
        long startTime = System.currentTimeMillis();

        // When - 대량 이벤트 발행
        for (int i = 1; i <= eventCount; i++) {
            PostFeaturedEvent event = new PostFeaturedEvent(
                    (long) i, 
                    "게시글 " + i + "이 인기글에 선정!",
                    (long) (i + 1000),
                    "인기글 " + i,
                    "축하 " + i);
            eventPublisher.publishEvent(event);
        }

        // Then - 모든 이벤트가 15초 내에 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> {
                    for (int i = 1; i <= eventCount; i++) {
                        verify(notificationSseUseCase).sendPostFeaturedNotification(
                                eq((long) i), eq("게시글 " + i + "이 인기글에 선정!"), eq((long) (i + 1000)));
                        verify(notificationFcmUseCase).sendPostFeaturedNotification(
                                eq((long) i), eq("인기글 " + i), eq("축하 " + i));
                    }

                    long endTime = System.currentTimeMillis();
                    long totalProcessingTime = endTime - startTime;
                    
                    // 대량 처리 시간이 15초를 초과하지 않아야 함
                    assert totalProcessingTime < 15000L : "대량 인기글 이벤트 처리 시간이 너무 오래 걸림: " + totalProcessingTime + "ms";
                });
    }

    @Test
    @DisplayName("이벤트 생성 시 유효성 검증 - null userId")
    void postFeaturedEventCreation_ShouldValidateNullUserId() {
        // When & Then - null userId로 이벤트 생성 시 예외 발생
        assertThatThrownBy(() -> new PostFeaturedEvent(
                null, "메시지", 1L, "제목", "내용"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자 ID는 null일 수 없습니다.");
    }

    @Test
    @DisplayName("이벤트 생성 시 유효성 검증 - null postId")
    void postFeaturedEventCreation_ShouldValidateNullPostId() {
        // When & Then - null postId로 이벤트 생성 시 예외 발생
        assertThatThrownBy(() -> new PostFeaturedEvent(
                1L, "메시지", null, "제목", "내용"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("게시글 ID는 null일 수 없습니다.");
    }

    @Test
    @DisplayName("이벤트 생성 시 유효성 검증 - 빈 SSE 메시지")
    void postFeaturedEventCreation_ShouldValidateEmptySseMessage() {
        // When & Then - 빈 SSE 메시지로 이벤트 생성 시 예외 발생
        assertThatThrownBy(() -> new PostFeaturedEvent(
                1L, "", 1L, "제목", "내용"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("SSE 메시지는 null이거나 비어있을 수 없습니다.");
    }

    @Test
    @DisplayName("이벤트 생성 시 유효성 검증 - 빈 FCM 제목")
    void postFeaturedEventCreation_ShouldValidateEmptyFcmTitle() {
        // When & Then - 빈 FCM 제목으로 이벤트 생성 시 예외 발생
        assertThatThrownBy(() -> new PostFeaturedEvent(
                1L, "메시지", 1L, "   ", "내용"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("FCM 제목은 null이거나 비어있을 수 없습니다.");
    }

    @Test
    @DisplayName("이벤트 생성 시 유효성 검증 - 빈 FCM 내용")
    void postFeaturedEventCreation_ShouldValidateEmptyFcmBody() {
        // When & Then - 빈 FCM 내용으로 이벤트 생성 시 예외 발생
        assertThatThrownBy(() -> new PostFeaturedEvent(
                1L, "메시지", 1L, "제목", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("FCM 내용은 null이거나 비어있을 수 없습니다.");
    }

    @Test
    @DisplayName("다양한 메시지 형태의 인기글 이벤트 처리")
    void postFeaturedEventWithVariousMessages_ShouldProcessCorrectly() {
        // Given - 다양한 형태의 메시지들
        PostFeaturedEvent event1 = new PostFeaturedEvent(
                1L, "🎉 축하합니다! 게시글이 주간 인기글 1위에 선정되었습니다!", 101L, 
                "🏆 1위 달성!", "주간 인기글 1위 축하드려요!");
        PostFeaturedEvent event2 = new PostFeaturedEvent(
                2L, "명예의 전당 등록 완료! 게시글이 영구 보관됩니다.", 102L,
                "명예의 전당", "영구 보관되는 명예를 얻으셨네요!");
        PostFeaturedEvent event3 = new PostFeaturedEvent(
                3L, "이달의 베스트 게시글로 선정! 특별 뱃지가 지급됩니다.", 103L,
                "이달의 베스트", "특별 뱃지를 받으셨어요!");

        // When
        eventPublisher.publishEvent(event1);
        eventPublisher.publishEvent(event2);
        eventPublisher.publishEvent(event3);

        // Then - 모든 다양한 메시지가 정확히 전달되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(notificationSseUseCase).sendPostFeaturedNotification(
                            eq(1L), eq("🎉 축하합니다! 게시글이 주간 인기글 1위에 선정되었습니다!"), eq(101L));
                    verify(notificationSseUseCase).sendPostFeaturedNotification(
                            eq(2L), eq("명예의 전당 등록 완료! 게시글이 영구 보관됩니다."), eq(102L));
                    verify(notificationSseUseCase).sendPostFeaturedNotification(
                            eq(3L), eq("이달의 베스트 게시글로 선정! 특별 뱃지가 지급됩니다."), eq(103L));
                    
                    verify(notificationFcmUseCase).sendPostFeaturedNotification(
                            eq(1L), eq("🏆 1위 달성!"), eq("주간 인기글 1위 축하드려요!"));
                    verify(notificationFcmUseCase).sendPostFeaturedNotification(
                            eq(2L), eq("명예의 전당"), eq("영구 보관되는 명예를 얻으셨네요!"));
                    verify(notificationFcmUseCase).sendPostFeaturedNotification(
                            eq(3L), eq("이달의 베스트"), eq("특별 뱃지를 받으셨어요!"));
                });
    }

    @Test
    @DisplayName("연속된 인기글 이벤트 처리 순서")
    void sequentialPostFeaturedEvents_ShouldMaintainOrder() {
        // Given - 동일 사용자의 연속된 인기글 등극
        Long userId = 1L;
        
        // When - 순서대로 인기글 이벤트 발행
        eventPublisher.publishEvent(new PostFeaturedEvent(
                userId, "첫 번째 인기글!", 101L, "1등", "첫 번째"));
        eventPublisher.publishEvent(new PostFeaturedEvent(
                userId, "두 번째 인기글!", 102L, "2등", "두 번째"));
        eventPublisher.publishEvent(new PostFeaturedEvent(
                userId, "세 번째 인기글!", 103L, "3등", "세 번째"));

        // Then - 비동기 처리이지만 모든 이벤트가 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(notificationSseUseCase).sendPostFeaturedNotification(
                            eq(userId), eq("첫 번째 인기글!"), eq(101L));
                    verify(notificationSseUseCase).sendPostFeaturedNotification(
                            eq(userId), eq("두 번째 인기글!"), eq(102L));
                    verify(notificationSseUseCase).sendPostFeaturedNotification(
                            eq(userId), eq("세 번째 인기글!"), eq(103L));
                    
                    // FCM 알림도 3번 호출
                    verify(notificationFcmUseCase, times(3)).sendPostFeaturedNotification(
                            eq(userId), 
                            org.mockito.ArgumentMatchers.anyString(), 
                            org.mockito.ArgumentMatchers.anyString());
                });
    }

    @Test
    @DisplayName("긴 메시지 내용의 인기글 이벤트 처리")
    void postFeaturedEventWithLongMessages_ShouldProcessCorrectly() {
        // Given - 매우 긴 메시지들
        String longSseMessage = "축하합니다! ".repeat(50) + "회원님의 게시글이 인기글에 선정되었습니다!";
        String longFcmTitle = "🎉 ".repeat(20) + "인기글 선정!";
        String longFcmBody = "정말 대단하신 글이었어요! ".repeat(30) + "축하드립니다!";
        
        PostFeaturedEvent event = new PostFeaturedEvent(1L, longSseMessage, 100L, longFcmTitle, longFcmBody);

        // When
        eventPublisher.publishEvent(event);

        // Then - 긴 메시지도 정상적으로 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(notificationSseUseCase).sendPostFeaturedNotification(
                            eq(1L), eq(longSseMessage), eq(100L));
                    verify(notificationFcmUseCase).sendPostFeaturedNotification(
                            eq(1L), eq(longFcmTitle), eq(longFcmBody));
                });
    }
}
