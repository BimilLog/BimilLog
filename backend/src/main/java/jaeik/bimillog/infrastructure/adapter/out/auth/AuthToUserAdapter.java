package jaeik.bimillog.infrastructure.adapter.out.auth;

import jaeik.bimillog.domain.auth.application.port.out.AuthToUserPort;
import jaeik.bimillog.domain.auth.application.service.SocialService;
import jaeik.bimillog.domain.auth.entity.SocialUserProfile;
import jaeik.bimillog.domain.user.application.port.in.UserSaveUseCase;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.UserDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <h2>인증-사용자 도메인 연결 어댑터</h2>
 * <p>Auth 도메인과 User 도메인을 연결하는 아웃바운드 어댑터입니다.</p>
 * <p>인증 도메인에서 사용자 도메인의 기능을 호출하는 중개 역할을 수행합니다.</p>
 *
 * <h3>주요 책임:</h3>
 * <ul>
 *   <li>Auth 도메인의 사용자 처리 요청을 User 도메인으로 전달</li>
 *   <li>User 도메인의 처리 결과를 LoginResult로 변환</li>
 *   <li>기존 사용자에 대한 JWT 쿠키 생성</li>
 *   <li>신규 사용자에 대한 임시 쿠키 생성</li>
 * </ul>
 *
 * <p><b>도메인 경계:</b> Auth 도메인의 포트 구현체로서 User 도메인과의 통신을 담당</p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2025-01
 */
@Component
@RequiredArgsConstructor
public class AuthToUserAdapter implements AuthToUserPort {

    private final UserSaveUseCase userSaveUseCase;

    /**
     * <h3>사용자 데이터 처리 위임</h3>
     * <p>User 도메인에 사용자 처리를 위임하고 그 결과를 반환합니다.</p>
     * <p>기존 사용자 또는 신규 사용자 정보를 UserDetail 형태로 반환합니다.</p>
     * <p>{@link SocialService}에서 소셜 로그인 처리 시 호출됩니다.</p>
     *
     * @param provider 소셜 로그인 제공자 (KAKAO 등)
     * @param profile 소셜 사용자 프로필 정보
     * @param fcmToken FCM 토큰 (선택사항)
     * @return UserDetail 기존 사용자 또는 신규 사용자 정보
     * @author Jaeik
     * @since 2.0.0
     */
    public UserDetail delegateUserData(SocialProvider provider, SocialUserProfile profile, String fcmToken) {
        return userSaveUseCase.processUserData(provider, profile, fcmToken);
    }
}