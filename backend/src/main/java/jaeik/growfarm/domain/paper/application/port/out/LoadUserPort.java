package jaeik.growfarm.domain.paper.application.port.out;

import jaeik.growfarm.entity.user.Users;

/**
 * <h2>사용자 조회 포트</h2>
 * <p>
 * Secondary Port: 사용자 관련 데이터 조회를 위한 포트
 * Paper 도메인에서 필요한 사용자 검증 기능만 포함
 * 기존 UserRepository의 일부 기능을 포함
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0 (헥사고날 아키텍처 적용)
 */
public interface LoadUserPort {

    /**
     * <h3>사용자명 존재 여부 확인</h3>
     * <p>
     * 기존 UserRepository.existsByUserName() 메서드와 동일한 기능
     * 롤링페이퍼 방문 시 사용자 존재 여부 검증용
     * </p>
     *
     * @param userName 확인할 사용자명
     * @return 사용자 존재 여부
     * @author Jaeik
     * @since 2.0.0
     */
    boolean existsByUserName(String userName);

    /**
     * <h3>사용자명으로 사용자 조회</h3>
     * <p>
     * 기존 UserRepository.findByUserName() 메서드와 동일한 기능
     * 메시지 작성 시 롤링페이퍼 소유자 조회용
     * </p>
     *
     * @param userName 조회할 사용자명
     * @return 조회된 사용자 엔티티 (없으면 null)
     * @author Jaeik
     * @since 2.0.0
     */
    Users findByUserName(String userName);
}