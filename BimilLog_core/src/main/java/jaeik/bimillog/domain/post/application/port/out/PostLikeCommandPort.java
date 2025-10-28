package jaeik.bimillog.domain.post.application.port.out;

import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostLike;

/**
 * <h2>게시글 추천 명령 포트</h2>
 * <p>Post 도메인의 게시글 추천 명령 작업을 담당하는 포트입니다.</p>
 * <p>추천 데이터 생성과 삭제</p>
 * <p>사용자별 추천 중복 방지</p>
 * <p>게시글 삭제 시 관련 추천 데이터 정리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PostLikeCommandPort {

    /**
     * <h3>글 추천 저장</h3>
     * <p>사용자의 게시글 추천을 데이터베이스에 저장합니다.</p>
     * <p>PostInteractionService의 addLike 메서드에서 새로운 추천 생성 시 호출됩니다.</p>
     *
     * @param postLike 저장할 게시글 추천 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    void savePostLike(PostLike postLike);

    /**
     * <h3>글 추천 삭제</h3>
     * <p>사용자가 특정 게시글에 대해 누른 추천을 삭제합니다.</p>
     * <p>PostInteractionService의 removeLike 메서드에서 추천 취소 시 호출됩니다.</p>
     *
     * @param member 추천을 취소할 사용자
     * @param post 추천을 취소할 게시글
     * @author Jaeik
     * @since 2.0.0
     */
    void deletePostLike(Member member, Post post);

    /**
     * <h3>게시글의 모든 추천 일괄 삭제</h3>
     * <p>특정 게시글에 대한 추천 데이터를 일괄 삭제합니다.</p>
     *
     * @param postId 모든 추천을 삭제할 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deletePostLikeByPostId(Long postId);
}