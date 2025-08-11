package jaeik.growfarm.domain.comment.infrastructure.adapter.out.persistence;

import jaeik.growfarm.domain.comment.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long>, CommentReadRepository {

    @Query("SELECT cl.comment.id FROM CommentLike cl WHERE cl.comment.id IN :commentIds AND cl.user.id = :userId")
    List<Long> findUserLikedCommentIds(@Param("commentIds") List<Long> commentIds, @Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Comment c SET c.user = null, c.content = '탈퇴한 사용자의 댓글입니다.' WHERE c.user.id = :userId")
    void anonymizeUserComments(@Param("userId") Long userId);
}




