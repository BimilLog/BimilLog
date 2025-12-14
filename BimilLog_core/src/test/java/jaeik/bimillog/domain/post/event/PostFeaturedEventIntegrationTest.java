package jaeik.bimillog.domain.post.event;

import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.notification.service.FcmCommandService;
import jaeik.bimillog.domain.notification.service.SseService;
import jaeik.bimillog.testutil.BaseEventIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.*;
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
@DisplayName("게시글 인기글 등극 이벤트 워크플로우 통합 테스트")
@Tag("integration")
public class PostFeaturedEventIntegrationTest extends BaseEventIntegrationTest {

    @MockitoBean
    private SseService sseService;

    @MockitoBean
    private FcmCommandService fcmCommandService;

    @Test
    @DisplayName("인기글 등극 이벤트 워크플로우 - SSE와 FCM 알림까지 완료")
    void postFeaturedEventWorkflow_ShouldCompleteNotifications() {
        // Given
        Long memberId = 1L;
        String sseMessage = "주간 인기 게시글로 선정되었어요!";
        Long postId = 100L;
        String postTitle = "테스트 게시글 제목";

        PostFeaturedEvent event = new PostFeaturedEvent(
                memberId, sseMessage, postId, NotificationType.POST_FEATURED_WEEKLY, postTitle);

        // When & Then
        publishAndVerify(event, () -> {
            verify(sseService).sendNotification(
                    eq(memberId),
                    eq(NotificationType.POST_FEATURED_WEEKLY),
                    eq(sseMessage),
                    anyString());
            verify(fcmCommandService).sendNotification(
                    eq(NotificationType.POST_FEATURED_WEEKLY),
                    eq(memberId),
                    isNull(),
                    eq(postTitle));
            verifyNoMoreInteractions(sseService, fcmCommandService);
        });
    }

    @Test
    @DisplayName("여러 다른 사용자의 인기글 이벤트 동시 처리")
    void multipleDifferentUserPostFeaturedEvents_ShouldProcessIndependently() {
        // Given
        PostFeaturedEvent event1 = new PostFeaturedEvent(
                1L, "주간 인기 게시글로 선정되었어요!", 101L,
                NotificationType.POST_FEATURED_WEEKLY, "게시글 1 제목");
        PostFeaturedEvent event2 = new PostFeaturedEvent(
                2L, "명예의 전당에 등극했어요!", 102L,
                NotificationType.POST_FEATURED_LEGEND, "게시글 2 제목");
        PostFeaturedEvent event3 = new PostFeaturedEvent(
                3L, "실시간 인기 게시글로 선정되었어요!", 103L,
                NotificationType.POST_FEATURED_REALTIME, "게시글 3 제목");

        // When & Then - 모든 이벤트가 독립적으로 처리되어야 함
        publishEventsAndVerify(new Object[]{event1, event2, event3}, () -> {
            verify(sseService).sendNotification(
                    eq(1L),
                    eq(NotificationType.POST_FEATURED_WEEKLY),
                    eq("주간 인기 게시글로 선정되었어요!"),
                    anyString());
            verify(sseService).sendNotification(
                    eq(2L),
                    eq(NotificationType.POST_FEATURED_LEGEND),
                    eq("명예의 전당에 등극했어요!"),
                    anyString());
            verify(sseService).sendNotification(
                    eq(3L),
                    eq(NotificationType.POST_FEATURED_REALTIME),
                    eq("실시간 인기 게시글로 선정되었어요!"),
                    anyString());

            verify(fcmCommandService).sendNotification(
                    eq(NotificationType.POST_FEATURED_WEEKLY),
                    eq(1L), isNull(), eq("게시글 1 제목"));
            verify(fcmCommandService).sendNotification(
                    eq(NotificationType.POST_FEATURED_LEGEND),
                    eq(2L), isNull(), eq("게시글 2 제목"));
            verify(fcmCommandService).sendNotification(
                    eq(NotificationType.POST_FEATURED_REALTIME),
                    eq(3L), isNull(), eq("게시글 3 제목"));
            verifyNoMoreInteractions(sseService, fcmCommandService);
        });
    }

    @Test
    @DisplayName("동일 사용자의 여러 게시글 인기글 등극")
    void multiplePostFeaturedEventsForSameUser_ShouldProcessAll() {
        // Given - 동일 사용자의 여러 게시글이 인기글로 선정
        Long memberId = 1L;
        PostFeaturedEvent event1 = new PostFeaturedEvent(
                memberId, "주간 인기 게시글로 선정되었어요!", 101L,
                NotificationType.POST_FEATURED_WEEKLY, "첫 번째 게시글");
        PostFeaturedEvent event2 = new PostFeaturedEvent(
                memberId, "명예의 전당에 등극했어요!", 102L,
                NotificationType.POST_FEATURED_LEGEND, "두 번째 게시글");
        PostFeaturedEvent event3 = new PostFeaturedEvent(
                memberId, "실시간 인기 게시글로 선정되었어요!", 103L,
                NotificationType.POST_FEATURED_REALTIME, "세 번째 게시글");

        // When & Then - 동일 사용자라도 각 게시글에 대해 개별 알림이 발송되어야 함
        publishEventsAndVerify(new Object[]{event1, event2, event3}, () -> {
            verify(sseService).sendNotification(
                    eq(memberId),
                    eq(NotificationType.POST_FEATURED_WEEKLY),
                    eq("주간 인기 게시글로 선정되었어요!"),
                    anyString());
            verify(sseService).sendNotification(
                    eq(memberId),
                    eq(NotificationType.POST_FEATURED_LEGEND),
                    eq("명예의 전당에 등극했어요!"),
                    anyString());
            verify(sseService).sendNotification(
                    eq(memberId),
                    eq(NotificationType.POST_FEATURED_REALTIME),
                    eq("실시간 인기 게시글로 선정되었어요!"),
                    anyString());

            verify(fcmCommandService).sendNotification(
                    eq(NotificationType.POST_FEATURED_WEEKLY),
                    eq(memberId), isNull(), eq("첫 번째 게시글"));
            verify(fcmCommandService).sendNotification(
                    eq(NotificationType.POST_FEATURED_LEGEND),
                    eq(memberId), isNull(), eq("두 번째 게시글"));
            verify(fcmCommandService).sendNotification(
                    eq(NotificationType.POST_FEATURED_REALTIME),
                    eq(memberId), isNull(), eq("세 번째 게시글"));
            verifyNoMoreInteractions(sseService, fcmCommandService);
        });
    }

}
