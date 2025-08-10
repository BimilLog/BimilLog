package jaeik.growfarm.service.auth;

import jaeik.growfarm.dto.auth.SocialLoginUserData;
import jaeik.growfarm.dto.kakao.KakaoInfoDTO;
import jaeik.growfarm.dto.user.TokenDTO;
import jaeik.growfarm.entity.user.Token;
import jaeik.growfarm.entity.user.Users;
import org.springframework.http.ResponseCookie;

import java.util.List;

public interface AuthUpdateService {

    List<ResponseCookie> saveExistUser(Users user, SocialLoginUserData userData, TokenDTO tokenDTO, String fcmToken);

    List<ResponseCookie> saveNewUser(String userName, String uuid, SocialLoginUserData userData, TokenDTO tokenDTO, String fcmToken);

    void logoutUser(Long userId);

    void performWithdrawProcess(Long userId);

    String renewalKaKaoToken(Token token);
}
