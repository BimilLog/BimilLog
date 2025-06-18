package jaeik.growfarm.dto.paper;

import jaeik.growfarm.entity.message.DecoType;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// 농작물 DTO
@Getter
@Setter
public class MessageDTO {

    private Long id;

    @Size(max = 8, message = "닉네임은 최대 8글자 까지 입력 가능합니다.")
    private String userName;

    private DecoType decoType;

    @Size(max = 8, message = "익명 이름은 최대 8글자 까지 입력 가능합니다.")
    private String anonymity;

    @Size(max = 255, message = "내용은 최대 255자 까지 입력 가능합니다.")
    private String message;

    private int width;

    private int height;
}
