package jaeik.growfarm.dto.board;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// 글 생성 요청 시 전달 받는 DTO
@Getter
@Setter
public class PostReqDTO {

    private Long userId;

    @Size(max = 8, message = "농장 이름은 최대 8글자 까지 입력 가능합니다.")
    private String farmName;

    @Size(max = 30, message = "글 제목은 최대 30자 까지 입력 가능합니다.")
    private String title;

    @Size(max = 1000, message = "글 내용은 최대 1000자 까지 입력 가능합니다.")
    private String content;
}
