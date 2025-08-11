package jaeik.growfarm.domain.auth.infrastructure.adapter.out;

import jaeik.growfarm.domain.auth.application.port.out.ManageAuthDataPort;
import jaeik.growfarm.domain.comment.application.port.in.CommentCommandUseCase;
import jaeik.growfarm.domain.notification.domain.FcmToken;
import jaeik.growfarm.domain.user.domain.Setting;
import jaeik.growfarm.domain.user.domain.Token;
import jaeik.growfarm.domain.user.domain.User;
import jaeik.growfarm.dto.auth.SocialLoginUserData;
import jaeik.growfarm.dto.user.ClientDTO;
import jaeik.growfarm.dto.user.TokenDTO;
import jaeik.growfarm.global.auth.AuthCookieManager;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.domain.notification.infrastructure.adapter.out.persistence.FcmTokenRepository;
import jaeik.growfarm.domain.auth.infrastructure.adapter.out.persistence.TokenRepository;
import jaeik.growfarm.domain.user.infrastructure.adapter.out.persistence.SettingRepository;
import jaeik.growfarm.domain.user.infrastructure.adapter.out.persistence.UserJdbcRepository;
import jaeik.growfarm.domain.user.infrastructure.adapter.out.persistence.UserRepository;
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
    public List<ResponseCookie> saveExistUser(User user, SocialLoginUserData userData, TokenDTO tokenDTO, String fcmToken) {
        user.updateUserInfo(userData.getNickname(), userData.getProfileImageUrl());

        Token token = tokenRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FIND_TOKEN));
        token.updateToken(tokenDTO.getAccessToken(), tokenDTO.getRefreshToken());

        return authCookieManager.generateJwtCookie(ClientDTO.of(user,
                tokenRepository.save(token).getId(),
                fcmToken != null ? fcmTokenRepository.save(FcmToken.create(user, fcmToken)).getId() : null));
    }

    @Override
    @Transactional
    public List<ResponseCookie> saveNewUser(String userName, String uuid, SocialLoginUserData userData, TokenDTO tokenDTO, String fcmToken) {
        User user = userRepository
                .save(User.createUser(userData, userName, settingRepository.save(Setting.createSetting())));
        tempDataAdapter.removeTempData(uuid);
        return authCookieManager.generateJwtCookie(ClientDTO.of(user,
                tokenRepository.save(Token.createToken(tokenDTO, user)).getId(),
                fcmToken != null ? fcmTokenRepository.save(FcmToken.create(user, fcmToken)).getId() : null));
    }

    @Override
    @Transactional
    public void logoutUser(Long userId) {
        userJdbcRepository.deleteAllTokensByUserId(userId);
        fcmTokenRepository.deleteByUser_Id(userId);
    }

    @Override
    @Transactional
    public void performWithdrawProcess(Long userId) {
        commentCommandUseCase.anonymizeUserComments(userId);

        entityManager.flush();
        entityManager.clear();

        userJdbcRepository.deleteAllTokensByUserId(userId);
        fcmTokenRepository.deleteByUser_Id(userId);

        userRepository.deleteById(userId);
    }
}