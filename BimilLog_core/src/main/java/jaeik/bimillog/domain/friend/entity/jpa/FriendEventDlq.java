package jaeik.bimillog.domain.friend.entity.jpa;

import jaeik.bimillog.domain.friend.entity.FriendEventType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * <h2>친구 이벤트 DLQ 엔티티</h2>
 * <p>Redis 친구 관련 이벤트 처리 실패 시 재처리를 위해 저장하는 Dead Letter Queue 엔티티입니다.</p>
 *
 * @author Jaeik
 * @version 2.5.0
 */
@Entity
@Table(name = "friend_event_dlq",
        indexes = @Index(name = "idx_dlq_status_created", columnList = "status, created_at"),
        uniqueConstraints = @UniqueConstraint(
                name = "uk_dlq_pending_event",
                columnNames = {"type", "member_id", "target_id", "status"}
        ))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FriendEventDlq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FriendEventType type;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column
    private Double score;

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
    private FriendEventDlq(FriendEventType type, Long memberId, Long targetId, Double score) {
        this.type = type;
        this.memberId = memberId;
        this.targetId = targetId;
        this.score = score;
        this.status = DlqStatus.PENDING;
        this.retryCount = 0;
        this.createdAt = LocalDateTime.now();
    }

    public static FriendEventDlq createFriendAdd(Long memberId, Long friendId) {
        return FriendEventDlq.builder()
                .type(FriendEventType.FRIEND_ADD)
                .memberId(memberId)
                .targetId(friendId)
                .build();
    }

    public static FriendEventDlq createFriendRemove(Long memberId, Long friendId) {
        return FriendEventDlq.builder()
                .type(FriendEventType.FRIEND_REMOVE)
                .memberId(memberId)
                .targetId(friendId)
                .build();
    }

    public static FriendEventDlq createScoreUp(Long memberId, Long targetId, Double score) {
        return FriendEventDlq.builder()
                .type(FriendEventType.SCORE_UP)
                .memberId(memberId)
                .targetId(targetId)
                .score(score)
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
