package jaeik.growfarm.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
public class KakaoFriendDTO {

    private Long id;

    private String uuid;

    @JsonProperty("profile_nickname")
    private String profileNickname;

    @JsonProperty("profile_thumbnail_image")
    private String profileThumbnailImage;

    private Boolean favorite;

    @Setter
    private String userName;
}
