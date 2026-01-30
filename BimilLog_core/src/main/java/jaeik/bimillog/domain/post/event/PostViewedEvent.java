package jaeik.bimillog.domain.post.event;

/**
 * <h2>게시글 조회 이벤트</h2>
 * <p>게시글이 조회되었을 때 발생하는 비동기 이벤트</p>
 * <p>PostQueryService에서 게시글 상세 조회 시 발행됩니다.</p>
 * <p>조회수 증가 및 실시간 인기글 점수 증가에 사용됩니다.</p>
 *
 * @param postId    조회된 게시글 ID
 * @param viewerKey 조회자 식별 키 (중복 조회 방지용, "m:{memberId}" 또는 "ip:{ip}")
 * @author Jaeik
 * @version 1.0.0
 */
public record PostViewedEvent(Long postId, String viewerKey) {
    public PostViewedEvent {
        if (postId == null) {
            throw new IllegalArgumentException("게시글 ID는 null일 수 없습니다.");
        }
        if (viewerKey == null || viewerKey.isBlank()) {
            throw new IllegalArgumentException("조회자 키는 null이거나 비어있을 수 없습니다.");
        }
    }
}
