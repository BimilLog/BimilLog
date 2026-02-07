package jaeik.bimillog.domain.post.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.UUID;

/**
 * <h2>게시글 생성 이벤트</h2>
 * <p>게시글이 생성되었을 때 발생하는 비동기 이벤트입니다.</p>
 * <p>PostCommandService에서 게시글 작성 시 발생합니다.</p>
 * <p>PostReadModel에 새 레코드를 INSERT하는데 사용됩니다.</p>

 * @author Jaeik
 * @version 2.6.0
 */
@Getter
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class PostCreatedEvent {
    private Long postId;
    private String title;
    private Long memberId;
    private String memberName;
    private Instant createdAt;
    private String eventId;

    public PostCreatedEvent(Long postId, String title, Long memberId, String memberName, Instant createdAt) {
        this.postId = postId;
        this.title = title;
        this.memberId = memberId;
        this.memberName = memberName;
        this.createdAt = createdAt;
        this.eventId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
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
