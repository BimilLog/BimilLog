package jaeik.bimillog.domain.paper.dto;

import jaeik.bimillog.domain.paper.entity.DecoType;
import jaeik.bimillog.domain.paper.entity.Message;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * <h2>롤링페이퍼 메시지 DTO</h2>
 * <p>클라이언트와 서버 간 롤링페이퍼 메시지 데이터 전송을 위한 데이터 전송 객체입니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Setter
@AllArgsConstructor
public class MessageDTO {

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
     * <h3>Message 엔티티에서 MessageDTO 변환</h3>
     *
     * @param message 변환할 엔티티
     * @return MessageDTO 변환된 메시지 DTO
     * @author Jaeik
     * @since 2.0.0
     */
    public static MessageDTO from(Message message) {
        return new MessageDTO(
                message.getId(),
                message.getMemberId(),
                message.getDecoType(),
                message.getAnonymity(),
                message.getContent(),
                message.getX(),
                message.getY(),
                message.getCreatedAt()
        );
    }

    /**
     * <h3>메시지 작성 시 decoType 필수 검증</h3>
     * <p>id가 null이면 작성 모드로 판단하여 decoType이 필수입니다.</p>
     * <p>id가 있으면 삭제/조회 모드로 판단하여 decoType 검증을 생략합니다.</p>
     *
     * @return true이면 검증 통과, false이면 검증 실패
     * @author Jaeik
     * @since 2.0.0
     */
    @AssertTrue(message = "메시지 작성 시 decoType은 필수입니다.")
    public boolean isDecoTypeValidForWrite() {
        return id != null || decoType != null;
    }

    /**
     * <h3>메시지 작성 시 content 필수 검증</h3>
     * <p>id가 null이면 작성 모드로 판단하여 content가 필수입니다.</p>
     * <p>빈 문자열과 공백만 있는 문자열은 유효하지 않습니다.</p>
     *
     * @return true이면 검증 통과, false이면 검증 실패
     * @author Jaeik
     * @since 2.0.0
     */
    @AssertTrue(message = "메시지 작성 시 내용은 필수입니다.")
    public boolean isContentValidForWrite() {
        return id != null || (content != null && !content.trim().isEmpty());
    }

    /**
     * <h3>그리드 X 좌표 범위 검증</h3>
     * <p>메시지 작성 시 x는 0~11 사이여야 합니다.</p>
     * <p>PC 그리드는 6x10 2페이지, Mobile 그리드는 4x10 3페이지 고려한 제한입니다.</p>
     *
     * @return true이면 검증 통과, false이면 검증 실패
     * @author Jaeik
     * @since 2.0.0
     */
    @AssertTrue(message = "x는 0~11 사이여야 합니다.")
    public boolean isXValid() {
        return id != null || (x >= 0 && x <= 11);
    }

    /**
     * <h3>그리드 Y 좌표 범위 검증</h3>
     * <p>메시지 작성 시 y는 0~9 사이여야 합니다.</p>
     * <p>PC와 Mobile 그리드 모두 최대 10줄까지 지원합니다.</p>
     *
     * @return true이면 검증 통과, false이면 검증 실패
     * @author Jaeik
     * @since 2.0.0
     */
    @AssertTrue(message = "y는 0~9 사이여야 합니다.")
    public boolean isYValid() {
        return id != null || (y >= 0 && y <= 9);
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
