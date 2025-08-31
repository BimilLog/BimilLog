package jaeik.bimillog.infrastructure.adapter.user.out.social.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jaeik.bimillog.domain.user.entity.KakaoFriendVO;
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

    /**
     * <h3>DTO를 VO로 변환</h3>
     * <p>인프라 KakaoFriendDTO를 도메인 VO로 변환합니다.</p>
     *
     * @return KakaoFriendVO 객체
     */
    public KakaoFriendVO toVO() {
        return KakaoFriendVO.of(id, uuid, profileNickname, profileThumbnailImage, favorite, userName);
    }

    /**
     * <h3>VO에서 DTO로 변환</h3>
     * <p>도메인 KakaoFriendVO를 인프라 DTO로 변환합니다.</p>
     *
     * @param vo 도메인 KakaoFriendVO
     * @return KakaoFriendDTO 객체
     */
    public static KakaoFriendDTO fromVO(KakaoFriendVO vo) {
        KakaoFriendDTO dto = new KakaoFriendDTO();
        dto.id = vo.id();
        dto.uuid = vo.uuid();
        dto.profileNickname = vo.profileNickname();
        dto.profileThumbnailImage = vo.profileThumbnailImage();
        dto.favorite = vo.favorite();
        dto.userName = vo.userName();
        return dto;
    }
}
