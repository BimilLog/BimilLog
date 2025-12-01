package jaeik.bimillog.domain.comment.repository;

import jaeik.bimillog.domain.comment.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * <h2>댓글 추천 레포지토리 인터페이스</h2>
 * <p>
 * 댓글 추천(`CommentLike`) 엔티티의 데이터베이스 작업을 처리하는 레포지토리 인터페이스
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    /**
     * <h3>댓글 ID와 사용자 ID로 추천 존재 여부 확인</h3>
     * <p>주어진 댓글 ID와 사용자 ID에 해당하는 추천이 존재하는지 확인합니다.</p>
     * <p>Spring Data JPA 네이밍 컨벤션을 사용하여 자동 쿼리 생성</p>
     *
     * @param commentId 댓글 ID
     * @param memberId 사용자 ID
     * @return boolean 추천이 존재하면 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    boolean existsByCommentIdAndMemberId(Long commentId, Long memberId);


    /**
     * <h3>특정 사용자가 주어진 댓글 목록 중 추천한 댓글의 ID 목록 조회.</h3>
     * @param memberId 특정 회원 ID
     * @param commentIds 조회할 댓글 ID 목록
     * @return 추천한 댓글의 ID 목록 (List<Long>)
     */
    List<Long> findComment_IdByMember_IdAndComment_IdIn(Long memberId, List<Long> commentIds);

    /**
     * <h3>댓글 ID와 사용자 ID로 추천 삭제</h3>
     * <p>주어진 댓글 ID와 사용자 ID에 해당하는 추천을 직접 삭제합니다.</p>
     *
     * @param commentId 댓글 ID
     * @param memberId 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Modifying
    @Query("DELETE FROM CommentLike cl WHERE cl.comment.id = :commentId AND cl.member.id = :memberId")
    void deleteByCommentIdAndMemberId(@Param("commentId") Long commentId, @Param("memberId") Long memberId);

}





