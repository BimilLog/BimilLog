package jaeik.bimillog.domain.post.event;

import jaeik.bimillog.domain.global.event.FriendInteractionEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PostLikedEvent implements FriendInteractionEvent {
    private String eventId;
    private Long postId;
    private Long postAuthorId;
    private Long likerId;

    public PostLikedEvent(Long postId, Long postAuthorId, Long likerId) {
        this.postId = postId;
        this.postAuthorId = postAuthorId;
        this.likerId = likerId;
        this.eventId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * <h3>캐시 전용 생성자</h3>
     * <p>친구 상호작용이 불필요한 경우 (테스트, 캐시 리스너 직접 호출 등)</p>
     */
    public PostLikedEvent(Long postId) {
        this(postId, null, null);
    }

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
