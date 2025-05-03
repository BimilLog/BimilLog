package jaeik.growfarm.dto.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

// 카카오 친구 DTO
@Getter
@Setter
public class KakaoFriendDTO {
    private Long id; // 친구의 고유 카카오 ID

    private String uuid; // 친구의 UUID

    @JsonProperty("profile_nickname")
    private String profileNickname;

    @JsonProperty("profile_thumbnail_image")
    private String profileThumbnailImage;
}
