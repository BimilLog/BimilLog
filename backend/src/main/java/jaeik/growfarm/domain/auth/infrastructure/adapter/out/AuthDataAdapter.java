package jaeik.growfarm.domain.auth.infrastructure.adapter.out;

import jaeik.growfarm.domain.auth.application.port.out.ManageAuthDataPort;
import jaeik.growfarm.domain.comment.application.port.in.CommentCommandUseCase;
import jaeik.growfarm.dto.auth.SocialLoginUserData;
import jaeik.growfarm.dto.user.ClientDTO;
import jaeik.growfarm.dto.user.TokenDTO;
import jaeik.growfarm.entity.notification.FcmToken;
import jaeik.growfarm.entity.user.Setting;
import jaeik.growfarm.entity.user.Token;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.AuthCookieManager;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.notification.FcmTokenRepository;
import jaeik.growfarm.repository.token.TokenRepository;
import jaeik.growfarm.repository.user.SettingRepository;
import jaeik.growfarm.repository.user.UserJdbcRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    private final TokenRepository tokenRepository;
    private final AuthCookieManager authCookieManager;
    private final UserRepository userRepository;
    private final SettingRepository settingRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final UserJdbcRepository userJdbcRepository;
    private final CommentCommandUseCase commentCommandUseCase;
    private final EntityManager entityManager;
    private final TempDataAdapter tempDataAdapter;

    @Override
    @Transactional
    public List<ResponseCookie> saveExistUser(Users user, SocialLoginUserData userData, TokenDTO tokenDTO, String fcmToken) {
        user.updateUserInfo(userData.getNickname(), userData.getProfileImageUrl());

        Token token = tokenRepository.findByUsers(user)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FIND_TOKEN));
        token.updateToken(tokenDTO.getAccessToken(), tokenDTO.getRefreshToken());

        return authCookieManager.generateJwtCookie(new ClientDTO(user,
                tokenRepository.save(token).getId(),
                fcmToken != null ? fcmTokenRepository.save(FcmToken.create(user, fcmToken)).getId() : null));
    }

    @Override
    @Transactional
    public List<ResponseCookie> saveNewUser(String userName, String uuid, SocialLoginUserData userData, TokenDTO tokenDTO, String fcmToken) {
        Users user = userRepository
                .save(Users.createUser(userData, userName, settingRepository.save(Setting.createSetting())));
        tempDataAdapter.removeTempData(uuid);
        return authCookieManager.generateJwtCookie(new ClientDTO(user,
                tokenRepository.save(Token.createToken(tokenDTO, user)).getId(),
                fcmToken != null ? fcmTokenRepository.save(FcmToken.create(user, fcmToken)).getId() : null));
    }

    @Override
    @Transactional
    public void logoutUser(Long userId) {
        userJdbcRepository.deleteAllTokensByUserId(userId);
        fcmTokenRepository.deleteByUsers_Id(userId);
    }

    @Override
    @Transactional
    public void performWithdrawProcess(Long userId) {
        commentCommandUseCase.anonymizeUserComments(userId);

        entityManager.flush();
        entityManager.clear();

        userJdbcRepository.deleteAllTokensByUserId(userId);
        fcmTokenRepository.deleteByUsers_Id(userId);

        userRepository.deleteById(userId);
    }
}