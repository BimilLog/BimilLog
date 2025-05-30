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

    /**
     * <h3>TokenDTO를 Token 엔티티로 변환</h3>
     *
     * <p>
     * TokenDTO 객체를 Token 엔티티로 변환한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param tokenDTO 토큰 DTO
     * @return Token 엔티티
     */
    public Token DTOToToken(TokenDTO tokenDTO) {
        return Token.builder()
                .kakaoAccessToken(tokenDTO.getKakaoAccessToken())
                .kakaoRefreshToken(tokenDTO.getKakaoRefreshToken())
                .jwtRefreshToken(tokenDTO.getJwtRefreshToken())
                .build();
    }

    /**
     * <h3>UserDTO를 Users 엔티티로 변환</h3>
     *
     * <p>
     * UserDTO 객체를 Users 엔티티로 변환한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param userDTO 사용자 DTO
     * @return Users 엔티티
     */
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

    /**
     * <h3>Users 엔티티를 UserDTO로 변환</h3>
     *
     * <p>
     * Users 엔티티를 UserDTO 객체로 변환한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param user Users 엔티티
     * @return 사용자 DTO
     */
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

    /**
     * <h3>Setting 엔티티를 SettingDTO로 변환</h3>
     *
     * <p>
     * Setting 엔티티를 SettingDTO 객체로 변환한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param setting Setting 엔티티
     * @return 설정 DTO
     */
    public SettingDTO settingToSettingDTO(Setting setting) {
        SettingDTO settingDTO = new SettingDTO();
        settingDTO.setCommentNotification(setting.isCommentNotification());
        settingDTO.setFarmNotification(setting.isFarmNotification());
        settingDTO.setPostFeaturedNotification(setting.isPostFeaturedNotification());
        settingDTO.setCommentFeaturedNotification(setting.isCommentFeaturedNotification());
        return settingDTO;
    }

    /**
     * <h3>토큰 ID로 사용자 DTO 조회</h3>
     *
     * <p>
     * 토큰 ID를 통해 사용자를 조회하고 UserDTO로 변환하여 반환한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param tokenId 토큰 ID
     * @return 사용자 DTO
     */
    public UserDTO getUserDTOByTokenId(Long tokenId) {
        Users user = userRepository.findByTokenId(tokenId);
        return UserToDTO(user);
    }
}
