package jaeik.growfarm.domain.post.application.port.out;

import jaeik.growfarm.domain.user.entity.User;

/**
 * <h2>사용자 조회 포트 (Post 도메인용)</h2>
 * <p>Post 도메인에서 사용자 정보를 조회하기 위한 아웃고잉 포트입니다.</p>
 *
 * @author BimilLog
 * @version 1.0.0
 */
public interface LoadUserPort {

    /**
     * <h3>ID로 사용자 프록시 조회</h3>
     * <p>실제 쿼리 없이 ID를 가진 사용자의 프록시(참조) 객체를 반환합니다.</p>
     * <p>JPA 연관 관계 설정 시 성능 최적화를 위해 사용됩니다.</p>
     *
     * @param userId 사용자 ID
     * @return User 프록시 객체
     */
    User getReferenceById(Long userId);
}
