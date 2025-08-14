package jaeik.growfarm.domain.comment.infrastructure.adapter.out.persistence;

import com.querydsl.core.Tuple;
import jaeik.growfarm.dto.comment.CommentDTO;
import jaeik.growfarm.dto.comment.SimpleCommentDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * <h2>댓글 조회 레포지토리 인터페이스</h2>
 * <p>
 * 댓글의 조회 작업을 위한 사용자 정의 레포지토리 인터페이스
 * </p>
 * <p>
 * QueryDSL을 사용하여 복잡한 조회 쿼리를 구현
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface CommentReadRepository {
    /**
     * <h3>최신순 댓글 조회</h3>
     * <p>주어진 게시글의 댓글을 최신순으로 페이지네이션하여 조회합니다. 사용자가 추천를 누른 댓글 정보도 포함합니다.</p>
     *
     * @param postId          게시글 ID
     * @param pageable        페이지 정보
     * @param likedCommentIds 사용자가 추천한 댓글 ID 목록
     * @return Page<CommentDTO> 최신순 댓글 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<CommentDTO> findCommentsWithLatestOrder(Long postId, Pageable pageable, List<Long> likedCommentIds);

    /**
     * <h3>인기 댓글 조회</h3>
     * <p>주어진 게시글의 인기 댓글 목록을 조회합니다. 사용자가 추천를 누른 댓글 정보도 포함합니다.</p>
     *
     * @param postId          게시글 ID
     * @param likedCommentIds 사용자가 추천한 댓글 ID 목록
     * @return List<CommentDTO> 인기 댓글 DTO 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<CommentDTO> findPopularComments(Long postId, List<Long> likedCommentIds);

    /**
     * <h3>게시글 ID로 루트 댓글 수 조회</h3>
     * <p>주어진 게시글 ID에 해당하는 최상위(루트) 댓글의 수를 조회합니다.</p>
     *
     * @param postId 게시글 ID
     * @return Long 루트 댓글의 수
     * @author Jaeik
     * @since 2.0.0
     */
    Long countRootCommentsByPostId(Long postId);

    /**
     * <h3>여러 게시글 ID에 대한 댓글 수 조회</h3>
     * <p>주어진 여러 게시글 ID에 해당하는 각 게시글의 댓글 수를 조회합니다.</p>
     *
     * @param postIds 게시글 ID 목록
     * @return Map<Long, Integer> 게시글 ID를 키로, 댓글 수를 값으로 하는 맵
     * @author Jaeik
     * @since 2.0.0
     */
    Map<Long, Integer> findCommentCountsByPostIds(List<Long> postIds);

    /**
     * <h3>사용자 추천한 댓글 목록 조회</h3>
     * <p>특정 사용자가 추천한 댓글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return Page<SimpleCommentDTO> 추천한 댓글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<SimpleCommentDTO> findLikedCommentsByUserId(Long userId, Pageable pageable);

    /**
     * <h3>사용자 작성 댓글 목록 조회</h3>
     * <p>특정 사용자가 작성한 댓글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return Page<SimpleCommentDTO> 작성한 댓글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<SimpleCommentDTO> findCommentsByUserId(Long userId, Pageable pageable);
}




