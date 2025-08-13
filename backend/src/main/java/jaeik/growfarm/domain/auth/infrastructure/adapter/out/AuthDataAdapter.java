package jaeik.growfarm.domain.auth.infrastructure.adapter.out;

import jaeik.growfarm.domain.auth.application.port.out.ManageAuthDataPort;
import jaeik.growfarm.domain.user.infrastructure.adapter.out.persistence.TokenRepository;
import jaeik.growfarm.domain.user.entity.Token;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.user.application.port.in.UserQueryUseCase;
import jaeik.growfarm.domain.user.application.port.out.UserPort;
import jaeik.growfarm.dto.auth.SocialLoginUserData;
import jaeik.growfarm.dto.user.ClientDTO;
import jaeik.growfarm.dto.user.TokenDTO;
import jaeik.growfarm.infrastructure.auth.AuthCookieManager;
import jaeik.growfarm.global.event.UserLoggedOutEvent;
import jaeik.growfarm.global.event.UserSignedUpEvent;
import jaeik.growfarm.global.event.FcmTokenRegisteredEvent;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import jaeik.growfarm.global.domain.SocialProvider;

import java.util.List;
import java.util.Optional;

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
    private final UserQueryUseCase userQueryUseCase;
    private final UserPort userPort;
    private final EntityManager entityManager;
    private final TempDataAdapter tempDataAdapter;
    private final ApplicationEventPublisher eventPublisher;

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

        Token token = tokenRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FIND_TOKEN));
        token.updateToken(tokenDTO.accessToken(), tokenDTO.refreshToken());

        // FCM 토큰 등록 이벤트 발행
        if (fcmToken != null && !fcmToken.isEmpty()) {
            eventPublisher.publishEvent(new FcmTokenRegisteredEvent(user.getId(), fcmToken));
        }

        return authCookieManager.generateJwtCookie(ClientDTO.of(user,
                tokenRepository.save(token).getId(),
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
        User user = userPort.save(User.createUser(userData, userName));
        eventPublisher.publishEvent(new UserSignedUpEvent(user.getId()));

        // 신규 사용자 FCM 토큰 등록 이벤트 발행
        if (fcmToken != null && !fcmToken.isEmpty()) {
            eventPublisher.publishEvent(new FcmTokenRegisteredEvent(user.getId(), fcmToken));
        }

        tempDataAdapter.removeTempData(uuid);
        return authCookieManager.generateJwtCookie(ClientDTO.of(user,
                tokenRepository.save(Token.createToken(tokenDTO, user)).getId(),
                null));
    }

    /**
     * <h3>로그아웃 처리</h3>
     * <p>사용자를 로그아웃하고, 소셜 로그아웃을 수행하며, 이벤트를 발행합니다.</p>
     *
     * @param userId 사용자 ID
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    @Transactional
    public void logoutUser(Long userId) {
        tokenRepository.deleteAllByUserId(userId);
        eventPublisher.publishEvent(UserLoggedOutEvent.of(userId, null));
    }

    /**
     * <h3>회원 탈퇴 처리</h3>
     * <p>사용자를 탈퇴시키고, 소셜 로그아웃을 수행하며, 이벤트를 발행합니다.</p>
     *
     * @param userId 사용자 ID
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    @Transactional
    public void performWithdrawProcess(Long userId) {
        entityManager.flush();
        entityManager.clear();

        tokenRepository.deleteAllByUserId(userId);

        userPort.deleteById(userId);
    }

}