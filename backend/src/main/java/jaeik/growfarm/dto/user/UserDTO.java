package jaeik.growfarm.dto.user;

import jaeik.growfarm.entity.user.UserRole;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

// 사용자 정보 DTO
@Setter
@Getter
@RequiredArgsConstructor
public class UserDTO {

    private Long userId;

    private Long kakaoId;

    private Long tokenId;

    private Long settingId;

    private String kakaoNickname;

    private String thumbnailImage;

    private String farmName;

    private UserRole role;
}