package jaeik.bimillog.domain.comment.application.port.out;

import jaeik.bimillog.domain.comment.application.service.CommentCommandService;
import jaeik.bimillog.domain.user.entity.User;

import java.util.Optional;

/**
 * <h2>댓글-사용자 연동 포트</h2>
 * <p>댓글 도메인에서 사용자 도메인의 데이터에 접근하기 위한 포트입니다.</p>
 * <p>사용자 존재성 검증, 사용자 정보 조회</p>
 * <p>익명 댓글 지원을 위한 Optional 반환</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface CommentToUserPort {

    /**
     * <h3>사용자 ID로 사용자 엔티티 조회</h3>
     * <p>특정 ID에 해당하는 사용자 엔티티를 조회합니다.</p>
     * <p>익명 댓글 시스템 지원을 위해 Optional로 반환합니다.</p>
     * <p>{@link CommentCommandService}에서 댓글 작성 시 사용자 정보 조회 및 알림 발송을 위해 호출됩니다.</p>
     *
     * @param userId 사용자 ID
     * @return Optional<User> 조회된 사용자 객체. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<User> findById(Long userId);
}