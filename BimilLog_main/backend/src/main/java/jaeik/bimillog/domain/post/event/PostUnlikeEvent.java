package jaeik.bimillog.domain.post.event;

/**
 * <h2>게시글 추천 취소 이벤트</h2>
 * <p>게시글 추천(좋아요)이 취소되었을 때 발생하는 비동기 이벤트</p>
 * <p>PostInteractionService에서 게시글 추천 토글 시(추천 취소) 발생합니다.</p>
 * <p>실시간 인기글 점수 감소에 사용됩니다.</p>
 *
 * @param postId 추천 취소된 게시글 ID
 * @author Jaeik
 * @version 2.0.0
 */
public record PostUnlikeEvent(Long postId) {
    public PostUnlikeEvent {
        if (postId == null) {
            throw new IllegalArgumentException("게시글 ID는 null일 수 없습니다.");
        }
    }
}
