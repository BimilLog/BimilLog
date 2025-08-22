package jaeik.growfarm.infrastructure.adapter.user.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jaeik.growfarm.infrastructure.adapter.user.out.social.dto.KakaoFriendDTO;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class KakaoFriendsResponse {

    private List<KakaoFriendDTO> elements;

    @JsonProperty("total_count")
    private Integer totalCount;

    @JsonProperty("before_url")
    private String beforeUrl;

    @JsonProperty("after_url")
    private String afterUrl;

    @JsonProperty("favorite_count")
    private Integer favoriteCount;
}
