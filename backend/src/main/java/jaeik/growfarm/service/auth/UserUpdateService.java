package jaeik.growfarm.service.auth;

import jaeik.growfarm.dto.kakao.KakaoInfoDTO;
import jaeik.growfarm.dto.user.TokenDTO;
import jaeik.growfarm.dto.user.UserDTO;
import jaeik.growfarm.entity.user.Token;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.JwtTokenProvider;
import jaeik.growfarm.repository.user.TokenRepository;
import jaeik.growfarm.util.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserUpdateService {

    private final TokenRepository tokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserUtil userUtil;

    @Transactional
    public TokenDTO updateUserInfo(Users user, KakaoInfoDTO kakaoInfoDTO, TokenDTO tokenDTO) {
        user.updateUserInfo(kakaoInfoDTO.getKakaoNickname(), kakaoInfoDTO.getThumbnailImage());
        TokenDTO updatetokenDTO = createJwtToken(user, tokenDTO);
        Token token = Token.DTOToToken(updatetokenDTO, user);
        tokenRepository.save(token);
        return updatetokenDTO;
    }

    /**
     * <h3>JWT 토큰 생성</h3>
     *
     * <p>사용자 정보를 기반으로 JWT 액세스 토큰과 리프레시 토큰을 생성한다.</p>
     *
     * @param user 사용자 엔티티
     * @return Result JWT 액세스 토큰과 리프레시 토큰
     * @author Jaeik
     * @since 1.0.0
     */
    private TokenDTO createJwtToken(Users user, TokenDTO tokenDTO) {
        UserDTO userDTO = userUtil.UserToDTO(user);
        String jwtAccessToken = jwtTokenProvider.generateAccessToken(userDTO);
        String jwtRefreshToken = jwtTokenProvider.generateRefreshToken(userDTO);
        tokenDTO.setJwtAccessToken(jwtAccessToken);
        tokenDTO.setJwtRefreshToken(jwtRefreshToken);
        return tokenDTO;
    }
}
