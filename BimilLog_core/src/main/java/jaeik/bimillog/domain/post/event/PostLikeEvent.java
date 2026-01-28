package jaeik.bimillog.domain.post.event;

import java.util.UUID;

/**
 * <h2>게시글 추천 이벤트</h2>
 * <p>게시글이 추천(좋아요)되었을 때 발생하는 비동기 이벤트</p>
 * <p>PostInteractionService에서 게시글 추천 토글 시 발생합니다.</p>
 * <p>실시간 인기글 점수 증가 및 상호작용 점수 증가에 사용됩니다.</p>
 * <p>추천 취소 시에는 이벤트를 발행하지 않습니다.</p>
 *
 * @param postId 추천된 게시글 ID
 * @param postAuthorId 게시글 작성자 ID (익명인 경우 null)
 * @param likerId 추천한 사용자 ID
 * @param eventId 이벤트 고유 ID (멱등성 보장용, 재시도 시 동일한 ID 유지)
 * @author Jaeik
 * @version 2.0.0
 */
public record PostLikeEvent(
        Long postId,
        Long postAuthorId,
        Long likerId,
        String eventId
) {
    public PostLikeEvent(Long postId, Long postAuthorId, Long likerId) {
        this(postId, postAuthorId, likerId, UUID.randomUUID().toString().replace("-", "").substring(0, 16));
    }

    public PostLikeEvent {
        if (postId == null) {
            throw new IllegalArgumentException("게시글 ID는 null일 수 없습니다.");
        }
        if (likerId == null) {
            throw new IllegalArgumentException("추천한 사용자 ID는 null일 수 없습니다.");
        }
        // postAuthorId는 익명 게시글의 경우 null일 수 있음
    }

    /**
     * 상호작용 점수 증가의 멱등성 보장을 위한 키를 반환합니다.
     * 이벤트 발행 시 생성된 UUID를 사용하여 재시도 시에도 동일한 키가 유지됩니다.
     *
     * @return 멱등성 키 (UUID)
     */
    public String getIdempotencyKey() {
        return eventId;
    }
}
