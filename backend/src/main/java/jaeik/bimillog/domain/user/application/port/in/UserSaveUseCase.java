package jaeik.bimillog.domain.user.application.port.in;

import jaeik.bimillog.domain.auth.entity.SocialUserProfile;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.UserDetail;
import jaeik.bimillog.infrastructure.adapter.out.auth.AuthToUserAdapter;

public interface UserSaveUseCase {

    /**
     * <h3>사용자 데이터 처리</h3>
     * <p>소셜 로그인 정보를 바탕으로 사용자 데이터를 처리합니다.</p>
     * <p>기존 사용자는 정보를 업데이트하고, 신규 사용자는 임시 데이터를 저장합니다.</p>
     * <p>{@link AuthToUserAdapter}에서 Auth 도메인의 요청을 받아 호출됩니다.</p>
     *
     * @param provider 소셜 로그인 제공자 (KAKAO 등)
     * @param profile 소셜 사용자 프로필 정보
     * @param fcmToken FCM 토큰 (선택사항)
     * @return UserDetail 기존 사용자(ExistingUserDetail) 또는 신규 사용자(NewUserDetail) 정보
     * @author Jaeik
     * @since 2.0.0
     */
    UserDetail processUserData(SocialProvider provider, SocialUserProfile profile, String fcmToken);
}
