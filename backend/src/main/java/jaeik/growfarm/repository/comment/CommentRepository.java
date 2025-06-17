package jaeik.growfarm.repository.comment;

import jaeik.growfarm.entity.comment.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>댓글 레포지토리</h2>
 * <p>댓글 관련 데이터베이스 작업을 수행하는 레포지토리</p>
 * <p>댓글 CRUD 및 커스텀 쿼리 메소드를 포함한다.</p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long>, CommentCustomRepository {

        /**
         * <h3>글의 댓글 개수 조회</h3>
         * <p>
         * 삭제된 댓글 포함하여 해당 글의 댓글 수를 조회합니다
         * </p>
         *
         * @param postId 게시글 ID
         * @return 전체 루트 댓글 개수
         * @author Jaeik
         * @since 1.0.0
         */
        int countByPostId(Long postId);

        /**
         * <h3>사용자 댓글 조회</h3>
         * <p>
         * 특정 사용자가 작성한 댓글을 페이지네이션하여 조회합니다.
         * </p>
         *
         * @param userId   사용자 ID
         * @param pageable 페이징 정보
         * @return 사용자 작성 댓글 페이지
         */
        Page<Comment> findByUserId(Long userId, Pageable pageable);

        /**
         * <h3>사용자가 추천한 댓글 조회</h3>
         * <p>
         * 특정 사용자가 추천한 댓글을 페이지네이션하여 조회합니다.
         * </p>
         *
         * @param userId   사용자 ID
         * @param pageable 페이징 정보
         * @return 추천한 댓글 페이지
         */
        Page<Comment> findByLikedComments(Long userId, Pageable pageable);

        /**
         * <h3>사용자가 추천한 댓글 배치 조회</h3>
         * <p>
         * 특정 사용자가 댓글 ID 리스트 중 추천한 댓글들을 조회한다.
         * </p>
         * 
         * @param commentIds 댓글 ID 리스트
         * @param userId     사용자 ID
         * @return 추천한 댓글 ID 리스트
         * @author Jaeik
         * @since 1.0.0
         */
        @Query("""
                        SELECT cl.comment.id
                        FROM CommentLike cl
                        WHERE cl.comment.id IN :commentIds
                        AND cl.user.id = :userId
                        """)
        List<Long> findUserLikedCommentIds(@Param("commentIds") List<Long> commentIds, @Param("userId") Long userId);

        /**
         * <h3>게시글 ID로 댓글 ID 리스트 조회</h3>
         * <p>
         * 특정 게시글에 달린 댓글들의 ID 리스트를 조회합니다.
         * </p>
         *
         * @param postId 게시글 ID
         * @return 댓글 ID 리스트
         * @author Jaeik
         * @since 1.0.0
         */
        List<Long> findCommentIdsByPostId(Long postId);
}
