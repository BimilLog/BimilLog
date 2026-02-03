package jaeik.bimillog.domain.paper.entity;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * <h2>자신의 롤링페이퍼 메시지 엔티티</h2>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Setter
@AllArgsConstructor
public class MyMessage {
    private Long id;
    private Long memberId;
    private DecoType decoType;
    @Size(max = 8, message = "익명 이름은 최대 8글자 까지 입력 가능합니다.")
    private String anonymity;
    @Size(max = 255, message = "내용은 최대 255자 까지 입력 가능합니다.")
    private String content;
    private int x;
    private int y;
    private Instant createdAt;

    /**
     * <h3>Message 엔티티에서 MyMessage 변환</h3>
     *
     * @param message 변환할 엔티티
     * @return MessageDTO 변환된 메시지 DTO
     * @author Jaeik
     * @since 2.0.0
     */
    public static MyMessage from(Message message) {
        return new MyMessage(
                message.getId(),
                message.getMember().getId(),
                message.getDecoType(),
                message.getAnonymity(),
                message.getContent(),
                message.getX(),
                message.getY(),
                message.getCreatedAt()
        );
    }

    /**
     * <h3>메시지 삭제 시 id 필수 검증</h3>
     * <p>id가 있으면 삭제 모드로 판단하여 id가 양수여야 합니다.</p>
     * <p>id가 null이거나 0 이하면 유효하지 않습니다.</p>
     *
     * @return true이면 검증 통과, false이면 검증 실패
     * @author Jaeik
     * @since 2.0.0
     */
    @AssertTrue(message = "메시지 삭제 시 messageId는 필수입니다.")
    public boolean isIdValidForDelete() {
        return id == null || id > 0;
    }
}
