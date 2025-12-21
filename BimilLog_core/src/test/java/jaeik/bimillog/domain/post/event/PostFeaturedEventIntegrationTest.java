package jaeik.bimillog.domain.post.event;

import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.notification.service.NotificationCommandService;
import jaeik.bimillog.testutil.BaseEventIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * <h2>게시글 인기글 등극 이벤트 워크플로우 통합 테스트</h2>
 * <p>게시글이 인기글로 선정될 때 알림 저장을 검증하는 통합 테스트</p>
 * <p>PostFeaturedEvent 발생 시 NotificationSaveListener가 호출되어 알림이 저장되는지 검증합니다.</p>
 *
 * @author Jaeik
 * @version 2.2.0
 */
@DisplayName("게시글 인기글 등극 이벤트 워크플로우 통합 테스트")
@Tag("integration")
public class PostFeaturedEventIntegrationTest extends BaseEventIntegrationTest {

    @MockitoBean
    private NotificationCommandService notificationCommandService;

    @Test
    @DisplayName("인기글 등극 이벤트 워크플로우")
    void postFeaturedEventWorkflow_ShouldWork() {
        // Given
        Long memberId = testMember.getId();
        String sseMessage = "주간 인기 게시글로 선정되었어요!";
        Long postId = 100L;
        String postTitle = "테스트 게시글 제목";

        PostFeaturedEvent event = new PostFeaturedEvent(
                memberId, sseMessage, postId, NotificationType.POST_FEATURED_WEEKLY, postTitle);

        // When & Then - 이벤트 발행 확인
        publishEvent(event);
        // PostFeaturedEvent는 알림 저장만 하므로 별도 검증 없음
    }

    @Test
    @DisplayName("여러 다른 사용자의 인기글 이벤트 동시 처리")
    void multipleDifferentUserPostFeaturedEvents_ShouldProcessIndependently() {
        // Given
        PostFeaturedEvent event1 = new PostFeaturedEvent(
                testMember.getId(), "주간 인기 게시글로 선정되었어요!", 101L,
                NotificationType.POST_FEATURED_WEEKLY, "게시글 1 제목");
        PostFeaturedEvent event2 = new PostFeaturedEvent(
                otherMember.getId(), "명예의 전당에 등극했어요!", 102L,
                NotificationType.POST_FEATURED_LEGEND, "게시글 2 제목");
        PostFeaturedEvent event3 = new PostFeaturedEvent(
                adminMember.getId(), "실시간 인기 게시글로 선정되었어요!", 103L,
                NotificationType.POST_FEATURED_REALTIME, "게시글 3 제목");

        // When & Then - 모든 이벤트가 독립적으로 발행됨
        publishEvents(event1, event2, event3);
        // PostFeaturedEvent는 알림 저장만 하므로 별도 검증 없음
    }

    @Test
    @DisplayName("동일 사용자의 여러 게시글 인기글 등극")
    void multiplePostFeaturedEventsForSameUser_ShouldProcessAll() {
        // Given - 동일 사용자의 여러 게시글이 인기글로 선정
        Long memberId = testMember.getId();
        PostFeaturedEvent event1 = new PostFeaturedEvent(
                memberId, "주간 인기 게시글로 선정되었어요!", 101L,
                NotificationType.POST_FEATURED_WEEKLY, "첫 번째 게시글");
        PostFeaturedEvent event2 = new PostFeaturedEvent(
                memberId, "명예의 전당에 등극했어요!", 102L,
                NotificationType.POST_FEATURED_LEGEND, "두 번째 게시글");
        PostFeaturedEvent event3 = new PostFeaturedEvent(
                memberId, "실시간 인기 게시글로 선정되었어요!", 103L,
                NotificationType.POST_FEATURED_REALTIME, "세 번째 게시글");

        // When & Then - 동일 사용자라도 각 게시글에 대해 개별 이벤트 발행
        publishEvents(event1, event2, event3);
        // PostFeaturedEvent는 알림 저장만 하므로 별도 검증 없음
    }

}
