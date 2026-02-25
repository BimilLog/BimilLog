package jaeik.bimillog.domain.friend.repository;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.comment.entity.QComment;
import jaeik.bimillog.domain.comment.entity.QCommentLike;
import jaeik.bimillog.domain.friend.entity.jpa.QFriendship;
import jaeik.bimillog.domain.post.entity.jpa.QPost;
import jaeik.bimillog.domain.post.entity.jpa.QPostLike;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>친구 도메인 어드민 복구용 쿼리 레포지터리</h2>
 * <p>Redis 재구축 시 DB 데이터를 청크 단위로 조회합니다.</p>
 * <p>모든 쿼리는 테이블 PK id 기준 keyset 페이지네이션으로 순차 스캔합니다. GROUP BY 없음.</p>
 *
 * @author Jaeik
 * @version 2.8.0
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
     * <h3>친구 관계 쌍 청크 조회</h3>
     * <p>friendship.id 기준 오름차순 순차 스캔입니다.</p>
     * <p>반환 배열: [friendship.id, memberId, friendId]</p>
     *
     * @param afterId 마지막으로 처리한 friendship.id (첫 호출 시 0)
     * @param size    한 번에 조회할 최대 행 수
     * @return long[3][] — 각 요소: [id, memberId, friendId]
     */
    public List<long[]> getFriendshipPairsChunk(long afterId, int size) {
        return jpaQueryFactory
                .select(friendship.id, friendship.member.id, friendship.friend.id)
                .from(friendship)
                .where(friendship.id.gt(afterId))
                .orderBy(friendship.id.asc())
                .limit(size)
                .fetch()
                .stream()
                .map(t -> new long[]{
                        t.get(friendship.id),
                        t.get(friendship.member.id),
                        t.get(friendship.friend.id)
                })
                .toList();
    }

    /**
     * <h3>게시글 좋아요 상호작용 청크 조회</h3>
     * <p>post_like.id 기준 오름차순 순차 스캔입니다. GROUP BY 없이 원시 행을 그대로 반환합니다.</p>
     * <p>익명 게시글 및 자기자신 상호작용은 제외합니다.</p>
     * <p>반환 배열: [postLike.id, likerId, postAuthorId]</p>
     *
     * @param afterId 마지막으로 처리한 post_like.id (첫 호출 시 0)
     * @param size    한 번에 조회할 최대 행 수
     */
    public List<long[]> getPostLikeInteractionsChunk(long afterId, int size) {
        return jpaQueryFactory
                .select(postLike.id, postLike.member.id, post.member.id)
                .from(postLike)
                .join(postLike.post, post)
                .where(
                        post.member.id.isNotNull(),
                        postLike.member.id.isNotNull(),
                        postLike.member.id.ne(post.member.id),
                        postLike.id.gt(afterId)
                )
                .orderBy(postLike.id.asc())
                .limit(size)
                .fetch()
                .stream()
                .map(t -> new long[]{
                        t.get(postLike.id),
                        t.get(postLike.member.id),
                        t.get(post.member.id)
                })
                .toList();
    }

    /**
     * <h3>댓글 작성 상호작용 청크 조회</h3>
     * <p>comment.id 기준 오름차순 순차 스캔입니다. GROUP BY 없이 원시 행을 그대로 반환합니다.</p>
     * <p>익명 댓글/게시글 및 자기자신 상호작용은 제외합니다.</p>
     * <p>반환 배열: [comment.id, commenterId, postAuthorId]</p>
     *
     * @param afterId 마지막으로 처리한 comment.id (첫 호출 시 0)
     * @param size    한 번에 조회할 최대 행 수
     */
    public List<long[]> getCommentInteractionsChunk(long afterId, int size) {
        return jpaQueryFactory
                .select(comment.id, comment.member.id, post.member.id)
                .from(comment)
                .join(comment.post, post)
                .where(
                        comment.member.id.isNotNull(),
                        post.member.id.isNotNull(),
                        comment.member.id.ne(post.member.id),
                        comment.id.gt(afterId)
                )
                .orderBy(comment.id.asc())
                .limit(size)
                .fetch()
                .stream()
                .map(t -> new long[]{
                        t.get(comment.id),
                        t.get(comment.member.id),
                        t.get(post.member.id)
                })
                .toList();
    }

    /**
     * <h3>댓글 좋아요 상호작용 청크 조회</h3>
     * <p>comment_like.id 기준 오름차순 순차 스캔입니다. GROUP BY 없이 원시 행을 그대로 반환합니다.</p>
     * <p>익명 댓글 및 자기자신 상호작용은 제외합니다.</p>
     * <p>반환 배열: [commentLike.id, likerId, commentAuthorId]</p>
     *
     * @param afterId 마지막으로 처리한 comment_like.id (첫 호출 시 0)
     * @param size    한 번에 조회할 최대 행 수
     */
    public List<long[]> getCommentLikeInteractionsChunk(long afterId, int size) {
        return jpaQueryFactory
                .select(commentLike.id, commentLike.member.id, comment.member.id)
                .from(commentLike)
                .join(commentLike.comment, comment)
                .where(
                        comment.member.id.isNotNull(),
                        commentLike.member.id.isNotNull(),
                        commentLike.member.id.ne(comment.member.id),
                        commentLike.id.gt(afterId)
                )
                .orderBy(commentLike.id.asc())
                .limit(size)
                .fetch()
                .stream()
                .map(t -> new long[]{
                        t.get(commentLike.id),
                        t.get(commentLike.member.id),
                        t.get(comment.member.id)
                })
                .toList();
    }
}
