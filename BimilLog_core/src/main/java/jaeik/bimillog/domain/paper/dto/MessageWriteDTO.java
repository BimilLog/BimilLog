package jaeik.bimillog.domain.paper.dto;

import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.paper.entity.DecoType;
import jaeik.bimillog.domain.paper.entity.Message;
import jakarta.validation.constraints.*;
import lombok.Getter;

/**
 * <h2>롤링페이퍼 메시지 작성용 DTO</h2>
 * @version 2.7.0
 * @author Jaeik
 */
@Getter
public class MessageWriteDTO {

    @NotNull
    private Long ownerId;

    @NotNull
    private DecoType decoType;

    @NotBlank
    @Size(max = 8, message = "익명 이름은 최대 8글자 까지 입력 가능합니다.")
    private String anonymity;

    @NotBlank
    @Size(max = 255, message = "내용은 최대 255자 까지 입력 가능합니다.")
    private String content;

    @Min(value = 0, message = "x는 0 이상이어야 합니다.")
    @Max(value = 11, message = "x는 11 이하여야 합니다.")
    private int x;

    @Min(value = 0, message = "y는 0 이상이어야 합니다.")
    @Max(value = 9, message = "y는 9 이하여야 합니다.")
    private int y;

    /**
     * <h3>Message엔티티로 변환</h3>
     */
    public Message convertDtoToEntity(Member member) {
        return Message.builder()
                .member(member)
                .anonymity(anonymity)
                .content(content)
                .decoType(decoType)
                .x(x)
                .y(y)
                .build();
    }
}
