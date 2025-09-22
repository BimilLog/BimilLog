package jaeik.bimillog.domain.user.application.port.in;

import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.domain.auth.entity.SocialUserProfile;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.infrastructure.adapter.out.api.social.SocialAdapter;
import jaeik.bimillog.infrastructure.adapter.out.user.SaveUserAdapter;

public interface UserSaveUseCase {

    /**
     * <h3>유저 정보 저장</h3>
     * <p>제공자(Provider)와 소셜 ID를 사용하여 사용자를 조회합니다.</p>
     * <p>{@link SocialAdapter}, {@link SaveUserAdapter}에서 소셜 로그인 처리 시 호출됩니다.</p>
     *
     * @param provider 소셜 로그인 제공자
     * @param socialId 사용자의 소셜 ID
     * @return Optional<User> 조회된 사용자 객체. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    LoginResult saveUserData(SocialProvider provider, SocialUserProfile profile, String fcmToken);
}
