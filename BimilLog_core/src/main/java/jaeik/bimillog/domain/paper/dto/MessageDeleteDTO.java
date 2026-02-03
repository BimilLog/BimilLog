package jaeik.bimillog.domain.paper.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * <h2>메시지 삭제 DTO</h2>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@Getter
@Setter
public class MessageDeleteDTO {

    @NotNull
    private Long messageId;
}
