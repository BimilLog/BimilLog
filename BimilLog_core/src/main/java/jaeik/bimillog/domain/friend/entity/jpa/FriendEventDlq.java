package jaeik.bimillog.domain.friend.entity.jpa;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * <h2>친구 이벤트 DLQ 엔티티</h2>
 * <p>Redis 친구 관련 이벤트 처리 실패 시 재처리를 위해 저장하는 Dead Letter Queue 엔티티입니다.</p>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "friend_event_dlq", indexes = @Index(name = "idx_dlq_status_created", columnList = "status, created_at"))
public class FriendEventDlq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FriendEventType type;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    private Double score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FriendDlqStatus status;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 친구 추가 이벤트용 DLQ 엔티티를 생성합니다.
     * eventId는 deterministic하게 생성되어 동일한 친구 추가 이벤트의 중복 저장을 방지합니다.
     */
    public static FriendEventDlq createFriendAdd(Long memberId, Long friendId) {
        return FriendEventDlq.builder()
                .eventId("FRIEND_ADD:" + memberId + ":" + friendId)
                .type(FriendEventType.FRIEND_ADD)
                .memberId(memberId)
                .targetId(friendId)
                .build();
    }

    /**
     * 친구 삭제 이벤트용 DLQ 엔티티를 생성합니다.
     * eventId는 deterministic하게 생성되어 동일한 친구 삭제 이벤트의 중복 저장을 방지합니다.
     */
    public static FriendEventDlq createFriendRemove(Long memberId, Long friendId) {
        return FriendEventDlq.builder()
                .eventId("FRIEND_REMOVE:" + memberId + ":" + friendId)
                .type(FriendEventType.FRIEND_REMOVE)
                .memberId(memberId)
                .targetId(friendId)
                .build();
    }

    /**
     * 상호작용 점수 증가 이벤트용 DLQ 엔티티를 생성합니다.
     * eventId는 호출자가 제공하여 각 이벤트를 개별적으로 추적합니다.
     */
    public static FriendEventDlq createScoreUp(String eventId, Long memberId, Long targetId, Double score) {
        return FriendEventDlq.builder()
                .eventId(eventId)
                .type(FriendEventType.SCORE_UP)
                .memberId(memberId)
                .targetId(targetId)
                .score(score)
                .build();
    }

    public void markAsProcessed() {
        this.status = FriendDlqStatus.PROCESSED;
    }

    public void markAsFailed() {
        this.status = FriendDlqStatus.FAILED;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }
}
