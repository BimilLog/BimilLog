package jaeik.bimillog.infrastructure.adapter.out.auth;

import jaeik.bimillog.domain.auth.application.port.out.AuthToMemberPort;
import jaeik.bimillog.domain.auth.entity.KakaoToken;
import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.member.application.port.in.MemberQueryUseCase;
import jaeik.bimillog.domain.member.application.port.in.MemberSaveUseCase;
import jaeik.bimillog.domain.member.entity.member.Member;
import jaeik.bimillog.domain.member.entity.member.SocialProvider;
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
 * @since 2025-01
 */
@Component
@RequiredArgsConstructor
public class AuthToMemberAdapter implements AuthToMemberPort {

    private final MemberSaveUseCase memberSaveUseCase;
    private final MemberQueryUseCase memberQueryUseCase;

    @Override
    public Member handleExistingMember(Member member, String newNickname, String newProfileImage, KakaoToken savedKakaoToken) {
        return memberSaveUseCase.handleExistingMember(member, newNickname, newProfileImage, savedKakaoToken);
    }

    @Override
    public void handleNewUser(SocialMemberProfile memberProfile, String uuid) {
        memberSaveUseCase.handleNewMember(memberProfile, uuid);
    }

    @Override
    public Optional<Member> checkMember(SocialProvider provider, String socialId) {
        return memberQueryUseCase.findByProviderAndSocialId(provider, socialId);
    }



}