package jaeik.bimillog.domain.member.adapter;

import jaeik.bimillog.domain.auth.entity.AuthToken;
import jaeik.bimillog.domain.auth.entity.SocialToken;
import jaeik.bimillog.domain.auth.service.AuthTokenService;
import jaeik.bimillog.domain.auth.service.SocialTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
/**
 * <h2>사용자-인증 도메인 연결 어댑터</h2>
 * <p>Member 도메인과 Auth도메인을 연결하는 어댑터입니다.</p>
 *
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class MemberToAuthAdapter {
    private final SocialTokenService socialTokenService;
    private final AuthTokenService authTokenService;

    /**
     * <h3>멤버 ID로 소셜토큰 조회</h3>
     * <p>카카오톡 친구 조회시 호출</p>
     */
    public Optional<SocialToken> getSocialToken(Long memberId) {
        return socialTokenService.getSocialToken(memberId);
    }

    /**
     * <h3>세션토큰 저장</h3>
     * <p>신규회원 가입 시 호출</p>
     */
    public AuthToken saveAuthToken(AuthToken authToken) {
        return authTokenService.save(authToken);
    }

    /**
     * <h3>소셜토큰 저장</h3>
     * <p>신규회원 가입 시 호출</p>
     */
    public SocialToken saveSocialToken(SocialToken socialToken) {
        return socialTokenService.save(socialToken);
    }
}
