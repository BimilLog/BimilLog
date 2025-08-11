package jaeik.growfarm.domain.auth.infrastructure.adapter.out;

import jaeik.growfarm.domain.auth.application.port.out.ManageAuthDataPort;
import jaeik.growfarm.dto.auth.SocialLoginUserData;
import jaeik.growfarm.dto.user.TokenDTO;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.service.auth.AuthUpdateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <h2>인증 데이터 어댑터</h2>
 * <p>사용자 데이터 저장, 업데이트, 삭제를 위한 어댑터</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class AuthDataAdapter implements ManageAuthDataPort {

    private final AuthUpdateService authUpdateService;

    @Override
    public List<ResponseCookie> saveExistUser(Users user, SocialLoginUserData userData, TokenDTO tokenDTO, String fcmToken) {
        return authUpdateService.saveExistUser(user, userData, tokenDTO, fcmToken);
    }

    @Override
    public List<ResponseCookie> saveNewUser(String userName, String uuid, SocialLoginUserData userData, TokenDTO tokenDTO, String fcmToken) {
        return authUpdateService.saveNewUser(userName, uuid, userData, tokenDTO, fcmToken);
    }

    @Override
    public void logoutUser(Long userId) {
        authUpdateService.logoutUser(userId);
    }

    @Override
    public void performWithdrawProcess(Long userId) {
        authUpdateService.performWithdrawProcess(userId);
    }
}