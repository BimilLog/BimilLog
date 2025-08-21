package jaeik.growfarm.infrastructure.adapter.auth.out.persistence.auth;

import jaeik.growfarm.domain.auth.application.port.out.SaveUserPort;
import jaeik.growfarm.domain.notification.application.port.in.NotificationFcmUseCase;
import jaeik.growfarm.domain.user.application.port.in.UserCommandUseCase;
import jaeik.growfarm.domain.user.application.port.in.UserQueryUseCase;
import jaeik.growfarm.domain.user.entity.Setting;
import jaeik.growfarm.domain.user.entity.Token;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.infrastructure.adapter.auth.out.social.dto.SocialLoginUserData;
import jaeik.growfarm.infrastructure.adapter.user.in.web.dto.TokenDTO;
import jaeik.growfarm.infrastructure.adapter.user.in.web.dto.UserDTO;
import jaeik.growfarm.infrastructure.adapter.user.out.persistence.user.token.TokenRepository;
import jaeik.growfarm.infrastructure.auth.AuthCookieManager;
import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
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
public class SaveUserAdapter implements SaveUserPort {

    private final TokenRepository tokenRepository;
    private final AuthCookieManager authCookieManager;
    private final UserQueryUseCase userQueryUseCase;
    private final UserCommandUseCase userCommandUseCase;
    private final TempDataAdapter tempDataAdapter;
    private final NotificationFcmUseCase notificationFcmUseCase;

    /**
     * <h3>기존 사용자 로그인 처리</h3>
     * <p>소셜 로그인 사용자 정보를 업데이트하고, FCM 토큰을 등록합니다.</p>
     *
     * @param userData  소셜 로그인 사용자 데이터
     * @param tokenDTO  토큰 정보
     * @param fcmToken  FCM 토큰 (선택적)
     * @return ResponseCookie 리스트
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    @Transactional
    public List<ResponseCookie> handleExistingUserLogin(SocialLoginUserData userData, TokenDTO tokenDTO, String fcmToken) { // fcmToken 인자 추가
        User user = userQueryUseCase.findByProviderAndSocialId(userData.provider(), userData.socialId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        user.updateUserInfo(userData.nickname(), userData.profileImageUrl());

        Token newToken = Token.builder()
                .accessToken(tokenDTO.accessToken())
                .refreshToken(tokenDTO.refreshToken())
                .users(user)
                .build();

        if (fcmToken != null && !fcmToken.isEmpty()) {
            notificationFcmUseCase.registerFcmToken(user.getId(), fcmToken);
        }

        return authCookieManager.generateJwtCookie(UserDTO.of(user,
                tokenRepository.save(newToken).getId(),
                null));
    }

    /**
     * <h3>신규 사용자 정보 저장</h3>
     * <p>임시 UUID를 사용하여 새로운 사용자를 등록하고, FCM 토큰이 존재하면 이벤트를 발행합니다.</p>
     *
     * @param userName  사용자의 이름
     * @param uuid      임시 UUID
     * @param userData  소셜 로그인 사용자 데이터
     * @param tokenDTO  토큰 정보
     * @param fcmToken  FCM 토큰 (선택적)
     * @return ResponseCookie 리스트
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    @Transactional
    public List<ResponseCookie> saveNewUser(String userName, String uuid, SocialLoginUserData userData, TokenDTO tokenDTO, String fcmToken) { // fcmToken 인자 추가
        Setting setting = Setting.createSetting();
        
        User user = userCommandUseCase.save(User.createUser(userData, userName, setting));

        if (fcmToken != null && !fcmToken.isEmpty()) {
            notificationFcmUseCase.registerFcmToken(user.getId(), fcmToken);
        }

        tempDataAdapter.removeTempData(uuid);
        return authCookieManager.generateJwtCookie(UserDTO.of(user,
                tokenRepository.save(Token.createToken(tokenDTO, user)).getId(),
                null));
    }
}