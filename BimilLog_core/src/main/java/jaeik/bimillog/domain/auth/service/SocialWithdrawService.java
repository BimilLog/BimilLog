package jaeik.bimillog.domain.auth.service;

import jaeik.bimillog.domain.admin.event.MemberBannedEvent;
import jaeik.bimillog.domain.auth.entity.SocialToken;
import jaeik.bimillog.domain.global.out.GlobalMemberQueryAdapter;
import jaeik.bimillog.domain.auth.out.SocialStrategyAdapter;
import jaeik.bimillog.infrastructure.api.social.SocialStrategy;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.domain.member.event.MemberWithdrawnEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SocialWithdrawService {

    private final SocialStrategyAdapter socialStrategyAdapter;
    private final GlobalMemberQueryAdapter globalMemberQueryAdapter;


    /**
     * <h3>소셜 계정 연동 해제</h3>
     * <p>사용자의 소셜 플랫폼 계정 연동을 해제합니다.</p>
     * <p>소셜 플랫폼 API를 호출하여 앱 연동을 완전히 차단합니다.</p>
     * <p>{@link MemberWithdrawnEvent}, {@link MemberBannedEvent} 이벤트 발생 시 소셜 계정 정리를 위해 호출됩니다.</p>
     *
     * @param provider 연동 해제할 소셜 플랫폼 제공자
     * @param socialId 소셜 플랫폼에서의 사용자 고유 ID
     * @param memberId 사용자 ID (소셜 토큰 조회용)
     * @author Jaeik
     * @since 2.0.0
     */
    public void unlinkSocialAccount(SocialProvider provider, String socialId, Long memberId) {
        log.info("소셜 연결 해제 시작 - 제공자: {}, 소셜 ID: {}, 회원 ID: {}", provider, socialId, memberId);

        // Member 조회 및 accessToken 추출
        Member member = globalMemberQueryAdapter.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다: " + memberId));
        SocialToken socialToken = member.getSocialToken();
        String accessToken = socialToken != null ? socialToken.getAccessToken() : null;

        SocialStrategy strategy = socialStrategyAdapter.getStrategy(provider);
        strategy.unlink(socialId, accessToken);

        log.info("소셜 연결 해제 완료 - 제공자: {}, 소셜 ID: {}", provider, socialId);
    }
}
