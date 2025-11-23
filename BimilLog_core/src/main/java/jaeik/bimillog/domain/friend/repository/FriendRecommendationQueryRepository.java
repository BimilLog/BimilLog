package jaeik.bimillog.domain.friend.repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.comment.entity.QComment;
import jaeik.bimillog.domain.comment.entity.QCommentLike;
import jaeik.bimillog.domain.friend.entity.RecommendedFriend;
import jaeik.bimillog.domain.friend.entity.jpa.QFriendRecommendation;
import jaeik.bimillog.domain.post.entity.QPost;
import jaeik.bimillog.domain.post.entity.QPostLike;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class FriendRecommendationQueryRepository {
    private final JPAQueryFactory jpaQueryFactory;
    private final QFriendRecommendation friendRecommendation = QFriendRecommendation.friendRecommendation;

    public Page<RecommendedFriend> getRecommendFriendList(Long memberId, Pageable pageable) {
        List<RecommendedFriend> recommendedFriends = jpaQueryFactory
                .select(Projections.constructor(RecommendedFriend.class,
                        friendRecommendation.recommendMember.id,
                        friendRecommendation.acquaintanceId,
                        friendRecommendation.manyAcquaintance,
                        friendRecommendation.depth
                        ))
                .from(friendRecommendation)
                .where(friendRecommendation.member.id.eq(memberId))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(friendRecommendation.score.desc())
                .fetch();

        Long total = jpaQueryFactory
                .select(friendRecommendation.count())
                .from(friendRecommendation)
                .where(friendRecommendation.member.id.eq(memberId))
                .fetchOne();

        return new PageImpl<>(recommendedFriends, pageable, total != null ? total : 0);
    }

    /**
     * 특정 멤버와 상호작용한 멤버들의 상호작용 점수를 계산합니다.
     * 상호작용은 다음 3가지 행동으로 구성됩니다:
     * 1. 내 게시글을 추천
     * 2. 내 댓글을 추천
     * 3. 내 게시글에 댓글 작성
     *
     * 각 행동당 1점이며, 최대 10점으로 제한됩니다.
     *
     * @param targetMemberId 대상 멤버 ID
     * @param candidateIds 상호작용 점수를 계산할 후보 멤버 ID 목록 (null이면 전체 멤버 대상)
     * @return 멤버 ID별 상호작용 점수 (최대 10점)
     */
    public Map<Long, Integer> findInteractionScores(Long targetMemberId, Set<Long> candidateIds) {
        QPost post = QPost.post;
        QPostLike postLike = QPostLike.postLike;
        QComment comment = QComment.comment;
        QCommentLike commentLike = QCommentLike.commentLike;

        Map<Long, Integer> scoreMap = new HashMap<>();

        // 1. 게시글 추천: 내 게시글을 추천한 사람들
        // SELECT pl.member_id, COUNT(*) FROM post_like pl
        // JOIN post p ON pl.post_id = p.post_id
        // WHERE p.member_id = :targetMemberId AND pl.member_id IN (:candidateIds)
        // GROUP BY pl.member_id
        List<Tuple> postLikeResults = jpaQueryFactory
                .select(postLike.member.id, postLike.count())
                .from(postLike)
                .join(postLike.post, post)
                .where(
                        post.member.id.eq(targetMemberId),
                        postLike.member.id.ne(targetMemberId), // 본인 제외
                        candidateIds != null ? postLike.member.id.in(candidateIds) : null
                )
                .groupBy(postLike.member.id)
                .fetch();

        for (Tuple result : postLikeResults) {
            Long memberId = result.get(0, Long.class);
            Long count = result.get(1, Long.class);
            scoreMap.merge(memberId, count.intValue(), Integer::sum);
        }

        // 2. 댓글 추천: 내 댓글을 추천한 사람들
        // SELECT cl.member_id, COUNT(*) FROM comment_like cl
        // JOIN comment c ON cl.comment_id = c.comment_id
        // WHERE c.member_id = :targetMemberId AND cl.member_id IN (:candidateIds)
        // GROUP BY cl.member_id
        List<Tuple> commentLikeResults = jpaQueryFactory
                .select(commentLike.member.id, commentLike.count())
                .from(commentLike)
                .join(commentLike.comment, comment)
                .where(
                        comment.member.id.eq(targetMemberId),
                        commentLike.member.id.ne(targetMemberId), // 본인 제외
                        candidateIds != null ? commentLike.member.id.in(candidateIds) : null
                )
                .groupBy(commentLike.member.id)
                .fetch();

        for (Tuple result : commentLikeResults) {
            Long memberId = result.get(0, Long.class);
            Long count = result.get(1, Long.class);
            scoreMap.merge(memberId, count.intValue(), Integer::sum);
        }

        // 3. 댓글 작성: 내 게시글에 댓글을 단 사람들
        // SELECT c.member_id, COUNT(*) FROM comment c
        // JOIN post p ON c.post_id = p.post_id
        // WHERE p.member_id = :targetMemberId AND c.member_id IN (:candidateIds)
        // GROUP BY c.member_id
        List<Tuple> commentResults = jpaQueryFactory
                .select(comment.member.id, comment.count())
                .from(comment)
                .join(comment.post, post)
                .where(
                        post.member.id.eq(targetMemberId),
                        comment.member.id.isNotNull(), // 익명 댓글 제외
                        comment.member.id.ne(targetMemberId), // 본인 제외
                        candidateIds != null ? comment.member.id.in(candidateIds) : null
                )
                .groupBy(comment.member.id)
                .fetch();

        for (Tuple result : commentResults) {
            Long memberId = result.get(0, Long.class);
            Long count = result.get(1, Long.class);
            scoreMap.merge(memberId, count.intValue(), Integer::sum);
        }

        // 상한 10점 적용
        scoreMap.replaceAll((memberId, score) -> Math.min(score, 10));

        return scoreMap;
    }
}
