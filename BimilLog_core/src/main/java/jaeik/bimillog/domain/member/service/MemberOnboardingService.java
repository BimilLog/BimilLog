package jaeik.bimillog.domain.member.service;

import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.auth.entity.SocialToken;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.entity.Setting;
import jaeik.bimillog.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * <h2>회원 온보딩 서비스</h2>
 * <p>신규/기존 회원 소셜 로그인 흐름과 가입 확정을 한 곳에서 담당합니다.</p>
 */
@Service
@RequiredArgsConstructor
public class MemberOnboardingService {
    private final MemberRepository memberRepository;

    /**
     * <h3>기존 회원 정보 동기화</h3>
     */
    @Transactional
    public Member syncExistingMember(Member member, String newNickname, String newProfileImage, SocialToken savedSocialToken) {
        member.updateSocialToken(savedSocialToken);
        member.updateMemberInfo(newNickname, newProfileImage);
        return member;
    }

    /**
     * <h3>신규 가입 처리</h3>
     * <p>임시 이름(냥_XXXXXX)을 자동 생성하여 회원을 등록합니다.</p>
     */
    @Transactional
    public Member signup(SocialMemberProfile socialMemberProfile, SocialToken socialToken) {
        String memberName = generateUniqueTempName();
        Setting setting = Setting.createSetting();
        Member member = Member.createMember(
                socialMemberProfile.getSocialId(),
                socialMemberProfile.getProvider(),
                socialMemberProfile.getNickname(),
                socialMemberProfile.getProfileImageUrl(),
                memberName,
                setting,
                socialToken
        );
        return memberRepository.save(member);
    }

    private String generateUniqueTempName() {
        String name;
        do {
            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
            name = "냥_" + suffix;
        } while (memberRepository.existsByMemberName(name));
        return name;
    }
}
