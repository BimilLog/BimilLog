package jaeik.bimillog.domain.post.entity.jpa;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * <h2>Post Read Model DLQ 엔티티</h2>
 * <p>PostReadModel 이벤트 처리 실패 시 재처리를 위해 저장하는 Dead Letter Queue 엔티티입니다.</p>
 *
 * @author Jaeik
 * @version 2.6.0
 */
@Entity
@Table(name = "post_read_model_dlq",
        indexes = @Index(name = "idx_post_dlq_status_created", columnList = "status, created_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostReadModelDlq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 64, unique = true)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private PostReadModelEventType eventType;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(length = 30)
    private String title;

    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "member_name", length = 50)
    private String memberName;

    @Column(name = "delta_value")
    private Integer deltaValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DlqStatus status;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum DlqStatus {
        PENDING,
        PROCESSED,
        FAILED
    }

    @Builder
    private PostReadModelDlq(String eventId, PostReadModelEventType eventType, Long postId,
                             String title, Long memberId, String memberName, Integer deltaValue) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.postId = postId;
        this.title = title;
        this.memberId = memberId;
        this.memberName = memberName;
        this.deltaValue = deltaValue;
        this.status = DlqStatus.PENDING;
        this.retryCount = 0;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 게시글 생성 이벤트용 DLQ
     */
    public static PostReadModelDlq createPostCreated(String eventId, Long postId, String title,
                                                     Long memberId, String memberName) {
        return PostReadModelDlq.builder()
                .eventId(eventId)
                .eventType(PostReadModelEventType.POST_CREATED)
                .postId(postId)
                .title(title)
                .memberId(memberId)
                .memberName(memberName)
                .build();
    }

    /**
     * 게시글 수정 이벤트용 DLQ
     */
    public static PostReadModelDlq createPostUpdated(String eventId, Long postId, String newTitle) {
        return PostReadModelDlq.builder()
                .eventId(eventId)
                .eventType(PostReadModelEventType.POST_UPDATED)
                .postId(postId)
                .title(newTitle)
                .build();
    }

    /**
     * 단순 이벤트용 DLQ (좋아요 증감, 댓글 수 증감)
     */
    public static PostReadModelDlq createSimpleEvent(PostReadModelEventType eventType, String eventId, Long postId) {
        return PostReadModelDlq.builder()
                .eventId(eventId)
                .eventType(eventType)
                .postId(postId)
                .build();
    }

    /**
     * 조회수 증가 이벤트용 DLQ
     */
    public static PostReadModelDlq createViewIncrement(String eventId, Long postId, Integer deltaValue) {
        return PostReadModelDlq.builder()
                .eventId(eventId)
                .eventType(PostReadModelEventType.VIEW_INCREMENT)
                .postId(postId)
                .deltaValue(deltaValue)
                .build();
    }

    public void markAsProcessed() {
        this.status = DlqStatus.PROCESSED;
    }

    public void markAsFailed() {
        this.status = DlqStatus.FAILED;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }
}
