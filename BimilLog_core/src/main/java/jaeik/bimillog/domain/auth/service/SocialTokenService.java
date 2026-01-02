package jaeik.bimillog.domain.auth.service;

import jaeik.bimillog.domain.auth.entity.SocialToken;
import jaeik.bimillog.domain.auth.out.SocialTokenQueryRepository;
import jaeik.bimillog.domain.auth.out.SocialTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SocialTokenService {
    private final SocialTokenQueryRepository socialTokenQueryRepository;
    private final SocialTokenRepository socialTokenRepository;

    public Optional<SocialToken> getSocialToken(Long memberId) {
        return socialTokenQueryRepository.findSocialTokenByMemberId(memberId);
    }

    /**
     * <h3>소셜 토큰 삭제</h3>
     * <p>회원 탈퇴 시 소셜 토큰을 삭제합니다.</p>
     *
     * @param memberId 사용자 ID
     */
    @Transactional
    public void deleteByMemberId(Long memberId) {
        socialTokenRepository.deleteByMemberId(memberId);
    }

    /**
     * <h3>소셜 토큰 저장</h3>
     * <p>새로운 소셜 토큰을 저장합니다.</p>
     * <p>SocialTokenRepository를 통해 데이터베이스에 저장합니다.</p>
     *
     * @param socialToken 저장할 소셜 토큰 엔티티
     * @return SocialToken 저장된 소셜 토큰 엔티티
     */
    public SocialToken save(SocialToken socialToken) {
        return socialTokenRepository.save(socialToken);
    }
}
