package jaeik.growfarm.infrastructure.adapter.user.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jaeik.growfarm.infrastructure.adapter.user.out.social.dto.KakaoFriendDTO;
import jaeik.growfarm.domain.user.entity.KakaoFriendsResponseVO;
import jaeik.growfarm.domain.user.entity.KakaoFriendVO;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

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

    /**
     * <h3>DTO를 VO로 변환</h3>
     * <p>인프라 KakaoFriendsResponse를 도메인 VO로 변환합니다.</p>
     *
     * @return KakaoFriendsResponseVO 객체
     */
    public KakaoFriendsResponseVO toVO() {
        List<KakaoFriendVO> voElements = null;
        if (elements != null) {
            voElements = elements.stream()
                    .map(KakaoFriendDTO::toVO)
                    .collect(Collectors.toList());
        }
        return KakaoFriendsResponseVO.of(voElements, totalCount, beforeUrl, afterUrl, favoriteCount);
    }

    /**
     * <h3>VO에서 DTO로 변환</h3>
     * <p>도메인 KakaoFriendsResponseVO를 인프라 DTO로 변환합니다.</p>
     *
     * @param vo 도메인 KakaoFriendsResponseVO
     * @return KakaoFriendsResponse DTO
     */
    public static KakaoFriendsResponse fromVO(KakaoFriendsResponseVO vo) {
        KakaoFriendsResponse response = new KakaoFriendsResponse();
        if (vo.elements() != null) {
            response.elements = vo.elements().stream()
                    .map(KakaoFriendDTO::fromVO)
                    .collect(Collectors.toList());
        }
        response.totalCount = vo.totalCount();
        response.beforeUrl = vo.beforeUrl();
        response.afterUrl = vo.afterUrl();
        response.favoriteCount = vo.favoriteCount();
        return response;
    }
}
