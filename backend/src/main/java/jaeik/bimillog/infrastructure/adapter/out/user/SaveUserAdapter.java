package jaeik.bimillog.infrastructure.adapter.out.user;

import jaeik.bimillog.domain.auth.application.port.out.TokenCommandPort;
import jaeik.bimillog.infrastructure.adapter.out.api.dto.SocialUserProfileDTO;
import jaeik.bimillog.domain.auth.entity.Token;
import jaeik.bimillog.domain.notification.application.port.in.FcmUseCase;
import jaeik.bimillog.domain.user.application.port.out.SaveUserPort;
import jaeik.bimillog.domain.user.entity.userdetail.ExistingUserDetail;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>사용자 저장 어댑터</h2>
 * <p>User 도메인의 아웃바운드 어댑터로 사용자 데이터 영속성을 담당합니다.</p>
 * <p>소셜 로그인 사용자의 실제 저장 로직과 관련 엔티티 처리를 수행합니다.</p>
 *
 * <h3>주요 책임:</h3>
 * <ul>
 *   <li>기존 사용자: 프로필 업데이트, Token 엔티티 생성, FCM 토큰 등록</li>
 *   <li>신규 사용자: User/Setting 엔티티 생성, 임시 데이터 삭제, JWT 쿠키 발급</li>
 *   <li>FCM 토큰 관리 및 NotificationFcmUseCase와 통합</li>
 * </ul>
 *
 * <p><b>도메인 분리:</b> Auth 도메인에서 User 도메인으로 이동되어 사용자 데이터 저장 책임만 담당</p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2025-01
 */
@Component
@RequiredArgsConstructor
public class SaveUserAdapter implements SaveUserPort {

    private final TokenCommandPort tokenCommandPort;
    private final UserRepository userRepository;
    private final FcmUseCase fcmUseCase;

    /**
     * <h3>기존 사용자 로그인 처리</h3>
     * <p>기존 회원의 소셜 로그인 시 사용자 정보 업데이트와 JWT 쿠키 발급을 처리합니다.</p>
     * <p>프로필 정보 동기화, 새로운 Token 엔티티 생성/저장, FCM 토큰 등록, JWT 쿠키 발급을 수행합니다.</p>
     *
     * @param userProfile 소셜 사용자 프로필 (OAuth 액세스/리프레시 토큰 포함)
     * @param fcmToken FCM 토큰 (선택적)
     * @return JWT 인증 쿠키 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public ExistingUserDetail handleExistingUserData(User existingUser, SocialUserProfileDTO userProfile, String fcmToken) {
        existingUser.updateUserInfo(userProfile.getNickname(), userProfile.getProfileImageUrl());

        Token newToken = Token.createToken(userProfile.getKakaoAccessToken(), userProfile.getKakaoRefreshToken(), existingUser);
        Long fcmTokenId = registerFcmTokenIfPresent(existingUser, fcmToken);
        Long tokenId = tokenCommandPort.save(newToken).getId();

        return ExistingUserDetail.of(existingUser, tokenId, fcmTokenId);
    }

    /**
     * <h3>신규 사용자 등록</h3>
     * <p>소셜 로그인 회원가입에서 입력받은 닉네임과 임시 데이터를 사용하여 신규 회원을 등록합니다.</p>
     * <p>User 엔티티와 Setting 생성, Token 엔티티 생성/저장, FCM 토큰 등록을 수행합니다.</p>
     *
     * @param userName 사용자가 입력한 닉네임
     * @param userProfile 소셜 사용자 프로필 (OAuth 액세스/리프레시 토큰 포함)
     * @param fcmToken FCM 토큰 (선택적)
     * @return ExistingUserDetail 생성된 사용자 정보를 담은 객체
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public ExistingUserDetail saveNewUser(String userName, SocialUserProfileDTO userProfile, String fcmToken) {
        Setting setting = Setting.createSetting();
        User user = userRepository.save(User.createUser(userProfile.getSocialId(), userProfile.getProvider(), userProfile.getNickname(), userProfile.getProfileImageUrl(), userName, setting));
        Long fcmTokenId = registerFcmTokenIfPresent(user, fcmToken);
        Token newToken = Token.createToken(userProfile.getKakaoAccessToken(), userProfile.getKakaoRefreshToken(), user);
        Long tokenId = tokenCommandPort.save(newToken).getId();

        return ExistingUserDetail.of(user, tokenId, fcmTokenId);
    }

    /**
     * <h3>FCM 토큰 등록 처리</h3>
     * <p>FCM 토큰이 존재할 경우에만 알림 서비스에 등록합니다.</p>
     * <p>{@link #handleExistingUserData}, {@link #saveNewUser} 메서드에서 FCM 토큰 등록을 위해 호출됩니다.</p>
     *
     * @param user 사용자
     * @param fcmToken FCM 토큰 (빈 문자열이나 null인 경우 무시)
     * @return 저장된 FCM 토큰 ID (토큰이 없거나 빈 값인 경우 null)
     * @author Jaeik
     * @since 2.0.0
     */
    private Long registerFcmTokenIfPresent(User user, String fcmToken) {
        if (fcmToken != null && !fcmToken.isEmpty()) {
            return fcmUseCase.registerFcmToken(user, fcmToken);
        }
        return null;
    }
}