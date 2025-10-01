package jaeik.bimillog.domain.global.application.port.out;

import jaeik.bimillog.domain.member.entity.member.Member;

import java.util.Optional;

/**
 * <h2>사용자 조회 공용 포트</h2>
 * <p>여러 도메인에서 공통으로 사용하는 사용자 조회 기능을 제공하는 포트입니다.</p>
 * <p>사용자 ID 조회, 사용자명 조회, 사용자 존재성 확인, JPA 프록시 조회</p>
 * <p>반환 타입을 Optional&lt;Member&gt;로 통일하여 일관성을 제공합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface GlobalMemberQueryPort {

    /**
     * <h3>사용자 ID로 사용자 조회</h3>
     * <p>특정 ID에 해당하는 사용자 엔티티를 조회합니다.</p>
     * <p>모든 도메인에서 일관된 Optional 반환 타입을 사용합니다.</p>
     *
     * @param memberId 조회할 사용자 ID
     * @return Optional&lt;Member&gt; 조회된 사용자 객체 (존재하지 않으면 Optional.empty())
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<Member> findById(Long memberId);

    /**
     * <h3>사용자명으로 사용자 조회</h3>
     * <p>특정 사용자명에 해당하는 사용자 엔티티를 조회합니다.</p>
     * <p>Paper 도메인에서 메시지 작성 시 대상 사용자 검증에 주로 사용됩니다.</p>
     *
     * @param memberName 조회할 사용자명
     * @return Optional&lt;Member&gt; 조회된 사용자 객체 (존재하지 않으면 Optional.empty())
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<Member> findByMemberName(String memberName);

    /**
     * <h3>사용자명 존재 여부 확인</h3>
     * <p>특정 사용자명을 가진 사용자가 시스템에 존재하는지 확인합니다.</p>
     * <p>사용자 엔티티가 필요하지 않고 존재성만 확인할 때 효율적입니다.</p>
     *
     * @param memberName 확인할 사용자명
     * @return boolean 사용자명이 존재하면 true, 그렇지 않으면 false
     * @author Jaeik
     * @since 2.0.0
     */
    boolean existsByMemberName(String memberName);

    /**
     * <h3>사용자 ID로 JPA 프록시 참조 조회</h3>
     * <p>실제 데이터베이스 조회 없이 사용자 ID를 가진 Member 프록시 객체를 반환합니다.</p>
     * <p>JPA 연관관계 설정 시 성능 최적화를 위해 사용됩니다.</p>
     * <p>실제 Member 데이터가 필요한 경우가 아닌 FK 설정용으로만 사용해야 합니다.</p>
     *
     * @param memberId 참조할 사용자 ID
     * @return Member 프록시 객체 (실제 데이터는 지연 로딩)
     * @author Jaeik
     * @since 2.0.0
     */
    Member getReferenceById(Long memberId);
}