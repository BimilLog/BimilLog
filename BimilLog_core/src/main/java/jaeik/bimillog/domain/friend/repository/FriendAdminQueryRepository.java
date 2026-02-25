package jaeik.bimillog.domain.friend.repository;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.comment.entity.QComment;
import jaeik.bimillog.domain.comment.entity.QCommentLike;
import jaeik.bimillog.domain.friend.entity.jpa.QFriendship;
import jaeik.bimillog.domain.friend.service.FriendAdminService;
import jaeik.bimillog.domain.post.entity.jpa.QPost;
import jaeik.bimillog.domain.post.entity.jpa.QPostLike;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>친구 도메인 어드민 복구용 쿼리 레포지터리</h2>
 * <p>Redis 재구축 시 DB 데이터를 조회합니다.</p>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@Repository
@RequiredArgsConstructor
public class FriendAdminQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    private static final QFriendship friendship = QFriendship.friendship;
    private static final QPostLike postLike = QPostLike.postLike;
    private static final QPost post = QPost.post;
    private static final QComment comment = QComment.comment;
    private static final QCommentLike commentLike = QCommentLike.commentLike;

    /**
     * <h3>전체 친구 관계 쌍 조회</h3>
     * <p>friendship 테이블에서 모든 (memberId, friendId) 쌍을 조회합니다.</p>
     *
     * @return List of long[] — 각 요소: [memberId, friendId]
     */
    public List<long[]> getAllFriendshipPairs() {
        List<Tuple> results = jpaQueryFactory
                .select(friendship.member.id, friendship.friend.id)
                .from(friendship)
                .fetch();

        return results.stream()
                .map(t -> new long[]{t.get(friendship.member.id), t.get(friendship.friend.id)})
                .toList();
    }

    /**
     * <h3>게시글 좋아요 상호작용 집계 조회</h3>
     * <p>post_like → post 조인 후, 좋아요 누른 사람과 게시글 작성자 간 상호작용 횟수를 집계합니다.</p>
     * <p>익명 게시글(post.member IS NULL) 및 자기자신 상호작용은 제외합니다.</p>
     *
     * @return List of long[] — 각 요소: [likerId, authorId, count]
     */
    public List<FriendAdminService.InteractionData> getPostLikeInteractions() {
        List<Tuple> results = jpaQueryFactory
                .select(postLike.member.id, post.member.id, postLike.count())
                .from(postLike)
                .join(postLike.post, post)
                .where(
                        post.member.id.isNotNull(),
                        postLike.member.id.isNotNull(),
                        postLike.member.id.ne(post.member.id)
                )
                .groupBy(postLike.member.id, post.member.id)
                .fetch();

        return results.stream()
                .map(t -> new FriendAdminService.InteractionData(
                        t.get(postLike.member.id),
                        t.get(post.member.id),
                        t.get(postLike.count())
                ))
                .toList();
    }

    /**
     * <h3>댓글 작성 상호작용 집계 조회</h3>
     * <p>comment → post 조인 후, 댓글 작성자와 게시글 작성자 간 상호작용 횟수를 집계합니다.</p>
     * <p>익명 댓글/게시글 및 자기자신 상호작용은 제외합니다.</p>
     *
     * @return List of long[] — 각 요소: [commenterId, postAuthorId, count]
     */
    public List<FriendAdminService.InteractionData> getCommentInteractions() {
        List<Tuple> results = jpaQueryFactory
                .select(comment.member.id, post.member.id, comment.count())
                .from(comment)
                .join(comment.post, post)
                .where(
                        comment.member.id.isNotNull(),
                        post.member.id.isNotNull(),
                        comment.member.id.ne(post.member.id)
                )
                .groupBy(comment.member.id, post.member.id)
                .fetch();

        return results.stream()
                .map(t -> new FriendAdminService.InteractionData(
                        t.get(comment.member.id),
                        t.get(post.member.id),
                        t.get(comment.count())
                ))
                .toList();
    }

    /**
     * <h3>댓글 좋아요 상호작용 집계 조회</h3>
     * <p>comment_like → comment 조인 후, 좋아요 누른 사람과 댓글 작성자 간 상호작용 횟수를 집계합니다.</p>
     * <p>익명 댓글 및 자기자신 상호작용은 제외합니다.</p>
     *
     * @return List of long[] — 각 요소: [likerId, commentAuthorId, count]
     */
    public List<FriendAdminService.InteractionData> getCommentLikeInteractions() {
        List<Tuple> results = jpaQueryFactory
                .select(commentLike.member.id, comment.member.id, commentLike.count())
                .from(commentLike)
                .join(commentLike.comment, comment)
                .where(
                        comment.member.id.isNotNull(),
                        commentLike.member.id.isNotNull(),
                        commentLike.member.id.ne(comment.member.id)
                )
                .groupBy(commentLike.member.id, comment.member.id)
                .fetch();

        return results.stream()
                .map(t -> new FriendAdminService.InteractionData(
                        t.get(commentLike.member.id),
                        t.get(comment.member.id),
                        t.get(commentLike.count())
                ))
                .toList();
    }
}
