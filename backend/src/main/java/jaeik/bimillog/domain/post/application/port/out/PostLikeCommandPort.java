package jaeik.bimillog.domain.post.application.port.out;

import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostLike;
import jaeik.bimillog.domain.user.entity.User;

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
     * <h3>게시글 추천 저장</h3>
     * <p>사용자의 게시글 추천 정보를 데이터베이스에 저장합니다.</p>
     * <p>PostInteractionService의 addLike 메서드에서 새로운 추천 생성 시 호출됩니다.</p>
     * <p>User-Post 조합의 유니크 제약조건을 통해 중복 추천을 방지하며, 추천 시간도 함께 기록됩니다.</p>
     * <p>저장 완료 후 추천 수 변경에 따른 캐시 무효화는 별도 이벤트로 처리됩니다.</p>
     *
     * @param postLike 저장할 게시글 추천 엔티티 (User, Post, 추천 시간 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    void save(PostLike postLike);

    /**
     * <h3>사용자별 게시글 추천 삭제</h3>
     * <p>특정 사용자가 특정 게시글에 대해 누른 추천을 삭제합니다.</p>
     * <p>PostInteractionService의 removeLike 메서드에서 추천 취소 시 호출됩니다.</p>
     * <p>User와 Post 엔티티를 조건으로 하여 해당 추천 레코드를 데이터베이스에서 제거합니다.</p>
     * <p>삭제 완료 후 추천 수 변경에 따른 캐시 무효화는 별도 이벤트로 처리됩니다.</p>
     *
     * @param user 추천을 취소할 사용자
     * @param post 추천을 취소할 게시글
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteByUserAndPost(User user, Post post);

    /**
     * <h3>게시글별 모든 추천 일괄 삭제</h3>
     * <p>특정 게시글에 대한 모든 사용자의 추천 데이터를 일괄 삭제합니다.</p>
     * <p>PostCommandService의 deletePost 메서드에서 게시글 삭제 전 관련 데이터 정리 시 호출됩니다.</p>
     * <p>CASCADE 삭제 설정이 없는 경우를 대비한 명시적인 데이터 정리 작업을 수행합니다.</p>
     * <p>다수의 추천 데이터를 한 번에 삭제하므로 트랜잭션 내에서 안전하게 처리됩니다.</p>
     *
     * @param postId 모든 추천을 삭제할 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteAllByPostId(Long postId);
}