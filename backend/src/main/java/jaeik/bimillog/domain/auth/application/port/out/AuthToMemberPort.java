package jaeik.bimillog.domain.auth.application.port.out;

import jaeik.bimillog.domain.auth.entity.KakaoToken;
import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.entity.SocialProvider;

import java.util.Optional;

/**
 * <h2>인증→회원 연결 포트</h2>
 * <p>소셜 로그인 단계에서 Auth 도메인이 Member 도메인의 기능을 호출할 때 사용하는 포트입니다.</p>
 * <p>회원 존재 여부 확인, 기존 회원 정보 갱신, 신규 회원 임시 저장 책임을 제공합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface AuthToMemberPort {

    /**
     * <h3>기존 회원 갱신</h3>
     * <p>소셜 프로필에서 가져온 닉네임/이미지/카카오 토큰을 Member 엔티티에 반영합니다.</p>
     *
     * @param member 기존 회원 엔티티
     * @param newNickname 소셜 플랫폼에서 가져온 최신 닉네임
     * @param newProfileImage 소셜 플랫폼에서 가져온 최신 프로필 이미지 URL
     * @param savedKakaoToken 영속화된 카카오 토큰 엔티티
     * @return 갱신된 회원 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    Member handleExistingMember(Member member, String newNickname, String newProfileImage, KakaoToken savedKakaoToken);

    /**
     * <h3>신규 회원 임시 저장</h3>
     * <p>추후 회원가입 단계에서 사용할 소셜 프로필을 Redis 등 임시 저장소에 보관합니다.</p>
     *
     * @param memberProfile 소셜 플랫폼에서 가져온 프로필 정보
     * @param uuid 임시 저장소 키 (클라이언트 쿠키와 매칭)
     * @author Jaeik
     * @since 2.0.0
     */
    void handleNewUser(SocialMemberProfile memberProfile, String uuid);

    /**
     * <h3>회원 존재 여부 조회</h3>
     * <p>소셜 제공자와 소셜 ID로 기존 회원이 있는지 확인합니다.</p>
     *
     * @param provider 소셜 플랫폼 제공자 (KAKAO 등)
     * @param socialId 소셜 플랫폼에서 제공하는 고유 ID
     * @return Optional&lt;Member&gt; 조회된 사용자 (존재하지 않으면 Optional.empty())
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<Member> checkMember(SocialProvider provider, String socialId);
}
