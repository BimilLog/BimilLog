package jaeik.growfarm.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TokenDTO {

    @JsonProperty("access_token")
    private String kakaoAccessToken;

    @JsonProperty("refresh_token")
    private String kakaoRefreshToken;

    private String jwtAccessToken;

    private String jwtRefreshToken;
}