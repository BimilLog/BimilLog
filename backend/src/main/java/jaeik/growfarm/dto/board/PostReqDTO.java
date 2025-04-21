package jaeik.growfarm.dto.board;

import lombok.Getter;
import lombok.Setter;

// 글 생성 요청 시 전달 받는 DTO
@Getter
@Setter
public class PostReqDTO {

    private Long userId;

    private String farmName;

    private String title;

    private String content;
}
