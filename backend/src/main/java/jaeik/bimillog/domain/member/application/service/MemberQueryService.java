
package jaeik.bimillog.domain.member.application.service;

import jaeik.bimillog.domain.member.application.port.in.MemberQueryUseCase;
import jaeik.bimillog.domain.member.application.port.out.MemberQueryPort;
import jaeik.bimillog.domain.member.entity.Setting;
import jaeik.bimillog.domain.member.entity.member.Member;
import jaeik.bimillog.domain.member.exception.UserCustomException;
import jaeik.bimillog.domain.member.exception.UserErrorCode;
import jaeik.bimillog.infrastructure.adapter.in.member.web.MemberQueryController;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * <h2>사용자 조회 서비스</h2>
 * <p>UserQueryUseCase의 구현체로 사용자 정보 조회 로직을 담당합니다.</p>
 * <p>사용자 엔티티 조회, 설정 조회, 닉네임 검증</p>
 * <p>소셜 로그인 사용자 조회, 토큰 기반 인증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class MemberQueryService implements MemberQueryUseCase {

    private final MemberQueryPort memberQueryPort;

    /**
     * <h3>ID로 사용자 조회</h3>
     * <p>사용자 ID를 사용하여 사용자를 조회합니다.</p>
     * <p>{@link MemberQueryUseCase}에서 기본 사용자 조회 시 호출됩니다.</p>
     *
     * @param id 사용자의 고유 ID
     * @return Optional<Member> 조회된 사용자 객체. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Member> findById(Long id) {
        return memberQueryPort.findById(id);
    }

    /**
     * <h3>닉네임 중복 확인</h3>
     * <p>해당 닉네임을 가진 사용자가 존재하는지 확인합니다.</p>
     * <p>{@link MemberQueryController}에서 닉네임 중복 확인 API 시 호출됩니다.</p>
     *
     * @param userName 확인할 닉네임
     * @return boolean 존재하면 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public boolean existsByUserName(String userName) {
        return memberQueryPort.existsByUserName(userName);
    }

    /**
     * <h3>닉네임으로 사용자 조회</h3>
     * <p>닉네임을 사용하여 사용자를 조회합니다.</p>
     * <p>{@link MemberQueryUseCase}에서 닉네임 기반 사용자 조회 시 호출됩니다.</p>
     *
     * @param userName 사용자 닉네임
     * @return Optional<Member> 조회된 사용자 객체. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Member> findByUserName(String userName) {
        return memberQueryPort.findByUserName(userName);
    }


    /**
     * <h3>ID로 사용자 프록시 조회</h3>
     * <p>실제 쿼리 없이 ID를 가진 사용자의 프록시(참조) 객체를 반환합니다.</p>
     * <p>JPA 연관 관계 설정 시 사용됩니다.</p>
     * <p>{@link MemberQueryUseCase}에서 사용자 엔티티 참조 생성 시 호출됩니다.</p>
     *
     * @param userId 사용자 ID
     * @return Member 프록시 객체
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public Member getReferenceById(Long userId) {
        return memberQueryPort.getReferenceById(userId);
    }

    /**
     * <h3>설정 ID로 설정 조회</h3>
     * <p>JWT 토큰의 settingId를 활용하여 설정 정보를 조회합니다.</p>
     * <p>Member 엔티티 전체 조회 없이 Setting만 직접 조회합니다.</p>
     * <p>{@link MemberQueryController}에서 사용자 설정 조회 API 시 호출됩니다.</p>
     *
     * @param settingId 설정 ID
     * @return 설정 엔티티
     * @throws UserCustomException 설정을 찾을 수 없는 경우
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    @Transactional(readOnly = true)
    public Setting findBySettingId(Long settingId) {
        return memberQueryPort.findSettingById(settingId)
                .orElseThrow(() -> new UserCustomException(UserErrorCode.SETTINGS_NOT_FOUND));
    }
}
