package jaeik.bimillog.domain.post.application.port.out;

import jaeik.bimillog.domain.user.entity.User;

/**
 * <h2>PostToUserPort</h2>
 * <p>
 * Post 도메인에서 User 도메인의 데이터를 조회하기 위한 크로스 도메인 아웃바운드 포트입니다.
 * 헥사고날 아키텍처에서 도메인 간 데이터 의존성을 추상화하여 도메인 경계를 명확히 분리합니다.
 * </p>
 * <p>PostCommandService에서 게시글 생성 시 작성자 정보 연결을 위해 호출됩니다.</p>
 * <p>PostLikeCommandService에서 게시글 추천 시 추천자 정보 연결을 위해 호출됩니다.</p>
 * <p>User 도메인의 세부 구현을 숨기고 Post 도메인이 필요로 하는 User 참조만 제공합니다.</p>
 * <p>JPA 프록시 패턴을 통해 실제 User 데이터 로드 없이 효율적인 연관 관계 설정을 지원합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PostToUserPort {

    /**
     * <h3>사용자 ID 기반 프록시 참조 조회</h3>
     * <p>실제 데이터베이스 조회 없이 사용자 ID를 가진 User 프록시 객체를 반환합니다.</p>
     * <p>PostCommandService에서 게시글 생성 시 작성자 연결을 위해 호출됩니다.</p>
     * <p>PostLikeCommandService에서 게시글 추천 시 추천자 연결을 위해 호출됩니다.</p>
     * <p>JPA의 getReference 메서드를 통해 프록시 객체를 반환하므로 불필요한 User 테이블 조회를 방지합니다.</p>
     * <p>연관 관계 설정 시에만 사용되며, User의 실제 데이터가 필요한 경우 별도 조회가 필요합니다.</p>
     *
     * @param userId 참조할 사용자의 ID
     * @return User 프록시 객체 (실제 데이터는 지연 로딩)
     * @author Jaeik
     * @since 2.0.0
     */
    User getReferenceById(Long userId);
}
