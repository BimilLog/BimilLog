package jaeik.bimillog.domain.post.out;

import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <h2>PostLikeRepository</h2>
 * <p>PostLike 엔티티 JPA Repository 인터페이스입니다.</p>
 * <p>PostLikeCommandAdapter와 PostLikeQueryAdapter에서 좋아요 관련 작업 시 호출됩니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    /**
     * <h3>사용자와 게시글로 추천 삭제</h3>
     * <p>특정 사용자의 특정 게시글에 대한 추천를 삭제합니다.</p>
     *
     * @param member 사용자 엔티티
     * @param post 게시글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteByMemberAndPost(Member member, Post post);

    /**
     * PostLike 엔티티가 주어진 postId와 memberId에 대해 존재하는지 확인합니다.
     *
     * @param postId 게시글 ID (PostLike.post.id)
     * @param memberId 사용자 ID (PostLike.member.id)
     * @return 추천 관계가 존재하면 true, 아니면 false
     */
    boolean existsByPostIdAndMemberId(Long postId, Long memberId);
}

