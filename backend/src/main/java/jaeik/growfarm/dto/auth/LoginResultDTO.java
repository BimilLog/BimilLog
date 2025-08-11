package jaeik.growfarm.dto.auth;

import jaeik.growfarm.dto.user.TokenDTO;
import lombok.Builder;

@Builder
public record LoginResultDTO(SocialLoginUserData userData, TokenDTO tokenDTO) {
}
