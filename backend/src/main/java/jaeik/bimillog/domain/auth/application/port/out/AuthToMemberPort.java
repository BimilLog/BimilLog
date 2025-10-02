package jaeik.bimillog.domain.auth.application.port.out;

import jaeik.bimillog.domain.auth.application.service.SocialLoginService;
import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.member.entity.MemberDetail;

/**
 * <h2>인증 TO 유저 포트</h2>
 * <p>인증 도메인에서 유저 도메인에 접근하는 포트입니다.</p>
 * <p>로그인 시 사용자 데이터를 담당합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface AuthToMemberPort {

    /**
     * <h3>로그인 시 사용자 데이터 처리</h3>
     * <p>소셜 사용자 프로필을 기반으로 사용자 데이터를 처리합니다.</p>
     * <p>기존 회원이면 로그인 처리를, 신규 회원이면 임시 데이터 저장을 수행합니다.</p>
     * <p>{@link SocialLoginService}에서 소셜 로그인 처리 중 사용자 데이터 처리 시 호출됩니다.</p>
     *
     * @param profile 소셜 사용자 프로필 정보 (FCM 토큰, provider 포함)
     * @return MemberDetail 기존 사용자(uuid = null) 또는 신규 사용자(uuid != null) 정보
     * @author Jaeik
     * @since 2.0.0
     */
    MemberDetail delegateUserData(SocialMemberProfile profile);

}