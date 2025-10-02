package jaeik.bimillog.domain.member.application.port.in;

import jaeik.bimillog.domain.auth.entity.KakaoToken;
import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.member.entity.member.Member;

public interface MemberSaveUseCase {


    Member handleExistingMember(Member member, String newNickname, String newProfileImage, KakaoToken savedKakaoToken);

    void handleNewMember(SocialMemberProfile memberProfile, String uuid);
}
