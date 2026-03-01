package jaeik.bimillog.domain.friend.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.comment.entity.QComment;
import jaeik.bimillog.domain.comment.entity.QCommentLike;
import jaeik.bimillog.domain.friend.entity.jpa.QFriendship;
import jaeik.bimillog.domain.member.entity.QMember;
import jaeik.bimillog.domain.post.entity.jpa.QPost;
import jaeik.bimillog.domain.post.entity.jpa.QPostLike;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import com.querydsl.core.Tuple;

import java.util.*;

/**
 * <h2>친구 도메인 어드민 복구용 쿼리 레포지터리</h2>
 * <p>Redis 재구축 시 DB 데이터를 청크 단위로 조회합니다.</p>
 * <p>friendship은 PK 단일 keyset, 상호작용 3개는 익명 행 스캔을 피하기 위해
 * 비익명 테이블(post/comment)을 드라이빙으로 삼고 (driveId, joinId) 복합 keyset을 사용합니다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class FriendAdminQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    private static final QFriendship friendship = QFriendship.friendship;
    private static final QPostLike postLike = QPostLike.postLike;
    private static final QPost post = QPost.post;
    private static final QComment comment = QComment.comment;
    private static final QCommentLike commentLike = QCommentLike.commentLike;
    private static final QMember member = QMember.member;

    public List<Long> getMemberId() {
        return jpaQueryFactory
                .select(member.id)
                .from(member)
                .fetch();
    }

    /**
     * <h3>memberId 배치의 친구 관계 일괄 조회</h3>
     * <p>friendship 테이블을 양방향으로 조회하여 Map&lt;memberId, Set&lt;friendId&gt;&gt;로 반환합니다.</p>
     *
     * @param memberIds 조회할 memberId 목록
     */
    public Map<Long, Set<Long>> getMemberFriendBatch(List<Long> memberIds) {
        List<Tuple> memberAsMainRows = jpaQueryFactory
                .select(friendship.member.id, friendship.friend.id)
                .from(friendship)
                .where(friendship.member.id.in(memberIds))
                .fetch();

        List<Tuple> memberAsFriendRows = jpaQueryFactory
                .select(friendship.friend.id, friendship.member.id)
                .from(friendship)
                .where(friendship.friend.id.in(memberIds))
                .fetch();

        Map<Long, Set<Long>> result = new HashMap<>();
        for (Long memberId : memberIds) {
            result.put(memberId, new HashSet<>());
        }

        for (Tuple tuple : memberAsMainRows) {
            Long memberId = tuple.get(friendship.member.id);
            Long friendId = tuple.get(friendship.friend.id);
            result.computeIfAbsent(memberId, k -> new HashSet<>()).add(friendId);
        }

        for (Tuple tuple : memberAsFriendRows) {
            Long memberId = tuple.get(friendship.friend.id);
            Long friendId = tuple.get(friendship.member.id);
            result.computeIfAbsent(memberId, k -> new HashSet<>()).add(friendId);
        }

        return result;
    }

