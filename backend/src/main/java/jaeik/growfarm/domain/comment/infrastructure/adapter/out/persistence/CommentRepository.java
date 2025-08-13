package jaeik.growfarm.domain.comment.infrastructure.adapter.out.persistence;

import jaeik.growfarm.domain.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import jaeik.growfarm.domain.post.entity.Post;

/**
 * <h2>댓글 레포지토리 인터페이스</h2>
 * <p>
 * 댓글(`Comment`) 엔티티의 데이터베이스 작업을 처리하는 레포지토리 인터페이스
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long>, CommentReadRepository {

    /**
     * <h3>게시글로 댓글 목록 조회</h3>
     * <p>주어진 게시글에 속한 모든 댓글 목록을 조회합니다.</p>
     *
     * @param post 게시글 엔티티
     * @return List<Comment> 댓글 엔티티 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<Comment> findByPost(Post post);

    /**
     * <h3>사용자가 좋아요한 댓글 ID 목록 조회</h3>
     * <p>주어진 댓글 ID 목록 중 사용자가 좋아요를 누른 댓글의 ID 목록을 조회합니다.</p>
     *
     * @param commentIds 댓글 ID 목록
     * @param userId     사용자 ID
     * @return List<Long> 사용자가 좋아요를 누른 댓글 ID 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Query("SELECT cl.comment.id FROM CommentLike cl WHERE cl.comment.id IN :commentIds AND cl.user.id = :userId")
    List<Long> findUserLikedCommentIds(@Param("commentIds") List<Long> commentIds, @Param("userId") Long userId);

    /**
     * <h3>게시글 ID로 사용자가 좋아요한 댓글 ID 목록 조회</h3>
     * <p>주어진 게시글 ID에 대한 댓글 중 사용자가 좋아요를 누른 댓글의 ID 목록을 조회합니다.</p>
     *
     * @param postId 게시글 ID
     * @param userId 사용자 ID
     * @return List<Long> 사용자가 좋아요를 누른 댓글 ID 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Query("SELECT cl.comment.id FROM CommentLike cl WHERE cl.comment.post.id = :postId AND cl.user.id = :userId")
    List<Long> findUserLikedCommentIdsByPostId(@Param("postId") Long postId, @Param("userId") Long userId);

    /**
     * <h3>사용자 댓글 익명화</h3>
     * <p>특정 사용자가 작성한 모든 댓글을 익명화 처리합니다. (사용자 탈퇴 시 호출)</p>
     *
     * @param userId 익명화할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Modifying
    @Query("UPDATE Comment c SET c.user = null, c.content = '탈퇴한 사용자의 댓글입니다.' WHERE c.user.id = :userId")
    void anonymizeUserComments(@Param("userId") Long userId);

    /**
     * <h3>게시글 ID로 모든 댓글 삭제</h3>
     * <p>주어진 게시글 ID에 해당하는 모든 댓글을 삭제합니다.</p>
     *
     * @param postId 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Modifying
    @Query("DELETE FROM Comment c WHERE c.post.id = :postId")
    void deleteAllByPostId(@Param("postId") Long postId);
}





