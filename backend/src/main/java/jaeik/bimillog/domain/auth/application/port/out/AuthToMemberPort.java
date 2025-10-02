package jaeik.bimillog.domain.auth.application.port.out;

import jaeik.bimillog.domain.auth.entity.KakaoToken;
import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.member.entity.member.Member;
import jaeik.bimillog.domain.member.entity.member.SocialProvider;

import java.util.Optional;

/**
 * <h2>인증 TO 유저 포트</h2>
 * <p>인증 도메인에서 유저 도메인에 접근하는 포트입니다.</p>
 * <p>로그인 시 사용자 데이터를 담당합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface AuthToMemberPort {


    Member handleExistingMember(Member member, String newNickname, String newProfileImage, KakaoToken savedKakaoToken);


    void handleNewUser(SocialMemberProfile memberProfile, String uuid);

    Optional<Member> checkMember(SocialProvider provider, String socialId);
}