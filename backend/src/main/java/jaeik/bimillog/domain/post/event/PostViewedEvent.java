package jaeik.bimillog.domain.post.event;

/**
 * <h2>게시글 조회 이벤트</h2>
 * <p>게시글이 조회되었을 때 발생하는 비동기 이벤트</p>
 * <p>PostQueryController에서 게시글 상세 조회 요청 시 중복 조회 검증 후 발생합니다.</p>
 * <p>PostViewEventListener에서 수신하여 비동기로 게시글의 조회수를 1 증가시킵니다.</p>
 * <p>메인 스레드의 응답 속도 향상을 위해 조회수 업데이트를 비동기로 처리합니다.</p>
 * <p>중복 조회 방지 로직은 Controller 레이어에서 처리되므로, 이벤트는 순수하게 조회수 증가만 담당합니다.</p>
 *
 * @param postId 조회된 게시글 ID
 * @author Jaeik
 * @version 2.0.0
 */
public record PostViewedEvent(Long postId) {
    public PostViewedEvent {
        if (postId == null) {
            throw new IllegalArgumentException("게시글 ID는 null일 수 없습니다.");
        }
    }
}