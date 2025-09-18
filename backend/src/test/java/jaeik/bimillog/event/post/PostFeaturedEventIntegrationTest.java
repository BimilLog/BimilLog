package jaeik.bimillog.event.post;

import jaeik.bimillog.domain.notification.application.port.in.NotificationFcmUseCase;
import jaeik.bimillog.domain.notification.application.port.in.NotificationSseUseCase;
import jaeik.bimillog.domain.post.event.PostFeaturedEvent;
import jaeik.bimillog.testutil.TestContainersConfiguration;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

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
@Import(TestContainersConfiguration.class)
@Transactional
@DisplayName("게시글 인기글 등극 이벤트 워크플로우 통합 테스트")
public class PostFeaturedEventIntegrationTest {

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
                    verifyNoMoreInteractions(notificationSseUseCase, notificationFcmUseCase);
                });
    }

    @Test
    @DisplayName("여러 다른 사용자의 인기글 이벤트 동시 처리")
    void multipleDifferentUserPostFeaturedEvents_ShouldProcessIndependently() {
        // Given
        PostFeaturedEvent event1 = new PostFeaturedEvent(
                1L, "게시글 1이 인기글에 선정되었습니다!", 101L, "인기글 선정", "축하합니다!");
        PostFeaturedEvent event2 = new PostFeaturedEvent(
                2L, "게시글 2가 명예의 전당에 등록되었습니다!", 102L, "명예의 전당", "대단합니다!");
        PostFeaturedEvent event3 = new PostFeaturedEvent(
                3L, "게시글 3이 주간 베스트에 선정되었습니다!", 103L, "주간 베스트", "훌륭합니다!");

        // When
        eventPublisher.publishEvent(event1);
        eventPublisher.publishEvent(event2);
        eventPublisher.publishEvent(event3);

        // Then - 모든 이벤트가 독립적으로 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
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
                    verifyNoMoreInteractions(notificationSseUseCase, notificationFcmUseCase);
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

        // Then - 동일 사용자라도 각 게시글에 대해 개별 알림이 발송되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
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
                    verifyNoMoreInteractions(notificationSseUseCase, notificationFcmUseCase);
                });
    }

    @Test
    @DisplayName("특수문자가 포함된 메시지 처리")
    void postFeaturedEventWithSpecialCharacters_ShouldProcessCorrectly() {
        // Given - 특수문자가 포함된 메시지
        PostFeaturedEvent event = new PostFeaturedEvent(
                1L, "🎉 축하합니다! <게시글>이 \"인기글\"에 선정되었습니다! & 더 많은 혜택을...", 101L, 
                "🏆 \"인기글\" 선정!", "<축하> & 더 많은 혜택을...");

        // When
        eventPublisher.publishEvent(event);

        // Then - 특수문자가 포함된 메시지도 정확히 전달되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(notificationSseUseCase).sendPostFeaturedNotification(
                            eq(1L), eq("🎉 축하합니다! <게시글>이 \"인기글\"에 선정되었습니다! & 더 많은 혜택을..."), eq(101L));
                    verify(notificationFcmUseCase).sendPostFeaturedNotification(
                            eq(1L), eq("🏆 \"인기글\" 선정!"), eq("<축하> & 더 많은 혜택을..."));
                    verifyNoMoreInteractions(notificationSseUseCase, notificationFcmUseCase);
                });
    }


    @Test
    @DisplayName("비동기 이벤트 리스너 정상 작동 검증")
    void postFeaturedEventAsync_ShouldTriggerListenerCorrectly() {
        // Given
        PostFeaturedEvent event = new PostFeaturedEvent(
                99L, "비동기 테스트 메시지", 9999L, "비동기 제목", "비동기 내용");

        // When
        eventPublisher.publishEvent(event);

        // Then - 비동기 처리가 정상 완료되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> {
                    verify(notificationSseUseCase).sendPostFeaturedNotification(
                            eq(99L), eq("비동기 테스트 메시지"), eq(9999L));
                    verify(notificationFcmUseCase).sendPostFeaturedNotification(
                            eq(99L), eq("비동기 제목"), eq("비동기 내용"));
                    verifyNoMoreInteractions(notificationSseUseCase, notificationFcmUseCase);
                });
    }
}