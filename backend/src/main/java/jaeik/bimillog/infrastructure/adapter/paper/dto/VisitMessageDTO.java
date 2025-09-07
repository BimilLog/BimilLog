package jaeik.bimillog.infrastructure.adapter.paper.dto;

import jaeik.bimillog.domain.paper.entity.DecoType;
import jaeik.bimillog.domain.paper.entity.VisitMessageDetail;
import lombok.Getter;
import lombok.Setter;

/**
 * <h2> 방문 메시지 DTO</h2>
 * <p>
 * 다른 사람의 롤링 페이퍼 방문시 전달 받는 DTO
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Setter
public class VisitMessageDTO {

    private Long id;

    private Long userId;

    private DecoType decoType;

    private int width;

    private int height;

    /**
     * <h3>도메인 VO로부터 DTO 생성</h3>
     * <p>VisitMessageDetail 도메인 값 객체를 DTO로 변환합니다.</p>
     *
     * @param visitMessageDetail 도메인 값 객체
     * @return VisitMessageDTO
     * @author Jaeik
     * @since 2.0.0
     */
    public static VisitMessageDTO from(VisitMessageDetail visitMessageDetail) {
        VisitMessageDTO dto = new VisitMessageDTO();
        dto.id = visitMessageDetail.id();
        dto.userId = visitMessageDetail.userId();
        dto.decoType = visitMessageDetail.decoType();
        dto.width = visitMessageDetail.width();
        dto.height = visitMessageDetail.height();
        return dto;
    }
}
