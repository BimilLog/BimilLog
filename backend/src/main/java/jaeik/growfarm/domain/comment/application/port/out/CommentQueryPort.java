package jaeik.growfarm.domain.comment.application.port.out;

import jaeik.growfarm.domain.comment.entity.Comment;
import jaeik.growfarm.domain.comment.entity.CommentInfo;
import jaeik.growfarm.domain.comment.entity.SimpleCommentInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <h2>댓글 조회 포트</h2>
 * <p>댓글 엔티티 조회를 위한 Out-Port</p>
 * <p>CQRS 패턴에 따른 조회 전용 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface CommentQueryPort {

    /**
     * <h3>인기 댓글 목록 조회</h3>
     * <p>주어진 게시글 ID에 대한 인기 댓글 목록을 조회합니다.</p>
     *
     * @param postId 게시글 ID
     * @param userId 사용자 ID (추천 여부 확인용, null 가능)
     * @return List<CommentInfo> 인기 댓글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<CommentInfo> findPopularComments(Long postId, Long userId);

    /**
     * <h3>게시글 ID 목록에 대한 댓글 수 조회 (배치)</h3>
     * <p>여러 게시글의 댓글 수를 배치로 조회하여 N+1 문제를 해결합니다.</p>
     *
     * @param postIds 게시글 ID 목록
     * @return Map<Long, Integer> 게시글 ID를 키로, 댓글 수를 값으로 하는 맵
     * @author Jaeik
     * @since 2.0.0
     */
    Map<Long, Integer> findCommentCountsByPostIds(List<Long> postIds);

    /**
     * <h3>단일 게시글의 댓글 수 조회</h3>
     * <p>단일 게시글의 댓글 수를 조회합니다.</p>
     *
     * @param postId 게시글 ID
     * @return Integer 댓글 수
     * @author Jaeik
     * @since 2.0.0
     */
    Integer countByPostId(Long postId);

    /**
     * <h3>과거순 댓글 목록 조회</h3>
     * <p>주어진 게시글 ID에 대한 댓글을 과거순으로 페이지네이션하여 조회합니다.</p>
     *
     * @param postId   게시글 ID
     * @param pageable 페이지 정보
     * @param userId   사용자 ID (추천 여부 확인용, null 가능)
     * @return Page<CommentInfo> 과거순 댓글 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<CommentInfo> findCommentsWithOldestOrder(Long postId, Pageable pageable, Long userId);

    /**
     * <h3>ID로 댓글 조회</h3>
     * <p>주어진 ID로 댓글을 조회합니다.</p>
     *
     * @param id 댓글 ID
     * @return Optional<Comment> 조회된 댓글 엔티티. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<Comment> findById(Long id);

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