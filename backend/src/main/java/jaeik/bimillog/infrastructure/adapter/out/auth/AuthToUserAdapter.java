package jaeik.bimillog.infrastructure.adapter.out.auth;

import jaeik.bimillog.domain.auth.application.port.out.AuthToUserPort;
import jaeik.bimillog.domain.auth.application.service.SocialService;
import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.domain.auth.entity.SocialUserProfile;
import jaeik.bimillog.domain.user.application.port.in.UserSaveUseCase;
import jaeik.bimillog.domain.user.entity.ExistedUserDetail;
import jaeik.bimillog.domain.user.entity.NewUserDetail;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.UserDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <h2>소셜 사용자 관리 어댑터</h2>
 * <p>소셜 로그인 사용자의 정보 조회 및 검증을 담당하는 어댑터입니다.</p>
 * <p>기존 사용자 확인, 블랙리스트 조회 등 사용자 검증 로직 처리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class AuthToUserAdapter implements AuthToUserPort {

    private final UserSaveUseCase userSaveUseCase;
    private final AuthCookieManager authCookieManager;

    /**
     * <h3>기존 소셜 사용자 조회</h3>
     * <p>소셜 제공자와 소셜 ID를 기반으로 기존 사용자를 조회합니다.</p>
     * <p>사용자가 존재하지 않는 경우 빈 Optional을 반환합니다.</p>
     * <p>{@link SocialService}에서 기존 회원 확인 시 호출됩니다.</p>
     *
     * @param provider 소셜 로그인 제공자 (예: KAKAO 등)
     * @param socialId 소셜 플랫폼에서의 사용자 고유 ID
     * @return Optional로 감싼 기존 사용자 정보
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional(readOnly = true)
    public LoginResult userDataProcess(SocialProvider provider, SocialUserProfile profile, String fcmToken) {
        UserDetail userDetail = userSaveUseCase.saveUserData(provider, profile, fcmToken);
        if (userDetail instanceof ExistedUserDetail) {
            List<ResponseCookie> cookies = authCookieManager.generateJwtCookie((ExistedUserDetail) userDetail);
            return new LoginResult.ExistingUser(cookies);
        } else {
           ResponseCookie tempCookie = authCookieManager.createTempCookie((NewUserDetail) userDetail);
           return new LoginResult.NewUser(((NewUserDetail) userDetail).getUuid(), tempCookie);
        }
    }
}