package jaeik.growfarm.util;

import jaeik.growfarm.dto.user.SettingDTO;
import jaeik.growfarm.dto.user.TokenDTO;
import jaeik.growfarm.dto.user.UserDTO;
import jaeik.growfarm.entity.user.Setting;
import jaeik.growfarm.entity.user.Token;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/*
 * UserUtil 클래스
 * 사용자 관련 유틸리티 클래스
 * 수정일 : 2025-05-03
 */
@Component
@RequiredArgsConstructor
public class UserUtil {

    private final UserRepository userRepository;

    public Token DTOToToken(TokenDTO tokenDTO) {
        return Token.builder()
                .kakaoAccessToken(tokenDTO.getKakaoAccessToken())
                .kakaoRefreshToken(tokenDTO.getKakaoRefreshToken())
                .jwtRefreshToken(tokenDTO.getJwtRefreshToken())
                .build();
    }

    public Users DTOToUser(UserDTO userDTO) {
        return Users.builder()
                .id(userDTO.getUserId())
                .kakaoId(userDTO.getKakaoId())
                .farmName(userDTO.getFarmName())
                .role(userDTO.getRole())
                .kakaoNickname(userDTO.getKakaoNickname())
                .thumbnailImage(userDTO.getThumbnailImage())
                .build();
    }

    public UserDTO UserToDTO(Users user) {
        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(user.getId());
        userDTO.setKakaoId(user.getKakaoId());
        userDTO.setFarmName(user.getFarmName());
        userDTO.setRole(user.getRole());
        userDTO.setKakaoNickname(user.getKakaoNickname());
        userDTO.setThumbnailImage(user.getThumbnailImage());
        userDTO.setTokenId(user.getToken().getId());
        userDTO.setSettingId(user.getSetting().getId());
        return userDTO;
    }

    // Setting -> SettingDTO
    public SettingDTO settingToSettingDTO(Setting setting) {
        SettingDTO settingDTO = new SettingDTO();
        settingDTO.setCommentNotification(setting.isCommentNotification());
        settingDTO.setFarmNotification(setting.isFarmNotification());
        settingDTO.setPostFeaturedNotification(setting.isPostFeaturedNotification());
        settingDTO.setCommentFeaturedNotification(setting.isCommentFeaturedNotification());
        return settingDTO;
    }

    public UserDTO getUserDTOByTokenId(Long tokenId) {
        Users user = userRepository.findByTokenId(tokenId);
        return UserToDTO(user);
    }
}
