package jaeik.bimillog.domain.comment.repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.comment.entity.*;
import jaeik.bimillog.domain.comment.service.CommentQueryService;
import jaeik.bimillog.domain.member.entity.QMember;
import jaeik.bimillog.domain.member.entity.QMemberBlacklist;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <h2>댓글 쿼리 어댑터</h2>
 * <p>댓글 조회 포트의 구현체입니다.</p>
 * <p>댓글 단건 조회, 인기 댓글 조회, 과거순 댓글 조회</p>
 * <p>사용자별 댓글 조회, 게시글별 댓글 수 조회</p>
 * <p>QueryDSL을 사용한 복잡한 JOIN 쿼리와 페이지네이션 처리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class CommentQueryRepository {
    private final JPAQueryFactory jpaQueryFactory;
    private static final QComment comment = QComment.comment;
    private static final QCommentLike commentLike = QCommentLike.commentLike;
    private static final QCommentClosure closure = QCommentClosure.commentClosure;
    private static final QMember member = QMember.member;
    private static final QMemberBlacklist memberBlacklist = QMemberBlacklist.memberBlacklist;

    /**
     * <h3>댓글 조회</h3>
     * <p>주어진 게시글의 댓글을 과거순 페이지네이션하여 조회합니다.</p>
     *
     * @param postId   게시글 ID
     * @param pageable 페이지 정보
     * @param memberId 사용자 ID (추천 여부 확인용, null 가능)
     * @return Page<CommentInfo> 과거순 댓글 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    public Page<CommentInfo> findComments(Long postId, Pageable pageable, Long memberId) {
        QCommentClosure parentClosure = new QCommentClosure("parentClosure");
        QCommentLike userCommentLike = new QCommentLike("userCommentLike");

        // 쿼리 빌딩
        JPAQuery<CommentInfo> query = getCommentInfoJPAQuery(memberId, parentClosure, userCommentLike);

        // 쿼리 실행
        List<CommentInfo> content = query
                .where(applyBlacklistFilter(comment.post.id.eq(postId), memberId))
                .groupBy(comment.id, member.memberName, comment.createdAt,
                        parentClosure.ancestor.id, userCommentLike.id)
                .orderBy(comment.createdAt.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = countRootCommentsByPostId(postId, memberId);
        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * <h3>인기 댓글 조회</h3>
     * <p>주어진 게시글의 인기 댓글 목록을 조회합니다.</p>
     * <p>추천 수가 높은 댓글들을 우선순위로 정렬하여 반환합니다.</p>
     *
     * @param memberId 사용자 ID (추천 여부 확인용, null 가능)
     * @return List<CommentInfo> 인기 댓글 정보 목록
     * @author Jaeik
     * @since 2.0.0
     */
    public List<CommentInfo> findPopularComments(Long postId, Long memberId) {
        // N+1 문제 해결을 위한 별도 Q타입 생성
        QCommentClosure parentClosure = new QCommentClosure("parentClosure");
        QCommentLike userCommentLike = new QCommentLike("userCommentLike");

        // 쿼리 빌딩
        JPAQuery<CommentInfo> query = getCommentInfoJPAQuery(memberId, parentClosure, userCommentLike);

        // 쿼리 실행
        List<CommentInfo> popularComments = query
                .where(applyBlacklistFilter(comment.post.id.eq(postId), memberId))
                .groupBy(comment.id, member.memberName, comment.createdAt,
                        parentClosure.ancestor.id, userCommentLike.id)
                .having(commentLike.countDistinct().goe(3)) // 추천 3개 이상
                .orderBy(commentLike.countDistinct().desc())
                .limit(3)
                .fetch();

        popularComments.forEach(info -> info.setPopular(true));
        return popularComments;
    }

    // 공통 댓글 빌딩
    private JPAQuery<CommentInfo> getCommentInfoJPAQuery(Long memberId, QCommentClosure parentClosure, QCommentLike userCommentLike) {
        JPAQuery<CommentInfo> query = jpaQueryFactory
                .select(Projections.constructor(CommentInfo.class,
                        comment.id,
                        comment.post.id,
                        comment.member.id,
                        member.memberName,
                        comment.content,
                        comment.deleted,
                        comment.createdAt,
                        parentClosure.ancestor.id.coalesce(comment.id),
                        commentLike.countDistinct().coalesce(0L).intValue(),
                        userCommentLike.id.isNotNull()
                ))
                .distinct()
                .from(comment)
                .leftJoin(comment.member, member)
                .leftJoin(commentLike).on(comment.id.eq(commentLike.comment.id))
                .leftJoin(parentClosure).on(
                        parentClosure.descendant.id.eq(comment.id)
                                .and(parentClosure.depth.eq(1))
                );

        // memberId가 있을 때만 userLike 조인
        if (memberId != null) {
            query.leftJoin(userCommentLike).on(
                    userCommentLike.comment.id.eq(comment.id)
                            .and(userCommentLike.member.id.eq(memberId))
            );
        }
        return query;
    }

    /**
     * <h3>사용자 작성 댓글 목록 조회</h3>
     * <p>특정 사용자가 작성한 댓글 목록을 페이지네이션으로 조회합니다.</p>
     * <p>최신 작성 댓글부터 과거 순서로 정렬하여 반환합니다.</p>
     * <p>{@link CommentQueryService}에서 사용자 작성 댓글 목록 조회 시 호출됩니다.</p>
     *
     * @param memberId 사용자 ID
     * @param pageable 페이지 정보
     * @return Page<SimpleCommentInfo> 작성한 댓글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    public Page<MemberActivityComment.SimpleCommentInfo> findCommentsByMemberId(Long memberId, Pageable pageable) {
        QCommentLike userCommentLike = new QCommentLike("userCommentLike");

        // 쿼리 빌딩 - memberId가 있으므로 항상 userLike 조인
        List<MemberActivityComment.SimpleCommentInfo> content = jpaQueryFactory
                .select(Projections.constructor(MemberActivityComment.SimpleCommentInfo.class,
                        comment.id,
                        comment.post.id,
                        comment.member.memberName,
                        comment.content,
                        comment.createdAt,
                        commentLike.countDistinct().coalesce(0L).intValue(),
                        userCommentLike.id.isNotNull()
                ))
                .from(comment)
                .join(comment.member, member)
                .leftJoin(commentLike).on(comment.id.eq(commentLike.comment.id))
                .leftJoin(userCommentLike).on(
                        userCommentLike.comment.id.eq(comment.id)
                                .and(userCommentLike.member.id.eq(memberId))
                )
                .where(comment.member.id.eq(memberId))
                .groupBy(comment.id, member.memberName, comment.createdAt, userCommentLike.id)
                .orderBy(comment.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = jpaQueryFactory
                .select(comment.count())
                .from(comment)
                .where(comment.member.id.eq(memberId))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * <h3>사용자 추천한 댓글 목록 조회</h3>
     * <p>특정 사용자가 추천한 댓글 목록을 추천일 기준 최신순으로 페이지네이션 조회합니다.</p>
     * <p>최신 추천 댓글부터 과거 순서로 정렬하여 반환합니다 (userLike.createdAt DESC).</p>
     *
     * @param memberId 사용자 ID
     * @param pageable 페이지 정보
     * @return Page<SimpleCommentInfo> 추천한 댓글 목록 페이지 (추천일 기준 최신순)
     * @author Jaeik
     * @since 2.0.0
     */
    public Page<MemberActivityComment.SimpleCommentInfo> findLikedCommentsByMemberId(Long memberId, Pageable pageable) {
        QCommentLike userLike = new QCommentLike("userLike");  // 필터링용
        QCommentLike allLikes = new QCommentLike("allLikes");  // 전체 좋아요 카운트용
        QCommentLike userCommentLike = new QCommentLike("userCommentLike");  // Projection용

        // 쿼리 빌딩
        List<MemberActivityComment.SimpleCommentInfo> content = jpaQueryFactory
                .select(Projections.constructor(MemberActivityComment.SimpleCommentInfo.class,
                        comment.id,
                        comment.post.id,
                        member.memberName,
                        comment.content,
                        comment.createdAt,
                        allLikes.countDistinct().coalesce(0L).intValue(), // 전체 좋아요 카운트
                        userCommentLike.id.isNotNull() // 사용자 추천 여부
                ))
                .from(comment)
                // 필터링용: 현재 사용자가 추천한 댓글만 (WHERE 대신 JOIN ON 사용)
                .join(userLike).on(
                        userLike.comment.id.eq(comment.id)
                                .and(userLike.member.id.eq(memberId))
                )
                // 전체 좋아요 카운트용: 모든 사용자의 좋아요
                .leftJoin(allLikes).on(allLikes.comment.id.eq(comment.id))
                .leftJoin(comment.member, member)
                // Projection용: 사용자 추천 여부 확인
                .leftJoin(userCommentLike).on(
                        userCommentLike.comment.id.eq(comment.id)
                                .and(userCommentLike.member.id.eq(memberId))
                )
                .groupBy(comment.id, member.memberName, comment.createdAt, userCommentLike.id, userLike.createdAt)
                .orderBy(userLike.createdAt.desc())  // 추천일 기준 정렬
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = jpaQueryFactory
                .select(comment.countDistinct())
                .from(comment)
                .join(commentLike).on(comment.id.eq(commentLike.comment.id))
                .where(commentLike.member.id.eq(memberId))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }



    /**
     * <h3>게시글 ID 목록에 대한 댓글 수 조회</h3>
     * <p>여러 게시글의 댓글 수를 배치로 조회합니다.</p>
     *
     * @param postIds 게시글 ID 목록
     * @return Map<Long, Integer> 게시글 ID를 키로, 댓글 수를 값으로 하는 맵
     * @author Jaeik
     * @since 2.0.0
     */
    public Map<Long, Integer> findCommentCountsByPostIds(List<Long> postIds) {
        List<Tuple> results = jpaQueryFactory
                .select(comment.post.id, comment.count())
                .from(comment)
                .where(comment.post.id.in(postIds))
                .groupBy(comment.post.id)
                .fetch();

        return results.stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(comment.post.id),
                        tuple -> Objects.requireNonNull(tuple.get(comment.count())).intValue()
                ));
    }

    /**
     * <h3>루트 댓글 수 조회</h3>
     * <p>주어진 게시글 ID에 해당하는 최상위(루트) 댓글의 수를 조회합니다.</p>
     * <p>depth=0인 댓글(루트 댓글)만 카운트합니다.</p>
     * <p>findCommentsWithOldestOrder 메서드에서 호출되어 페이지네이션 total 값 계산을 담당합니다.</p>
     *
     * @param postId 게시글 ID
     * @return Long 루트 댓글의 수
     * @author Jaeik
     * @since 2.0.0
     */
    private Long countRootCommentsByPostId(Long postId, Long memberId) {
        BooleanExpression blacklistFilter = applyBlacklistFilter(comment.post.id.eq(postId), memberId);
        return jpaQueryFactory
                .select(comment.countDistinct())
                .from(comment)
                .leftJoin(comment.member, member)
                .join(closure).on(comment.id.eq(closure.descendant.id))
                .where(blacklistFilter.and(closure.depth.eq(0)))
                .fetchOne();
    }

    // 블랙리스트 제외
    private BooleanExpression applyBlacklistFilter(BooleanExpression baseCondition, Long viewerId) {
        if (viewerId == null) {
            return baseCondition;
        }

        BooleanExpression blacklistBlock = JPAExpressions
                .selectOne()
                .from(memberBlacklist)
                .where(
                        memberBlacklist.requestMember.id.eq(viewerId).and(memberBlacklist.blackMember.id.eq(member.id))
                                .or(memberBlacklist.requestMember.id.eq(member.id).and(memberBlacklist.blackMember.id.eq(viewerId)))
                )
                .notExists();

        return baseCondition.and(blacklistBlock);
    }
}
