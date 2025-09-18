package jaeik.bimillog.infrastructure.adapter.out.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jaeik.bimillog.domain.user.entity.KakaoFriendsResponseVO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

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

    /**
     * <h3>DTO를 VO로 변환</h3>
     * <p>인프라 KakaoFriendsResponse를 도메인 VO로 변환합니다.</p>
     *
     * @return KakaoFriendsResponseVO 객체
     */
    public KakaoFriendsResponseVO toVO() {
        List<KakaoFriendsResponseVO.Friend> voElements = null;
        if (elements != null) {
            voElements = elements.stream()
                    .map(Friend::toVO)
                    .toList();
        }
        return KakaoFriendsResponseVO.of(voElements, totalCount, beforeUrl, afterUrl, favoriteCount);
    }

    /**
     * <h3>VO에서 DTO로 변환</h3>
     * <p>도메인 KakaoFriendsResponseVO를 인프라 DTO로 변환합니다.</p>
     *
     * @param vo 도메인 KakaoFriendsResponseVO
     * @return KakaoFriendsDTO DTO
     */
    public static KakaoFriendsDTO fromVO(KakaoFriendsResponseVO vo) {
        KakaoFriendsDTO response = new KakaoFriendsDTO();
        if (vo.elements() != null) {
            response.elements = vo.elements().stream()
                    .map(Friend::fromVO)
                    .collect(Collectors.toList());
        }
        response.totalCount = vo.totalCount();
        response.beforeUrl = vo.beforeUrl();
        response.afterUrl = vo.afterUrl();
        response.favoriteCount = vo.favoriteCount();
        return response;
    }

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
        private String userName;

        /**
         * <h3>DTO를 VO로 변환</h3>
         * <p>인프라 Friend DTO를 도메인 VO로 변환합니다.</p>
         *
         * @return KakaoFriendsResponseVO.Friend 객체
         */
        public KakaoFriendsResponseVO.Friend toVO() {
            return KakaoFriendsResponseVO.Friend.of(id, uuid, profileNickname, profileThumbnailImage, favorite, userName);
        }

        /**
         * <h3>VO에서 DTO로 변환</h3>
         * <p>도메인 KakaoFriendsResponseVO.Friend를 인프라 DTO로 변환합니다.</p>
         *
         * @param vo 도메인 KakaoFriendsResponseVO.Friend
         * @return Friend 객체
         */
        public static Friend fromVO(KakaoFriendsResponseVO.Friend vo) {
            Friend dto = new Friend();
            dto.id = vo.id();
            dto.uuid = vo.uuid();
            dto.profileNickname = vo.profileNickname();
            dto.profileThumbnailImage = vo.profileThumbnailImage();
            dto.favorite = vo.favorite();
            dto.userName = vo.userName();
            return dto;
        }
    }
}
