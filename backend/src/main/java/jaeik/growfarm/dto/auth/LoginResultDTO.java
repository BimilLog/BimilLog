package jaeik.growfarm.dto.auth;

import jaeik.growfarm.dto.user.TokenDTO;
import lombok.Builder;

/**
 * <h2>로그인 결과 DTO</h2>
 * <p>소셜 로그인 후 사용자 정보와 토큰 정보를 포함하는 DTO입니다.</p>
 *
 * @param userData 사용자 데이터
 * @param tokenDTO 토큰 정보
 * @since 2.0.0
 * @author Jaeik
 */
@Builder
public record LoginResultDTO(SocialLoginUserData userData, TokenDTO tokenDTO) {
}
