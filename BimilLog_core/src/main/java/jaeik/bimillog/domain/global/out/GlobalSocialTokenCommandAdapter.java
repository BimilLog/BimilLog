package jaeik.bimillog.domain.global.out;

import jaeik.bimillog.domain.auth.entity.SocialToken;
import jaeik.bimillog.domain.auth.out.SocialTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>소셜 토큰 명령 어댑터</h2>
 * <p>소셜 플랫폼 OAuth 토큰 쓰기 기능을 구현하는 어댑터입니다.</p>
 * <p>SocialTokenRepository를 통해 실제 데이터베이스 작업을 수행합니다.</p>
 * <p>모든 소셜 플랫폼의 토큰 저장/삭제를 통합 관리합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class GlobalSocialTokenCommandAdapter {
    private final SocialTokenRepository socialTokenRepository;

    /**
     * <h3>소셜 토큰 저장</h3>
     * <p>새로운 소셜 토큰을 저장합니다.</p>
     * <p>SocialTokenRepository를 통해 데이터베이스에 저장합니다.</p>
     *
     * @param socialToken 저장할 소셜 토큰 엔티티
     * @return SocialToken 저장된 소셜 토큰 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    public SocialToken save(SocialToken socialToken) {
        return socialTokenRepository.save(socialToken);
    }

    /**
     * <h3>소셜 토큰 삭제</h3>
     * <p>회원 탈퇴 시 소셜 토큰을 삭제합니다.</p>
     *
     * @param memberId 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional
    public void deleteByMemberId(Long memberId) {
        socialTokenRepository.deleteByMemberId(memberId);
    }
}
