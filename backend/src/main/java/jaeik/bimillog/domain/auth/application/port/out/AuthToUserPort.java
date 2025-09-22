package jaeik.bimillog.domain.auth.application.port.out;

import jaeik.bimillog.domain.auth.application.service.SocialService;
import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.domain.auth.entity.SocialUserProfile;
import jaeik.bimillog.domain.user.entity.SocialProvider;

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
     * <h3>사용자 데이터 처리</h3>
     * <p>소셜 제공자와 소셜 ID를 기반으로 기존 사용자를 조회합니다.</p>
     * <p>소셜 로그인 시 기존 회원 여부를 판단하기 위해 사용됩니다.</p>
     * <p>{@link SocialService}에서 소셜 로그인 처리 중 기존 사용자 확인 시 호출됩니다.</p>
     *
     * @param provider 소셜 로그인 제공자 (KAKAO 등)
     * @return Optional로 감싼 기존 사용자 정보
     * @author Jaeik
     * @since 2.0.0
     */
    LoginResult userDataProcess(SocialProvider provider, SocialUserProfile profile, String fcmToken);

}