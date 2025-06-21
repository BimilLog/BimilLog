package jaeik.growfarm.dto.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * <h2>카카오 친구 정보 DTO</h2>
 * <p>
 * 카카오 친구 목록에서 각 친구의 정보를 담는 데이터 전송 객체
 * </p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Getter
@Setter
public class KakaoFriendDTO {
    private Long id; // 친구의 고유 카카오 ID

    private String uuid; // 친구의 UUID

    @JsonProperty("profile_nickname")
    private String profileNickname;

    @JsonProperty("profile_thumbnail_image")
    private String profileThumbnailImage;

    @Size(max = 8, message = "닉네임은 최대 8글자 까지 입력 가능합니다.")
    private String userName;
}
