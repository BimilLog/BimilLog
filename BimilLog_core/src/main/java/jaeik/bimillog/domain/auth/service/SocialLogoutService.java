package jaeik.bimillog.domain.auth.service;

import jaeik.bimillog.domain.admin.event.MemberBannedEvent;
import jaeik.bimillog.domain.auth.entity.SocialToken;
import jaeik.bimillog.domain.auth.adapter.SocialStrategyAdapter;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.domain.member.event.MemberWithdrawnEvent;
import jaeik.bimillog.infrastructure.api.social.SocialStrategy;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * <h2>소셜 로그아웃, 연결해제</h2>
 * <p>사용자의 로그아웃 처리와 소셜 플랫폼 연동 해제를 담당.</p>
 * <p>로그아웃은 없음 탈퇴시 연결해제와 징계시 강제 로그아웃만이 존재</p>
 * <p>모든 로그아웃은 서비스 로그아웃으로 해석</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SocialLogoutService {
    private final SocialStrategyAdapter socialStrategyAdapter;
    private final SocialTokenService socialTokenService;

    /**
     * <h3>소셜 로그인 강제 로그아웃</h3>
     * <p>관리자에 의한 강제 로그아웃 처리입니다.</p>
     *
     * @param socialId 소셜 플랫폼 사용자 ID
     * @param provider 소셜 플랫폼 제공자
     */
    public void forceLogout(String socialId, SocialProvider provider) {
        SocialStrategy strategy = socialStrategyAdapter.getStrategy(provider);
        strategy.forceLogout(socialId);
    }

    /**
     * <h3>소셜 계정 연동 해제</h3>
     * <p>사용자의 소셜 플랫폼 계정 연동을 해제합니다.</p>
     * <p>소셜 플랫폼 API를 호출하여 앱 연동을 완전히 차단합니다.</p>
     * <p>{@link MemberWithdrawnEvent}, {@link MemberBannedEvent} 이벤트 발생 시 소셜 계정 정리를 위해 호출됩니다.</p>
     *
     * @param provider 연동 해제할 소셜 플랫폼 제공자
     * @param socialId 소셜 플랫폼에서의 사용자 고유 ID
     * @param memberId 사용자 ID (소셜 토큰 조회용)
     */
    public void unlinkSocialAccount(SocialProvider provider, String socialId, Long memberId) {
        log.info("소셜 연결 해제 시작 - 제공자: {}, 소셜 ID: {}, 회원 ID: {}", provider, socialId, memberId);
        Optional<SocialToken> socialToken = socialTokenService.getSocialToken(memberId);
        String accessToken = socialToken.map(SocialToken::getAccessToken).orElse(null);

        SocialStrategy strategy = socialStrategyAdapter.getStrategy(provider);
        strategy.unlink(socialId, accessToken);
        log.info("소셜 연결 해제 완료 - 제공자: {}, 소셜 ID: {}", provider, socialId);
    }
}
