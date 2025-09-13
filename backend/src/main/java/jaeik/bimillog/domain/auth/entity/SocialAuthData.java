package jaeik.bimillog.domain.auth.entity;

import jaeik.bimillog.domain.auth.application.service.SocialService;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.Token;

/**
 * <h2>소셜 인증 데이터</h2>
 * <p>소셜 로그인 과정에서 사용되는 인증 관련 데이터를 담는 도메인 엔티티입니다.</p>
 * <p>소셜 사용자 프로필, 임시 사용자 데이터</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public class SocialAuthData {

    /**
     * <h3>소셜 사용자 프로필</h3>
     * <p>소셜 플랫폼에서 받아온 사용자 프로필 정보를 담는 도메인 값 객체입니다.</p>
     * <p>외부 소셜 플랫폼의 API 응답을 도메인 계층에서 사용할 수 있는 순수한 형태로 변환한 모델입니다.</p>
     * <p>카카오, 구글 등 플랫폼별 차이점을 추상화하여 통일된 인터페이스를 제공합니다.</p>
     *
     * @param socialId 소셜 플랫폼에서의 사용자 고유 ID
     * @param email 사용자 이메일 주소
     * @param provider 소셜 플랫폼 제공자 (KAKAO, GOOGLE 등)
     * @param nickname 소셜 플랫폼에서의 사용자 닉네임
     * @param profileImageUrl 프로필 이미지 URL (선택사항)
     * @author Jaeik
     * @since 2.0.0
     */
    public record SocialUserProfile(
            String socialId, 
            String email, 
            SocialProvider provider,
            String nickname, 
            String profileImageUrl
    ) {}

    /**
     * <h3>인증 결과 데이터</h3>
     * <p>소셜 플랫폼 OAuth 인증 완료 후 반환되는 결과 데이터입니다.</p>
     * <p>소셜 사용자 프로필과 토큰 정보를 포함하여 인증 플로우에서 사용됩니다.</p>
     * <p>{@link jaeik.bimillog.domain.auth.application.port.out.SocialStrategyPort}의 authenticate 메서드 반환값으로 사용됩니다.</p>
     *
     * @param userProfile 소셜 플랫폼에서 받은 사용자 프로필 정보
     * @param token 소셜 로그인으로 발급받은 토큰 정보
     * @author Jaeik
     * @since 2.0.0
     */
    public record AuthenticationResult(
            SocialUserProfile userProfile,
            Token token
    ) {}

    /**
     * <h3>임시 사용자 데이터</h3>
     * <p>소셜 로그인 성공 후 회원가입 완료까지 임시 저장되는 사용자 데이터입니다.</p>
     * <p>Redis에 UUID 키로 저장되어 회원가입 페이지에서 사용자 이름 입력 완료 시 활용됩니다.</p>
     * <p>신규 사용자의 회원가입 프로세스에서 소셜 인증 정보를 안전하게 전달하기 위한 임시 컨테이너입니다.</p>
     *
     * @param userProfile 소셜 플랫폼에서 받은 사용자 프로필 정보
     * @param token 소셜 로그인으로 발급받은 토큰 정보
     * @param fcmToken 푸시 알림용 Firebase Cloud Messaging 토큰 (선택사항)
     *
     * @author Jaeik
     * @version 2.0.0
     */
    public record TempUserData(
            SocialUserProfile userProfile,
            Token token,
            String fcmToken
    ) {}
}