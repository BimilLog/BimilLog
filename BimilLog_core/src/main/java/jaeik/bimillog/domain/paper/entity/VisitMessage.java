package jaeik.bimillog.domain.paper.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;



/**
 * <h2>롤링페이퍼 방문 메시지 DTO</h2>
 * <p>다른 사용자의 롤링페이퍼 방문 시 데이터 전송 객체입니다.</p>
 * <p>메시지 내용과 익명 작성자명을 제외하고 위치, 크기, 장식 정보만 포함합니다</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Setter
@AllArgsConstructor
public class VisitMessage {

    private Long id;

    private Long memberId;

    private DecoType decoType;

    private int x;

    private int y;

    /**
     * <h3>Message 엔티티에서 VisitMessageDTO 변환</h3>
     * <p>VisitMessageDetail 값 객체를 DTO로 변환합니다.</p>
     *
     * @param message 메시지 엔티티
     * @return VisitMessageDTO 변환된 방문 메시지 DTO
     * @author Jaeik
     * @since 2.0.0
     */
    public static VisitMessage from(Message message) {
        return new VisitMessage(
                message.getId(),
                message.getMemberId(),
                message.getDecoType(),
                message.getX(),
                message.getY()
        );
    }
}
