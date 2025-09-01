package jaeik.bimillog.domain.user.application.port.out;

import jaeik.bimillog.domain.comment.entity.SimpleCommentInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * <h2>댓글 조회 포트</h2>
 * <p>사용자 도메인에서 댓글 도메인의 정보를 조회하기 위한 출력 포트입니다.</p>
 * <p>헥사고날 아키텍처 원칙에 따라 도메인 간 의존성을 관리합니다.</p>
 *
 * @author Jaeik  
 * @version 2.0.0
 */
public interface LoadCommentPort {

    /**
     * <h3>사용자 작성 댓글 목록 조회</h3>
     * <p>특정 사용자가 작성한 댓글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return Page<SimpleCommentInfo> 작성한 댓글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<SimpleCommentInfo> findCommentsByUserId(Long userId, Pageable pageable);

    /**
     * <h3>사용자 추천한 댓글 목록 조회</h3>
     * <p>특정 사용자가 추천한 댓글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return Page<SimpleCommentInfo> 추천한 댓글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<SimpleCommentInfo> findLikedCommentsByUserId(Long userId, Pageable pageable);
}