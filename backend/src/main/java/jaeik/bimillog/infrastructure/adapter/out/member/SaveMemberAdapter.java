package jaeik.bimillog.infrastructure.adapter.out.member;

import jaeik.bimillog.domain.member.application.port.out.SaveMemberPort;
import jaeik.bimillog.domain.member.entity.member.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>사용자 저장 어댑터</h2>
 * <p>Member 도메인의 아웃바운드 어댑터로 사용자 데이터 영속성을 담당합니다.</p>
 * <p>소셜 로그인 사용자의 실제 저장 로직과 관련 엔티티 처리를 수행합니다.</p>
 *
 * <h3>주요 책임:</h3>
 * <ul>
 *   <li>기존 사용자: 프로필 업데이트, AuthToken 엔티티 생성, FCM 토큰 등록</li>
 *   <li>신규 사용자: Member/Setting 엔티티 생성, 임시 데이터 삭제, JWT 쿠키 발급</li>
 *   <li>FCM 토큰 관리 및 NotificationFcmUseCase와 통합</li>
 * </ul>
 *
 * <p><b>도메인 분리:</b> Auth 도메인에서 Member 도메인으로 이동되어 사용자 데이터 저장 책임만 담당</p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2025-01
 */
@Component
@RequiredArgsConstructor
public class SaveMemberAdapter implements SaveMemberPort {

    private final MemberRepository userRepository;

    /**
     * <h3>신규 사용자 등록</h3>
     * <p>소셜 로그인 회원가입에서 입력받은 닉네임과 임시 데이터를 사용하여 신규 회원을 등록합니다.</p>
     * <p>Member 엔티티와 Setting 생성, AuthToken 엔티티 생성/저장, FCM 토큰 등록을 수행합니다.</p>
     *
     * @param memberName 사용자가 입력한 닉네임
     * @param userProfile 소셜 사용자 프로필 (OAuth 액세스/리프레시 토큰, FCM 토큰 포함)
     * @return MemberDetail 생성된 사용자 정보를 담은 객체
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public Member saveNewMember(Member member) {
        return userRepository.save(member);
    }
}