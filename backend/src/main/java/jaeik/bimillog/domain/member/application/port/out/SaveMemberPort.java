package jaeik.bimillog.domain.member.application.port.out;

import jaeik.bimillog.domain.member.application.service.MemberSignupService;
import jaeik.bimillog.domain.member.entity.member.Member;

/**
 * <h2>회원 저장 포트</h2>
 * <p>{@link MemberSignupService}가 생성한 신규 {@link Member} 엔티티를 영속화합니다.</p>
 * <p>추가 비즈니스 로직 없이 JPA 저장 책임만 외부로 위임하기 위한 포트입니다.</p>
 */
public interface SaveMemberPort {

    /**
     * <h3>신규 회원 저장</h3>
     * <p>{@link MemberSignupService}가 조립한 {@link Member} 엔티티를 영속화합니다.</p>
     *
     * @param member 저장할 회원 엔티티
     * @return 저장된 회원 엔티티(식별자가 부여됨)
     */
    Member saveNewMember(Member member);

}
