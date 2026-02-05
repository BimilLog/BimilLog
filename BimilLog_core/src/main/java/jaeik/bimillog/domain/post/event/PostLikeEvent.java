package jaeik.bimillog.domain.post.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * <h2>게시글 추천 이벤트</h2>
 * <p>게시글이 추천(좋아요)되었을 때 발생하는 비동기 이벤트</p>
 * <p>PostInteractionService에서 게시글 추천 토글 시 발생합니다.</p>
 * <p>실시간 인기글 점수 증가 및 상호작용 점수 증가에 사용됩니다.</p>
 * <p>추천 취소 시에는 이벤트를 발행하지 않습니다.</p>

 * @author Jaeik
 * @version 2.7.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PostLikeEvent {
    private Long postId;
    private Long postAuthorId;
    private Long likerId;
    private String eventId;

    public PostLikeEvent(Long postId, Long postAuthorId, Long likerId) {
        this.postId = postId;
        this.postAuthorId = postAuthorId;
        this.likerId = likerId;
        this.eventId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
