package jaeik.bimillog.domain.paper.dto;

import jaeik.bimillog.domain.paper.entity.DecoType;
import jaeik.bimillog.domain.paper.entity.Message;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * <h2>메시지 삭제 DTO</h2>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@Getter
@Setter
@AllArgsConstructor
public class MessageDeleteDTO {
    @NotNull
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
}
