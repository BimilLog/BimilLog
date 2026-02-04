package jaeik.bimillog.domain.post.event;

import java.util.UUID;

/**
 * <h2>게시글 수정 이벤트</h2>
 * <p>게시글이 수정되었을 때 발생하는 비동기 이벤트입니다.</p>
 * <p>PostCommandService에서 게시글 수정 시 발생합니다.</p>
 * <p>PostReadModel의 제목을 업데이트하는데 사용됩니다.</p>
 *
 * @param postId   수정된 게시글 ID
 * @param newTitle 새로운 제목
 * @param eventId  이벤트 고유 ID (멱등성 보장용)
 * @author Jaeik
 * @version 2.6.0
 */
public record PostUpdatedEvent(
        Long postId,
        String newTitle,
        String eventId
) {
    public PostUpdatedEvent(Long postId, String newTitle) {
        this(postId, newTitle,
                "POST_UPDATED:" + postId + ":" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
    }

    public PostUpdatedEvent {
        if (postId == null) {
            throw new IllegalArgumentException("게시글 ID는 null일 수 없습니다.");
        }
        if (newTitle == null || newTitle.isBlank()) {
            throw new IllegalArgumentException("게시글 제목은 null이거나 빈 문자열일 수 없습니다.");
        }
    }

    /**
     * 멱등성 보장을 위한 키를 반환합니다.
     *
     * @return 멱등성 키
     */
    public String getIdempotencyKey() {
        return eventId;
    }
}
