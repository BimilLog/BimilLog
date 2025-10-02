package jaeik.bimillog.domain.member.application.port.out;

import jaeik.bimillog.domain.auth.entity.MemberDetail;
import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.member.application.service.SignUpService;

/**
 * <h2>사용자 정보 저장 포트</h2>
 * <p>소셜 로그인과 회원가입 과정에서 사용자 데이터를 저장하는 포트입니다.</p>
 * <p>기존 사용자 로그인 처리, 신규 사용자 계정 생성, AuthToken 엔티티 저장, JWT 쿠키 발급</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface SaveMemberPort {

    /**
     * <h3>신규 사용자 정보 저장</h3>
     * <p>회원가입을 완료하는 신규 사용자의 정보를 데이터베이스에 저장합니다.</p>
     * <p>사용자 엔티티와 기본 설정 생성, AuthToken 엔티티 저장, FCM 토큰 등록을 처리합니다.</p>
     * <p>{@link SignUpService}에서 신규 사용자 회원가입 완료 처리 시 호출됩니다.</p>
     *
     * @param memberName 사용자가 입력한 닉네임
     * @param userProfile Redis에서 복원된 소셜 사용자 프로필 정보 (OAuth 액세스/리프레시 토큰, FCM 토큰 포함)
     * @return MemberDetail 생성된 사용자 정보를 담은 객체 (uuid = null)
     * @author Jaeik
     * @since 2.0.0
     */
    MemberDetail saveNewMember(String memberName, SocialMemberProfile userProfile);

}