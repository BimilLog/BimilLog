package jaeik.bimillog.domain.member.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@NoArgsConstructor
public class KakaoFriendsDTO {

    private List<Friend> elements;

    @JsonProperty("total_count")
    private Integer totalCount;

    @JsonProperty("before_url")
    private String beforeUrl;

    @JsonProperty("after_url")
    private String afterUrl;

    @JsonProperty("favorite_count")
    private Integer favoriteCount;

    @Getter
    @NoArgsConstructor
    public static class Friend {

        private Long id;

        private String uuid;

        @JsonProperty("profile_nickname")
        private String profileNickname;

        @JsonProperty("profile_thumbnail_image")
        private String profileThumbnailImage;

        private Boolean favorite;

        @Setter
        private String memberName;
    }
}
