package jaeik.bimillog.domain.comment.application.port.out;

import jaeik.bimillog.domain.user.entity.User;

import java.util.Optional;

/**
 * <h2>CommentToUserPort</h2>
 * <p>
 * Comment 도메인에서 User 도메인의 데이터에 접근하기 위한 아웃바운드 포트입니다.
 * 헥사고날 아키텍처에서 도메인 간 의존성을 추상화하여 도메인의 독립성을 보장합니다.
 * </p>
 * <p>
 * 이 포트는 댓글 도메인이 사용자 도메인의 정보를 조회하는 기능을 제공합니다:
 * - 사용자 존재성 검증: 댓글 작성자의 유효성 확인
 * - 사용자 정보 조회: 댓글과 연관된 사용자의 세부 정보 획득
 * - 익명 댓글 지원: Optional 반환을 통한 유연한 사용자 처리
 * </p>
 * <p>
 * 비즈니스 컨텍스트에서 이 포트가 필요한 이유:
 * 1. 익명 댓글 지원 - 로그인하지 않은 사용자의 댓글 작성 허용
 * 2. 사용자 검증 분리 - 각 도메인이 필요에 따라 적절한 검증 수준 적용
 * 3. 도메인 경계 유지 - Comment 도메인이 User 도메인의 구현에 직접 의존하지 않음
 * </p>
 * <p>
 * CommentService에서 댓글 작성 시 사용자 정보 조회를 위해 사용됩니다.
 * CommentService에서 댓글 알림 발송 시 사용자 정보 확인을 위해 사용됩니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface CommentToUserPort {

    /**
     * <h3>사용자 ID로 사용자 엔티티 조회</h3>
     * <p>특정 ID에 해당하는 사용자 엔티티를 조회합니다.</p>
     * <p>익명 댓글 시스템 지원을 위해 Optional로 반환하여 사용자가 존재하지 않을 경우를 우아하게 처리합니다.</p>
     * <p>호출하는 도메인 서비스에서 비즈니스 요구사항에 따라 사용자 필수 여부를 결정할 수 있습니다.</p>
     * <p>CommentService에서 댓글 작성자 정보 조회 시 호출됩니다.</p>
     * <p>CommentService에서 댓글 알림 대상 사용자 확인 시 호출됩니다.</p>
     *
     * @param userId 사용자 ID
     * @return Optional<User> 조회된 사용자 객체. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<User> findById(Long userId);
}