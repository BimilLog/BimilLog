
package jaeik.bimillog.domain.auth.application.port.in;

import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.domain.member.entity.member.SocialProvider;

/**
 * <h2>소셜 로그인 유스케이스</h2>
 * <p>소셜 플랫폼과의 인증 및 연동 작업을 처리하는 유스케이스입니다.</p>
 * <p>소셜 로그인 관리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface SocialLoginUseCase {

    /**
     * <h3>소셜 로그인 처리</h3>
     * <p>소셜 플랫폼의 인가 코드를 사용하여 로그인 처리를 수행합니다.</p>
     * <p>기존 회원은 즉시 로그인 처리하고, 신규 회원은 임시 데이터를 생성하여 회원가입 단계로 안내합니다.</p>
     *
     * @param provider 소셜 로그인 제공자 (KAKAO 등)
     * @param code 소셜 플랫폼에서 발급한 인가 코드
     * @param fcmToken Firebase Cloud Messaging 토큰 (푸시 알림용, 선택적)
     * @return 로그인 결과 (기존 회원: 쿠키 포함, 신규 회원: 임시 UUID 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    LoginResult processSocialLogin(SocialProvider provider, String code, String fcmToken);
}
