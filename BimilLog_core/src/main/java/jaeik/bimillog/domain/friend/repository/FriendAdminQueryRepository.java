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

import static jaeik.bimillog.infrastructure.redis.RedisKey.INTERACTION_SCORE_DEFAULT;

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

    public Map<Long, Map<Long, Double>> getInteractionScore(List<Long> memberIds) {
        List<Tuple> commentInteraction = jpaQueryFactory
                .select(comment.member.id, post.member.id, comment.count())
                .from(comment)
                .innerJoin(comment.post, post)
                .where(comment.member.id.in(memberIds), post.member.id.isNotNull(), comment.member.id.ne(post.member.id))
                .groupBy(comment.member.id, post.member.id)
                .fetch();

        List<Tuple> postLikeInteraction = jpaQueryFactory
                .select(postLike.member.id, post.member.id, postLike.count())
                .from(postLike)
                .innerJoin(postLike.post, post)
                .where(postLike.member.id.in(memberIds), post.member.id.isNotNull(), postLike.member.id.ne(post.member.id))
                .groupBy(postLike.member.id, post.member.id)
                .fetch();

        List<Tuple> commentLikeInteraction = jpaQueryFactory
                .select(commentLike.member.id, comment.member.id, commentLike.count())
                .from(commentLike)
                .innerJoin(commentLike.comment, comment)
                .where(commentLike.member.id.in(memberIds), comment.member.id.isNotNull(), commentLike.member.id.ne(comment.member.id))
                .groupBy(commentLike.member.id, comment.member.id)
                .fetch();

        Map<Long, Map<Long, Double>> result = new HashMap<>();

        for (Tuple row : commentInteraction) {
            Long actorId = row.get(0, Long.class);
            Long targetId = row.get(1, Long.class);
            Long count = row.get(2, Long.class);

            result.computeIfAbsent(actorId, k -> new HashMap<>())
                    .merge(targetId, count * INTERACTION_SCORE_DEFAULT, Double::sum);
        }

        for (Tuple row : postLikeInteraction) {
            Long actorId = row.get(0, Long.class);
            Long targetId = row.get(1, Long.class);
            Long count = row.get(2, Long.class);

            result.computeIfAbsent(actorId, k -> new HashMap<>())
                    .merge(targetId, count * INTERACTION_SCORE_DEFAULT, Double::sum);
        }

        for (Tuple row : commentLikeInteraction) {
            Long actorId = row.get(0, Long.class);
            Long targetId = row.get(1, Long.class);
            Long count = row.get(2, Long.class);

            result.computeIfAbsent(actorId, k -> new HashMap<>())
                    .merge(targetId, count * INTERACTION_SCORE_DEFAULT, Double::sum);
        }

        return result;
    }
}
