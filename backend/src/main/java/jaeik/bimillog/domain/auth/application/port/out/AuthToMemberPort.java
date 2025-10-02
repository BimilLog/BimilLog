package jaeik.bimillog.domain.auth.application.port.out;

import jaeik.bimillog.domain.auth.entity.KakaoToken;
import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.member.entity.member.Member;
import jaeik.bimillog.domain.member.entity.member.SocialProvider;

import java.util.Optional;

/**
 * <h2>인증→회원 연결 포트</h2>
 * <p>소셜 로그인 단계에서 Auth 도메인이 Member 도메인의 기능을 호출할 때 사용하는 포트입니다.</p>
 * <p>회원 존재 여부 확인, 기존 회원 정보 갱신, 신규 회원 임시 저장 책임을 제공합니다.</p>
 */
public interface AuthToMemberPort {

    /**
     * <h3>기존 회원 갱신</h3>
     * <p>소셜 프로필에서 가져온 닉네임/이미지/카카오 토큰을 Member 엔티티에 반영합니다.</p>
     */
    Member handleExistingMember(Member member, String newNickname, String newProfileImage, KakaoToken savedKakaoToken);

    /**
     * <h3>신규 회원 임시 저장</h3>
     * <p>추후 회원가입 단계에서 사용할 소셜 프로필을 Redis 등 임시 저장소에 보관합니다.</p>
     */
    void handleNewUser(SocialMemberProfile memberProfile, String uuid);

    /**
     * <h3>회원 존재 여부 조회</h3>
     * <p>소셜 제공자와 소셜 ID로 기존 회원이 있는지 확인합니다.</p>
     */
    Optional<Member> checkMember(SocialProvider provider, String socialId);
}
