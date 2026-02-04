package jaeik.bimillog.domain.post.entity.jpa;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * <h2>처리된 이벤트 엔티티</h2>
 * <p>이벤트 멱등성 보장을 위해 처리된 이벤트 ID를 저장합니다.</p>
 *
 * @author Jaeik
 * @version 2.6.0
 */
@Entity
@Table(name = "processed_event",
        indexes = @Index(name = "idx_processed_event_type_at", columnList = "event_type, processed_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedEvent {

    @Id
    @Column(name = "event_id", length = 64)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    public ProcessedEvent(String eventId, String eventType) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.processedAt = LocalDateTime.now();
    }
}
