package jaeik.growfarm.dto.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class KakaoFriendListDTO {
    private List<KakaoFriendDTO> elements;

    @JsonProperty("total_count")
    private int totalCount;
}
