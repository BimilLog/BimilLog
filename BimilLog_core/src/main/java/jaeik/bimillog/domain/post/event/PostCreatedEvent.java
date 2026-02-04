package jaeik.bimillog.domain.post.event;

import java.time.Instant;
import java.util.UUID;

/**
 * <h2>게시글 생성 이벤트</h2>
 * <p>게시글이 생성되었을 때 발생하는 비동기 이벤트입니다.</p>
 * <p>PostCommandService에서 게시글 작성 시 발생합니다.</p>
 * <p>PostReadModel에 새 레코드를 INSERT하는데 사용됩니다.</p>
 *
 * @param postId     생성된 게시글 ID
 * @param title      게시글 제목
 * @param memberId   작성자 ID (익명인 경우 null)
 * @param memberName 작성자 이름 (익명인 경우 "익명")
 * @param createdAt  게시글 생성 시각
 * @param eventId    이벤트 고유 ID (멱등성 보장용)
 * @author Jaeik
 * @version 2.6.0
 */
public record PostCreatedEvent(
        Long postId,
        String title,
        Long memberId,
        String memberName,
        Instant createdAt,
        String eventId
) {
    public PostCreatedEvent(Long postId, String title, Long memberId, String memberName, Instant createdAt) {
        this(postId, title, memberId, memberName, createdAt,
                "POST_CREATED:" + postId + ":" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
    }

    public PostCreatedEvent {
        if (postId == null) {
            throw new IllegalArgumentException("게시글 ID는 null일 수 없습니다.");
        }
        if (title == null || title.isBlank()) {
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