//    public Map<Long, Map<Long, Double>> getInteractionScore(List<Long> memberIds) {
//
//
//    }


    /**
     * <h3>게시글 좋아요 상호작용 청크 조회</h3>
     * <p>드라이빙: post (member_id IS NOT NULL) → post_like 조인.</p>
     * <p>익명 게시글 행을 드라이빙 단계에서 제외하므로 post_like의 불필요한 조인을 피합니다.</p>
     * <p>복합 keyset (post.id, postLike.id)으로 청크 경계의 누락 없이 순회합니다.</p>
     * <p>반환 배열: [post.id, postLike.id, likerId, postAuthorId]</p>
     *
     * @param afterDriveId 마지막으로 처리한 post.id (첫 호출 시 0)
     * @param afterJoinId  마지막으로 처리한 postLike.id (첫 호출 시 0)
     * @param size         한 번에 조회할 최대 행 수
     */
    public List<long[]> getPostLikeInteractionsChunk(long afterDriveId, long afterJoinId, int size) {
        List<long[]> list = jpaQueryFactory
                .select(post.id, postLike.id, postLike.member.id, post.member.id)
                .from(post)
                .join(postLike).on(postLike.post.eq(post))
                .where(
                        post.member.id.isNotNull(),
                        postLike.member.id.isNotNull(),
                        postLike.member.id.ne(post.member.id),
                        post.id.gt(afterDriveId)
                                .or(post.id.eq(afterDriveId).and(postLike.id.gt(afterJoinId)))
                )
                .orderBy(post.id.asc(), postLike.id.asc())
                .limit(size)
                .fetch()
                .stream()
                .map(t -> new long[]{
                        t.get(post.id),
                        t.get(postLike.id),
                        t.get(postLike.member.id),
                        t.get(post.member.id)
                })
                .toList();
        log.info("DB 게시글 추천 조회 : {}이후 {}개 조회 완료", afterJoinId, size);
        return list;
    }

    /**
     * <h3>댓글 작성 상호작용 청크 조회</h3>
     * <p>드라이빙: post (member_id IS NOT NULL) → comment 조인.</p>
     * <p>익명 게시글 행을 드라이빙 단계에서 제외하므로 comment의 불필요한 조인을 피합니다.</p>
     * <p>복합 keyset (post.id, comment.id)으로 청크 경계의 누락 없이 순회합니다.</p>
     * <p>반환 배열: [post.id, comment.id, commenterId, postAuthorId]</p>
     *
     * @param afterDriveId 마지막으로 처리한 post.id (첫 호출 시 0)
     * @param afterJoinId  마지막으로 처리한 comment.id (첫 호출 시 0)
     * @param size         한 번에 조회할 최대 행 수
     */
    public List<long[]> getCommentInteractionsChunk(long afterDriveId, long afterJoinId, int size) {
        List<long[]> list = jpaQueryFactory
                .select(post.id, comment.id, comment.member.id, post.member.id)
                .from(post)
                .join(comment).on(comment.post.eq(post))
                .where(
                        post.member.id.isNotNull(),
                        comment.member.id.isNotNull(),
                        comment.member.id.ne(post.member.id),
                        post.id.gt(afterDriveId)
                                .or(post.id.eq(afterDriveId).and(comment.id.gt(afterJoinId)))
                )
                .orderBy(post.id.asc(), comment.id.asc())
                .limit(size)
                .fetch()
                .stream()
                .map(t -> new long[]{
                        t.get(post.id),
                        t.get(comment.id),
                        t.get(comment.member.id),
                        t.get(post.member.id)
                })
                .toList();
        log.info("DB 댓글 조회 : {}이후 {}개 조회 완료", afterJoinId, size);
        return list;
    }

    /**
     * <h3>댓글 좋아요 상호작용 청크 조회</h3>
     * <p>드라이빙: comment (member_id IS NOT NULL) → comment_like 조인.</p>
     * <p>익명 댓글 행을 드라이빙 단계에서 제외하므로 comment_like의 불필요한 조인을 피합니다.</p>
     * <p>복합 keyset (comment.id, commentLike.id)으로 청크 경계의 누락 없이 순회합니다.</p>
     * <p>반환 배열: [comment.id, commentLike.id, likerId, commentAuthorId]</p>
     *
     * @param afterDriveId 마지막으로 처리한 comment.id (첫 호출 시 0)
     * @param afterJoinId  마지막으로 처리한 commentLike.id (첫 호출 시 0)
     * @param size         한 번에 조회할 최대 행 수
     */
    public List<long[]> getCommentLikeInteractionsChunk(long afterDriveId, long afterJoinId, int size) {
        List<long[]> list = jpaQueryFactory
                .select(comment.id, commentLike.id, commentLike.member.id, comment.member.id)
                .from(comment)
                .join(commentLike).on(commentLike.comment.eq(comment))
                .where(
                        comment.member.id.isNotNull(),
                        commentLike.member.id.isNotNull(),
                        commentLike.member.id.ne(comment.member.id),
                        comment.id.gt(afterDriveId)
                                .or(comment.id.eq(afterDriveId).and(commentLike.id.gt(afterJoinId)))
                )
                .orderBy(comment.id.asc(), commentLike.id.asc())
                .limit(size)
                .fetch()
                .stream()
                .map(t -> new long[]{
                        t.get(comment.id),
                        t.get(commentLike.id),
                        t.get(commentLike.member.id),
                        t.get(comment.member.id)
                })
                .toList();
        log.info("DB 댓글 추천 조회 : {}이후 {}개 조회 완료", afterJoinId, size);
        return list;
    }
}
