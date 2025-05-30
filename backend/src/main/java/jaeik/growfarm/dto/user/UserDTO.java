package jaeik.growfarm.dto.user;

import jaeik.growfarm.entity.user.UserRole;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * <h3>사용자 정보 DTO</h3>
 * <p>
 * 사용자의 기본 정보를 담는 데이터 전송 객체
 * </p>
 * 
 * @since 1.0.0
 * @author Jaeik
 */
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