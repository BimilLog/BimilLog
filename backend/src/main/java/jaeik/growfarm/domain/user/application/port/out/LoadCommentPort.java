package jaeik.growfarm.domain.user.application.port.out;

import jaeik.growfarm.dto.comment.SimpleCommentDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * <h2>댓글 조회 포트</h2>
 * <p>User 도메인에서 Comment 도메인의 데이터에 접근하기 위한 Out-Port</p>
 *
 * @author jaeik
 * @version 2.0.0
 */
public interface LoadCommentPort {

    /**
     * <h3>사용자 작성 댓글 목록 조회</h3>
     * <p>특정 사용자가 작성한 댓글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return 작성한 댓글 목록 페이지
     * @author jaeik
     * @version 2.0.0
     */
    Page<SimpleCommentDTO> findCommentsByUserId(Long userId, Pageable pageable);

    /**
     * <h3>사용자 추천한 댓글 목록 조회</h3>
     * <p>특정 사용자가 추천한 댓글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return 추천한 댓글 목록 페이지
     * @author jaeik
     * @version 2.0.0
     */
    Page<SimpleCommentDTO> findLikedCommentsByUserId(Long userId, Pageable pageable);
}