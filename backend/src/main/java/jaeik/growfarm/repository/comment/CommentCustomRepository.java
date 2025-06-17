package jaeik.growfarm.repository.comment;

import com.querydsl.core.Tuple;
import jaeik.growfarm.entity.comment.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * <h2>커스텀 댓글 저장소 인터페이스</h2>
 * <p>
 * 댓글 관련 데이터베이스 작업을 수행하며 커스텀한 쿼리메소드가 포함되어 있습니다.
 * </p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Repository
public interface CommentCustomRepository {

    Page<Comment> findByLikedComments(Long userId, Pageable pageable);

    /**
     * <h3>게시글별 댓글 수 조회</h3>
     * <p>
     * 게시글 ID 리스트로 각 게시글의 댓글 수를 조회한다.
     * </p>
     *
     * @param postIds 게시글 ID 리스트
     * @return 게시글 ID와 댓글 수의 맵
     * @author Jaeik
     * @since 1.0.0
     */
    Map<Long, Integer> findCommentCountsByPostIds(List<Long> postIds);

    /**
     * <h3>인기댓글 조회</h3>
     * <p>
     * 해당 게시글의 댓글 중에서 추천수 3개 이상인 상위 3개를 조회한다.
     * </p>
     *
     * @param postId 게시글 ID
     * @return 인기댓글 리스트 (추천수 포함)
     * @author Jaeik
     * @since 1.0.0
     */
    List<Tuple> findPopularComments(Long postId);

    /**
     * <h3>일반 댓글 조회 (최신순)</h3>
     * <p>
     * 루트댓글을 최신순으로 조회하고 자손댓글도 함께 반환한다.
     * </p>
     *
     * @param postId   게시글 ID
     * @param pageable 페이징 정보
     * @return 최신순 정렬된 루트 댓글과 자손댓글 리스트
     * @author Jaeik
     * @since 1.0.0
     */
    List<Tuple> findCommentsWithLatestOrder(Long postId, Pageable pageable);

    /**
     * <h3>루트 댓글 총 개수 조회</h3>
     * <p>
     * 페이징을 위한 전체 루트 댓글 개수를 조회한다.
     * </p>
     *
     * @param postId 게시글 ID
     * @return 전체 루트 댓글 개수
     * @author Jaeik
     * @since 1.0.0
     */
    Long countRootCommentsByPostId(Long postId);
}
