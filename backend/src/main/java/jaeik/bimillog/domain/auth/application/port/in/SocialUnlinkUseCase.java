package jaeik.bimillog.domain.auth.application.port.in;

import jaeik.bimillog.domain.user.entity.SocialProvider;

/**
 * <h2>소셜 연결 해제 유스케이스</h2>
 * <p>사용자 차단 시 소셜 로그인 연결 해제와 관련된 비즈니스 로직을 처리하는 유스케이스 인터페이스</p>
 * <p>사용자가 차단되었을 때 소셜 플랫폼과의 연결을 해제합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface SocialUnlinkUseCase {

    /**
     * <h3>소셜 로그인 연결 해제</h3>
     * <p>사용자 차단 시 소셜 플랫폼과의 연결을 해제합니다.</p>
     * <p>해제 실패 시에도 사용자 차단 프로세스는 계속 진행됩니다.</p>
     *
     * @param provider 소셜 제공자 (KAKAO 등)
     * @param socialId 소셜 플랫폼에서의 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void unlinkSocialAccount(SocialProvider provider, String socialId);
}