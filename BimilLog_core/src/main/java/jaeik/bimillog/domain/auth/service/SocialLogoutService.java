package jaeik.bimillog.domain.auth.service;

import jaeik.bimillog.domain.auth.entity.SocialToken;
import jaeik.bimillog.domain.global.out.GlobalMemberQueryAdapter;
import jaeik.bimillog.domain.auth.out.SocialStrategyAdapter;
import jaeik.bimillog.domain.global.strategy.SocialPlatformStrategy;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <h2>로그아웃 서비스</h2>
 * <p>사용자의 로그아웃 처리와 소셜 플랫폼 연동 해제를 담당하는 서비스입니다.</p>
 * <p>JWT 토큰 무효화, 소셜 플랫폼 로그아웃, 이벤트 발행</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SocialLogoutService {
    private final SocialStrategyAdapter socialStrategyAdapter;
    private final GlobalMemberQueryAdapter globalMemberQueryAdapter;


    /**
     * <h3>소셜 플랫폼 로그아웃</h3>
     * <p>사용자의 소셜 플랫폼 세션을 로그아웃 처리합니다.</p>
     * <p>Member를 통해 소셜 토큰을 조회하여 소셜 플랫폼 API를 호출합니다.</p>
     *
     * @param memberId 회원 ID
     * @param provider 소셜 플랫폼 제공자
     * @throws Exception 소셜 플랫폼 로그아웃 처리 중 예외 발생 시
     * @author Jaeik
     * @since 2.0.0
     */
    public void socialLogout(Long memberId, SocialProvider provider) throws Exception {
        Member member = globalMemberQueryAdapter.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_NOT_FIND_TOKEN));
        SocialToken socialToken = member.getSocialToken();
        if (socialToken == null) {
            throw new CustomException(ErrorCode.AUTH_NOT_FIND_TOKEN);
        }
        SocialPlatformStrategy strategy = socialStrategyAdapter.getStrategy(provider);
        strategy.auth().logout(socialToken.getAccessToken());
    }

    /**
     * <h3>강제 로그아웃</h3>
     * <p>관리자에 의한 강제 로그아웃 처리입니다.</p>
     *
     * @param socialId 소셜 플랫폼 사용자 ID
     * @param provider 소셜 플랫폼 제공자
     * @author Jaeik
     * @since 2.0.0
     */
    public void forceLogout(String socialId, SocialProvider provider) {
        SocialPlatformStrategy strategy = socialStrategyAdapter.getStrategy(provider);
        strategy.auth().forceLogout(socialId);
    }
}
