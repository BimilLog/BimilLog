package jaeik.bimillog.domain.member.service;

import jaeik.bimillog.domain.auth.entity.AuthToken;
import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.auth.entity.SocialToken;
import jaeik.bimillog.domain.global.entity.CustomUserDetails;
import jaeik.bimillog.domain.global.out.GlobalCookieAdapter;
import jaeik.bimillog.domain.global.out.GlobalJwtAdapter;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.entity.Setting;
import jaeik.bimillog.domain.member.out.MemberRepository;
import jaeik.bimillog.domain.member.out.MemberToAuthAdapter;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.infrastructure.redis.member.RedisMemberDataAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * <h2>회원 온보딩 서비스</h2>
 * <p>신규/기존 회원 소셜 로그인 흐름과 가입 확정을 한 곳에서 담당합니다.</p>
 */
@Service
@RequiredArgsConstructor
public class MemberOnboardingService {
    private final RedisMemberDataAdapter redisMemberDataAdapter;
    private final MemberRepository memberRepository;
    private final MemberToAuthAdapter memberToAuthAdapter;
    private final GlobalCookieAdapter globalCookieAdapter;
    private final GlobalJwtAdapter globalJwtAdapter;

    /**
     * <h3>온보딩 대기 데이터 저장</h3>
     */
    @Transactional
    public void storePendingMember(SocialMemberProfile memberProfile, String uuid) {
        redisMemberDataAdapter.saveTempData(uuid, memberProfile);
    }

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
     */
    @Transactional
    public List<ResponseCookie> signup(String memberName, String uuid) {
        try {
            Optional<SocialMemberProfile> socialMemberProfile = redisMemberDataAdapter.getTempData(uuid);
            if (socialMemberProfile.isEmpty()) {
                throw new CustomException(ErrorCode.AUTH_INVALID_TEMP_DATA);
            }

            SocialMemberProfile memberProfile = socialMemberProfile.get();
            SocialToken initialSocialToken = SocialToken.createSocialToken(
                    memberProfile.getAccessToken(),
                    memberProfile.getRefreshToken()
            );
            SocialToken persistedSocialToken = memberToAuthAdapter.saveSocialToken(initialSocialToken);

            Setting setting = Setting.createSetting();
            Member member = Member.createMember(
                    memberProfile.getSocialId(),
                    memberProfile.getProvider(),
                    memberProfile.getNickname(),
                    memberProfile.getProfileImageUrl(),
                    memberName,
                    setting,
                    persistedSocialToken
            );

            Member persistedMember = memberRepository.save(member);

            AuthToken initialAuthToken = AuthToken.createToken("", persistedMember);
            AuthToken persistedAuthToken = memberToAuthAdapter.saveAuthToken(initialAuthToken);

            CustomUserDetails userDetails = CustomUserDetails.ofExisting(persistedMember, persistedAuthToken.getId());
            String accessToken = globalJwtAdapter.generateAccessToken(userDetails);
            String refreshToken = globalJwtAdapter.generateRefreshToken(userDetails);
            memberToAuthAdapter.updateJwtRefreshToken(persistedAuthToken.getId(), refreshToken);

            redisMemberDataAdapter.removeTempData(uuid);

            return globalCookieAdapter.generateJwtCookie(accessToken, refreshToken);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.MEMBER_EXISTED_NICKNAME, e);
        }
    }
}
