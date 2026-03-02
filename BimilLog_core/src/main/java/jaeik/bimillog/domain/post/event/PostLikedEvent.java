package jaeik.bimillog.domain.post.event;

import jaeik.bimillog.domain.global.event.CacheCountEvent;
import jaeik.bimillog.domain.global.event.FriendInteractionEvent;
import jaeik.bimillog.domain.global.event.RealtimeScoreEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * <h2>게시글 추천 이벤트</h2>
 * <p>게시글이 추천(좋아요)되었을 때 발생하는 이벤트</p>
 * <p>캐시 카운터 증가, 실시간 인기글 점수 증가, 친구 상호작용 점수 증가에 사용됩니다.</p>
 * <p>익명 게시글이나 자기 자신 추천 시 FriendInteractionListener에서 null guard로 처리됩니다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Slf4j
public record PostLikedEvent(String eventId, Long postId, Long postAuthorId, Long likerId) implements FriendInteractionEvent, RealtimeScoreEvent, CacheCountEvent {

    public static PostLikedEvent of(Long postId, Long postAuthorId, Long likerId) {
        return new PostLikedEvent(
                UUID.randomUUID().toString().replace("-", "").substring(0, 16),
                postId, postAuthorId, likerId
        );
    }

    @Override
    public double realtimeScore() { return 4.0; }

    @Override
    public String counterField() { return "likeCount"; }

    @Override
    public int counterDelta() { return 1; }

    @Override
    public Long getMemberId() {
        return likerId;
    }

    @Override
    public Long getTargetMemberId() {
        return postAuthorId;
    }

    @Override
    public String getIdempotencyKey() {
        return eventId;
    }

    @Override
    public void getAlreadyProcess() {
        log.info("이미 처리된 게시글 좋아요 이벤트 : postId={}, idempotencyKey={}", postId, eventId);
    }

    @Override
    public void getDlqMessage(Exception e) {
        log.warn("게시글 추천 상호작용 점수 증가 실패 DLQ 진입: postId={}, authorId={}, likerId={}", postId, postAuthorId, likerId, e);
    }
}
