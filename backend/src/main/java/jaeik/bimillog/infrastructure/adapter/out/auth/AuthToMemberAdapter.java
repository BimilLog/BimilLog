package jaeik.bimillog.infrastructure.adapter.out.auth;

import jaeik.bimillog.domain.auth.application.port.out.AuthToMemberPort;
import jaeik.bimillog.domain.auth.entity.KakaoToken;
import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.member.application.port.in.HandleMemberLoginUseCase;
import jaeik.bimillog.domain.member.application.port.in.MemberQueryUseCase;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * <h2>인증-사용자 도메인 연결 어댑터</h2>
 * <p>Auth 도메인과 Member 도메인을 연결하는 아웃바운드 어댑터입니다.</p>
 * <p>인증 도메인에서 사용자 도메인의 기능을 호출하는 중개 역할을 수행합니다.</p>
 *
 * <h3>주요 책임:</h3>
 * <ul>
 *   <li>Auth 도메인의 사용자 처리 요청을 Member 도메인으로 전달</li>
 *   <li>Member 도메인의 처리 결과를 LoginResult로 변환</li>
 *   <li>기존 사용자에 대한 JWT 쿠키 생성</li>
 *   <li>신규 사용자에 대한 임시 쿠키 생성</li>
 * </ul>
 *
 * <p><b>도메인 경계:</b> Auth 도메인의 포트 구현체로서 Member 도메인과의 통신을 담당</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class AuthToMemberAdapter implements AuthToMemberPort {

    private final HandleMemberLoginUseCase handleMemberLoginUseCase;
    private final MemberQueryUseCase memberQueryUseCase;

    /**
     * <h3>기존 회원 정보 갱신</h3>
     * <p>소셜 프로필에서 가져온 최신 닉네임, 이미지, 카카오 토큰을 Member 엔티티에 반영합니다.</p>
     * <p>Member 도메인의 HandleMemberLoginUseCase로 위임하여 처리합니다.</p>
     *
     * @param member 기존 회원 엔티티
     * @param newNickname 소셜 플랫폼에서 가져온 최신 닉네임
     * @param newProfileImage 소셜 플랫폼에서 가져온 최신 프로필 이미지 URL
     * @param savedKakaoToken 영속화된 카카오 토큰 엔티티
     * @return 갱신된 회원 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Member handleExistingMember(Member member, String newNickname, String newProfileImage, KakaoToken savedKakaoToken) {
        return handleMemberLoginUseCase.handleExistingMember(member, newNickname, newProfileImage, savedKakaoToken);
    }

    /**
     * <h3>신규 회원 임시 정보 저장</h3>
     * <p>회원가입 전 단계로, 소셜 프로필 정보를 UUID 키와 함께 Redis에 임시 저장합니다.</p>
     * <p>Member 도메인의 HandleMemberLoginUseCase로 위임하여 처리합니다.</p>
     *
     * @param memberProfile 소셜 플랫폼에서 가져온 프로필 정보
     * @param uuid 임시 저장소 키 (클라이언트 쿠키와 매칭)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void handleNewUser(SocialMemberProfile memberProfile, String uuid) {
        handleMemberLoginUseCase.handleNewMember(memberProfile, uuid);
    }

    /**
     * <h3>회원 존재 여부 조회</h3>
     * <p>소셜 제공자와 소셜 ID로 기존 회원이 있는지 확인합니다.</p>
     * <p>Member 도메인의 MemberQueryUseCase로 위임하여 처리합니다.</p>
     *
     * @param provider 소셜 플랫폼 제공자 (KAKAO 등)
     * @param socialId 소셜 플랫폼에서 제공하는 고유 ID
     * @return Optional&lt;Member&gt; 조회된 사용자 (존재하지 않으면 Optional.empty())
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<Member> checkMember(SocialProvider provider, String socialId) {
        return memberQueryUseCase.findByProviderAndSocialId(provider, socialId);
    }



}