package jaeik.bimillog.domain.post.event;

import jaeik.bimillog.domain.global.event.CacheCountEvent;
import jaeik.bimillog.domain.global.event.RealtimeScoreEvent;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;

/**
 * <h2>게시글 도메인 이벤트</h2>
 * <p>게시글 도메인에서 발생하는 모든 이벤트를 정의합니다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
public interface PostEvent {

    /**
     * <h3>글 작성 캐시 이벤트</h3>
     * <p>게시글 작성 후 첫 페이지 캐시에 반영하기 위한 이벤트</p>
     *
     * @param postDetail 새로 작성된 게시글 요약 정보
     */
    record PostWrittenEvent(PostSimpleDetail postDetail) implements PostEvent {}

    /**
     * <h3>글 삭제 캐시 이벤트</h3>
     * <p>게시글 삭제 후 모든 캐시(ZSet + JSON LIST)를 정리하기 위한 이벤트</p>
     *
     * @param postId 삭제된 게시글 ID
     */
    record PostRemovedEvent(Long postId) implements PostEvent {}

    /**
     * <h3>글 수정 캐시 이벤트</h3>
     * <p>게시글 수정 후 모든 JSON LIST 캐시의 제목을 갱신하기 위한 이벤트</p>
     *
     * @param postId      수정된 게시글 ID
     * @param updatedPost 수정된 게시글 요약 정보
     */
    record PostModifiedEvent(Long postId, PostSimpleDetail updatedPost) implements PostEvent {}

    /**
     * <h3>인기글 등극 이벤트</h3>
     * <p>게시글이 주간 인기글, 전설의 게시글, 실시간 인기글로 등극했을 때 발생</p>
     * <p>NotificationSaveListener에서 수신하여 알림을 저장하고 SSE와 FCM 알림을 비동기로 발송합니다.</p>
     * <p>FCM 푸시 알림의 title과 body는 FcmCommandService에서 NotificationType에 따라 작성됩니다.</p>
     *
     * @param memberId         게시글 작성자 ID (알림을 받을 대상 사용자)
     * @param sseMessage       SSE 알림 메시지 (브라우저 실시간 알림용)
     * @param postId           등극한 게시글 ID
     * @param notificationType 인기글 유형 (WEEKLY/LEGEND/REALTIME)
     * @param postTitle        게시글 제목 (FCM 알림 본문에 사용)
     */
    record PostFeaturedEvent(Long memberId, String sseMessage, Long postId,
                             NotificationType notificationType, String postTitle) implements PostEvent {}

    /**
     * <h3>게시글 상세 조회 캐시 이벤트</h3>
     * <p>게시글 상세 조회 시 조회수 증가 및 실시간 인기글 점수를 반영하기 위한 이벤트</p>
     *
     * @param postId    조회된 게시글 ID
     * @param viewerKey 조회자 식별 키 (중복 조회 방지용)
     */
    record PostDetailViewedEvent(Long postId, String viewerKey) implements PostEvent, RealtimeScoreEvent {
        @Override
        public double realtimeScore() { return 2.0; }
    }

    /**
     * <h3>게시글 추천취소 캐시 이벤트</h3>
     * <p>게시글 추천취소 후 좋아요 카운터 -1 및 실시간 인기글 점수 -4.0을 반영하기 위한 이벤트</p>
     *
     * @param postId 추천취소된 게시글 ID
     */
    record PostUnlikedEvent(Long postId) implements PostEvent, RealtimeScoreEvent, CacheCountEvent {
        @Override
        public double realtimeScore() { return -4.0; }

        @Override
        public String counterField() { return "likeCount"; }

        @Override
        public int counterDelta() { return -1; }
    }
}
