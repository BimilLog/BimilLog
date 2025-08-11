package jaeik.growfarm.dto.paper;

import jaeik.growfarm.domain.message.domain.DecoType;
import lombok.Getter;
import lombok.Setter;

/**
 * <h2> 방문 메시지 DTO</h2>
 * <p>
 * 다른 사람의 롤링 페이퍼 방문시 전달 받는 DTO
 * </p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Getter
@Setter
public class VisitMessageDTO {

    private Long id;

    private Long userId;

    private DecoType decoType;

    private int width;

    private int height;
}
