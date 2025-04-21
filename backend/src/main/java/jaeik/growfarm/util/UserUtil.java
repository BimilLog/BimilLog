package jaeik.growfarm.util;

import jaeik.growfarm.dto.user.TokenDTO;
import jaeik.growfarm.dto.user.UserDTO;
import jaeik.growfarm.entity.user.Token;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
        return userDTO;
    }

    public UserDTO getUserDTOByTokenId(Long tokenId) {
        Users user = userRepository.findByTokenId(tokenId);
        return UserToDTO(user);
    }
}
