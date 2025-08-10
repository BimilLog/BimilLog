package jaeik.growfarm.repository.comment.user;

import jaeik.growfarm.dto.comment.SimpleCommentDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>댓글 사용자별 조회 저장소</h2>
 * <p>
 * 사용자별 댓글 조회 기능을 담당한다.
 * Post Repository의 PostUserRepository 구조를 참조하여 ISP 원칙 적용
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
public interface CommentUserRepository {

    /**
     * <h3>사용자 작성 댓글 조회</h3>
     * <p>
     * 사용자 ID를 기준으로 해당 사용자가 작성한 댓글 목록을 조회한다.
     * </p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이징 정보
     * @return 사용자가 작성한 댓글 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<SimpleCommentDTO> findCommentsByUserId(Long userId, Pageable pageable);

    /**
     * <h3>사용자가 추천한 댓글 조회</h3>
     * <p>
     * 사용자 ID를 기준으로 해당 사용자가 추천한 댓글 목록을 조회한다.
     * </p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이징 정보
     * @return 사용자가 추천한 댓글 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<SimpleCommentDTO> findLikedCommentsByUserId(Long userId, Pageable pageable);

    /**
     * <h3>사용자가 추천한 댓글 ID 배치 조회</h3>
     * <p>
     * 특정 사용자가 댓글 ID 리스트 중 추천한 댓글들을 조회한다.
     * </p>
     * 
     * @param commentIds 댓글 ID 리스트
     * @param userId     사용자 ID
     * @return 추천한 댓글 ID 리스트
     * @author Jaeik
     * @since 2.0.0
     */
    List<Long> findUserLikedCommentIds(List<Long> commentIds, Long userId);
}