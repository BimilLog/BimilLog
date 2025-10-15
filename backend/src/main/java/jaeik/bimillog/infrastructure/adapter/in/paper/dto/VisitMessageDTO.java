package jaeik.bimillog.infrastructure.adapter.in.paper.dto;

import jaeik.bimillog.domain.paper.entity.DecoType;
import jaeik.bimillog.domain.paper.entity.VisitMessageDetail;
import lombok.Getter;
import lombok.Setter;

/**
 * <h2>롤링페이퍼 방문 메시지 DTO</h2>
 * <p>다른 사용자의 롤링페이퍼 방문 시 메시지의 그리드 레이아웃 정보를 전달하는 데이터 전송 객체입니다.</p>
 * <p>메시지 내용과 익명 작성자명을 제외하고 위치, 크기, 장식 정보만 포함합니다</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Setter
public class VisitMessageDTO {

    private Long id;

    private Long memberId;

    private DecoType decoType;

    private int x;

    private int y;

    /**
     * <h3>VisitMessageDetail VO에서 VisitMessageDTO 변환</h3>
     * <p>VisitMessageDetail 값 객체를 DTO로 변환합니다.</p>
     * <p>PaperQueryController에서 호출되어 도메인 데이터를 HTTP 응답용 DTO로 변환합니다.</p>
     *
     * @param visitMessageDetail 변환할 VisitMessageDetail 도메인 값 객체 (레이아웃 정보만 포함)
     * @return VisitMessageDTO 변환된 방문 메시지 DTO (클라이언트 전달용)
     * @author Jaeik
     * @since 2.0.0
     */
    public static VisitMessageDTO from(VisitMessageDetail visitMessageDetail) {
        VisitMessageDTO dto = new VisitMessageDTO();
        dto.id = visitMessageDetail.id();
        dto.memberId = visitMessageDetail.memberId();
        dto.decoType = visitMessageDetail.decoType();
        dto.x = visitMessageDetail.x();
        dto.y = visitMessageDetail.y();
        return dto;
    }
}
