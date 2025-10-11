package jaeik.bimillog.domain.member.application.port.in;

import jaeik.bimillog.domain.auth.entity.KakaoToken;
import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.member.entity.Member;

/**
 * <h2>사용자 로그인 처리 유스케이스</h2>
 * <p>소셜 로그인 시 Member 도메인의 사용자 처리 기능을 정의하는 포트입니다.</p>
 * <p>기존 회원 정보 갱신과 신규 회원 임시 저장 기능을 제공합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface HandleMemberLoginUseCase {

    /**
     * <h3>기존 회원 정보 갱신</h3>
     * <p>소셜 프로필에서 가져온 최신 닉네임, 이미지, 카카오 토큰을 Member 엔티티에 반영합니다.</p>
     * <p>영속성 컨텍스트 내에서 dirty checking을 통해 자동 업데이트됩니다.</p>
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
     * <h3>신규 회원 임시 정보 저장</h3>
     * <p>회원가입 전 단계로, 소셜 프로필 정보를 UUID 키와 함께 Redis에 임시 저장합니다.</p>
     * <p>클라이언트는 UUID를 쿠키로 받아 회원가입 단계에서 다시 전송합니다.</p>
     *
     * @param memberProfile 소셜 플랫폼에서 가져온 프로필 정보
     * @param uuid 임시 저장소 키 (클라이언트 쿠키와 매칭)
     * @author Jaeik
     * @since 2.0.0
     */
    void handleNewMember(SocialMemberProfile memberProfile, String uuid);
}
