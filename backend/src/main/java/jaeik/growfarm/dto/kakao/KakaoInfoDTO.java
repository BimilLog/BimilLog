package jaeik.growfarm.dto.kakao;

import lombok.Getter;
import lombok.Setter;

// 카카오 사용자 정보 DTO
@Getter @Setter
public class KakaoInfoDTO {

    private Long kakaoId;

    private String kakaoNickname;

    private String thumbnailImage;
}
