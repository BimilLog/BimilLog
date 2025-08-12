package jaeik.growfarm.domain.auth.infrastructure.adapter.out;

import jaeik.growfarm.domain.auth.application.port.out.ManageAuthDataPort;
import jaeik.growfarm.domain.auth.infrastructure.adapter.out.persistence.TokenRepository;
import jaeik.growfarm.domain.user.entity.Token;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.user.infrastructure.adapter.out.persistence.UserRepository;
import jaeik.growfarm.dto.auth.SocialLoginUserData;
import jaeik.growfarm.dto.user.ClientDTO;
import jaeik.growfarm.dto.user.TokenDTO;
import jaeik.growfarm.infrastructure.auth.AuthCookieManager;
import jaeik.growfarm.global.event.UserLoggedOutEvent;
import jaeik.growfarm.global.event.UserSignedUpEvent;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    private final UserRepository userRepository;
    private final EntityManager entityManager;
    private final TempDataAdapter tempDataAdapter;
    private final ApplicationEventPublisher eventPublisher;


    @Override
    @Transactional
    public List<ResponseCookie> saveExistUser(User user, SocialLoginUserData userData, TokenDTO tokenDTO) {
        user.updateUserInfo(userData.getNickname(), userData.getProfileImageUrl());

        Token token = tokenRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FIND_TOKEN));
        token.updateToken(tokenDTO.getAccessToken(), tokenDTO.getRefreshToken());

        return authCookieManager.generateJwtCookie(ClientDTO.of(user,
                tokenRepository.save(token).getId(),
                null)); // fcmTokenId는 null로 설정
    }

    @Override
    @Transactional
    public List<ResponseCookie> saveNewUser(String userName, String uuid, SocialLoginUserData userData, TokenDTO tokenDTO) {
        User user = userRepository.save(User.createUser(userData, userName));
        eventPublisher.publishEvent(new UserSignedUpEvent(user.getId()));
        tempDataAdapter.removeTempData(uuid);
        return authCookieManager.generateJwtCookie(ClientDTO.of(user,
                tokenRepository.save(Token.createToken(tokenDTO, user)).getId(),
                null)); // fcmTokenId는 null로 설정
    }

    @Override
    @Transactional
    public void logoutUser(Long userId) {
        tokenRepository.deleteAllByUserId(userId);
        // FCM 토큰 삭제는 이벤트를 통해 notification 도메인에서 처리
        eventPublisher.publishEvent(UserLoggedOutEvent.of(userId, null));
    }

    @Override
    @Transactional
    public void performWithdrawProcess(Long userId) {
        entityManager.flush();
        entityManager.clear();

        tokenRepository.deleteAllByUserId(userId);
        // FCM 토큰 삭제는 이벤트를 통해 notification 도메인에서 처리
        // 회원 탈퇴 이벤트는 UserAccountService에서 발행

        userRepository.deleteById(userId);
    }

    @Override
    public Optional<Token> findTokenByUserId(Long userId) {
        return tokenRepository.findByUsers_Id(userId);
    }
}