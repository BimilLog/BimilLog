package jaeik.growfarm.service.auth;

import jaeik.growfarm.dto.auth.SocialLoginUserData;
import jaeik.growfarm.dto.user.ClientDTO;
import jaeik.growfarm.dto.user.TokenDTO;
import jaeik.growfarm.entity.notification.FcmToken;
import jaeik.growfarm.entity.user.BlackList;
import jaeik.growfarm.entity.user.Setting;
import jaeik.growfarm.entity.user.Token;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.JwtHandler;
import jaeik.growfarm.global.auth.AuthCookieManager;
import jaeik.growfarm.global.event.UserBannedEvent;
import jaeik.growfarm.repository.comment.CommentRepository;
import jaeik.growfarm.repository.notification.FcmTokenRepository;
import jaeik.growfarm.repository.token.TokenRepository;
import jaeik.growfarm.repository.user.BlackListRepository;
import jaeik.growfarm.repository.user.SettingRepository;
import jaeik.growfarm.repository.user.UserJdbcRepository;
import jaeik.growfarm.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.util.List;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;

@Service
@RequiredArgsConstructor
public class AuthUpdateServiceImpl implements AuthUpdateService {

    private final TokenRepository tokenRepository;
    private final JwtHandler jwtHandler;
    private final AuthCookieManager authCookieManager;
    private final TempUserDataManager tempUserDataManager;
    private final UserRepository userRepository;
    private final SettingRepository settingRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final UserJdbcRepository userJdbcRepository;
    private final CommentRepository commentRepository;
    private final EntityManager entityManager;
    private final BlackListRepository blackListRepository;

    @EventListener
    @Transactional
    public void handleUserBannedEvent(UserBannedEvent event) {
        BlackList blackList = BlackList.createBlackList(event.getSocialId(), event.getProvider());
        blackListRepository.save(blackList);
    }

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
        tempUserDataManager.removeTempData(uuid);
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
        commentRepository.anonymizeUserComments(userId);

        entityManager.flush();
        entityManager.clear();

        userJdbcRepository.deleteAllTokensByUserId(userId);
        fcmTokenRepository.deleteByUsers_Id(userId);

        userRepository.deleteById(userId);
    }
}
