package jaeik.bimillog.domain.auth.adapter;

import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.auth.entity.SocialToken;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.domain.member.service.MemberOnboardingService;
import jaeik.bimillog.domain.member.service.MemberQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * <h2>인증-사용자 도메인 연결 어댑터</h2>
 * <p>Auth 도메인과 Member 도메인을 연결하는 어댑터입니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class AuthToMemberAdapter {
    private final MemberOnboardingService memberOnboardingService;
    private final MemberQueryService memberQueryService;

    /**
     * <h3>기존 회원 정보 갱신</h3>
     * <p>소셜 프로필에서 가져온 최신 닉네임, 이미지, 소셜 토큰을 Member 엔티티에 반영합니다.</p>
     *
     * @param member 기존 회원 엔티티
     * @param newNickname 소셜 플랫폼에서 가져온 최신 닉네임
     * @param newProfileImage 소셜 플랫폼에서 가져온 최신 프로필 이미지 URL
     * @param savedSocialToken 영속화된 소셜 토큰 엔티티
     * @return 갱신된 회원 엔티티
     */
    public Member handleExistingMember(Member member, String newNickname, String newProfileImage, SocialToken savedSocialToken) {
        return memberOnboardingService.syncExistingMember(member, newNickname, newProfileImage, savedSocialToken);
    }

    public Member handleNewMember(SocialMemberProfile socialMemberProfile, SocialToken socialToken) {
        return memberOnboardingService.signup(socialMemberProfile, socialToken);
    }

    /**
     * <h3>소셜 제공자와 ID로 사용자 조회</h3>
     */
    public Optional<Member> findByProviderAndSocialId(SocialProvider provider, String socialId) {
        return memberQueryService.findByProviderAndSocialId(provider, socialId);
    }

    /**
     * <h3>사용자 ID로 사용자 조회</h3>
     * <p>특정 ID에 해당하는 사용자 엔티티를 조회합니다.</p>
     *
     * @param memberId 조회할 사용자 ID
     * @return Optional&lt;Member&gt; 조회된 사용자 객체 (존재하지 않으면 Optional.empty())
     */
    public Member findById(Long memberId) {
        return memberQueryService.findById(memberId);
    }
}
