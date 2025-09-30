package jaeik.bimillog.domain.auth.application.port.out;

import jaeik.bimillog.domain.auth.application.service.SocialLoginService;
import jaeik.bimillog.infrastructure.adapter.out.api.dto.SocialUserProfileDTO;
import jaeik.bimillog.domain.user.entity.user.SocialProvider;
import jaeik.bimillog.domain.user.entity.userdetail.UserDetail;

/**
 * <h2>인증 TO 유저 포트</h2>
 * <p>인증 도메인에서 유저 도메인에 접근하는 포트입니다.</p>
 * <p>로그인 시 사용자 데이터를 담당합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface AuthToUserPort {

    /**
     * <h3>로그인 시 사용자 데이터 처리</h3>
     * <p>소셜 제공자와 프로필 정보를 기반으로 사용자 데이터를 처리합니다.</p>
     * <p>기존 회원이면 로그인 처리를, 신규 회원이면 임시 데이터 저장을 수행합니다.</p>
     * <p>{@link SocialLoginService}에서 소셜 로그인 처리 중 사용자 데이터 처리 시 호출됩니다.</p>
     *
     * @param provider 소셜 로그인 제공자 (KAKAO 등)
     * @param profile 소셜 사용자 프로필 정보
     * @param fcmToken FCM 토큰 (선택사항)
     * @return LoginResult 기존 사용자(쿠키) 또는 신규 사용자(UUID) 정보
     * @author Jaeik
     * @since 2.0.0
     */
    UserDetail delegateUserData(SocialProvider provider, SocialUserProfileDTO profile, String fcmToken);

}