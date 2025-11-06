package jaeik.bimillog.domain.comment.out;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.comment.service.CommentCommandService;
import jaeik.bimillog.domain.comment.service.CommentQueryService;
import jaeik.bimillog.domain.comment.entity.*;
import jaeik.bimillog.domain.member.entity.QMember;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static jaeik.bimillog.domain.comment.out.CommentProjection.getCommentInfoProjectionWithUserLike;
import static jaeik.bimillog.domain.comment.out.CommentProjection.getSimpleCommentInfoProjection;
import static jaeik.bimillog.domain.comment.out.CommentProjection.getSimpleCommentInfoProjectionWithAllLikes;

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
public class CommentQueryAdapter {

    private final JPAQueryFactory jpaQueryFactory;
    private static final QComment comment = QComment.comment;
    private static final QCommentLike commentLike = QCommentLike.commentLike;
    private static final QCommentClosure closure = QCommentClosure.commentClosure;
    private static final QMember member = QMember.member;

    /**
     * <h3>사용자 작성 댓글 목록 조회</h3>
     * <p>특정 사용자가 작성한 댓글 목록을 페이지네이션으로 조회합니다.</p>
     * <p>최신 작성 댓글부터 과거 순서로 정렬하여 반환합니다.</p>
     * <p>{@link CommentQueryService}에서 사용자 작성 댓글 목록 조회 시 호출됩니다.</p>
     *
     * @param memberId   사용자 ID
     * @param pageable 페이지 정보
     * @return Page<SimpleCommentInfo> 작성한 댓글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
        public Page<SimpleCommentInfo> findCommentsByMemberId(Long memberId, Pageable pageable) {
        // N+1 문제 해결을 위한 별도 Q타입 생성
        QCommentLike userCommentLike = new QCommentLike("userCommentLike");

        // 쿼리 빌딩 - memberId가 있으므로 항상 userLike 조인
        List<SimpleCommentInfo> content = jpaQueryFactory
                .select(getSimpleCommentInfoProjection(userCommentLike))
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
     * <p>{@link CommentQueryService}에서 사용자 추천한 댓글 목록 조회 시 호출됩니다.</p>
     *
     * <h4>중요: likeCount 정확성을 위한 JOIN 구조</h4>
     * <ul>
     *   <li>userLike: WHERE 조건 대신 JOIN ON에서 필터링 (현재 사용자가 추천한 댓글만)</li>
     *   <li>allLikes: LEFT JOIN으로 전체 좋아요 수 카운트 (모든 사용자의 좋아요)</li>
     *   <li>userCommentLike: Projection용 (사용자 추천 여부 확인)</li>
     * </ul>
     *
     * @param memberId   사용자 ID
     * @param pageable 페이지 정보
     * @return Page<SimpleCommentInfo> 추천한 댓글 목록 페이지 (추천일 기준 최신순)
     * @author Jaeik
     * @since 2.0.0
     */
        public Page<SimpleCommentInfo> findLikedCommentsByMemberId(Long memberId, Pageable pageable) {
        // N+1 문제 해결 및 정확한 likeCount를 위한 별도 Q타입 생성
        QCommentLike userLike = new QCommentLike("userLike");  // 필터링용
        QCommentLike allLikes = new QCommentLike("allLikes");  // 전체 좋아요 카운트용
        QCommentLike userCommentLike = new QCommentLike("userCommentLike");  // Projection용

        // 쿼리 빌딩 - JOIN ON 절에서 필터링하여 WHERE 절 사용 방지
        List<SimpleCommentInfo> content = jpaQueryFactory
                .select(getSimpleCommentInfoProjectionWithAllLikes(allLikes, userCommentLike))
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
     * <h3>인기 댓글 조회</h3>
     * <p>주어진 게시글의 인기 댓글 목록을 조회합니다.</p>
     * <p>추천 수가 높은 댓글들을 우선순위로 정렬하여 반환합니다.</p>
     * <p>{@link CommentQueryService}에서 인기 댓글 조회 시 호출됩니다.</p>
     *
     * @param postId 게시글 ID
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
        JPAQuery<CommentInfo> query = jpaQueryFactory
                .select(getCommentInfoProjectionWithUserLike(parentClosure, userCommentLike))
                .from(comment)
                .leftJoin(comment.member, member)
                .leftJoin(commentLike).on(comment.id.eq(commentLike.comment.id))
                // parentId 조회를 위한 closure 조인 (depth=1)
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

        // 쿼리 실행
        List<CommentInfo> popularComments = query
                .where(comment.post.id.eq(postId))
                .groupBy(comment.id, member.memberName, comment.createdAt,
                         parentClosure.ancestor.id, userCommentLike.id)
                .having(commentLike.countDistinct().goe(3)) // 추천 3개 이상
                .orderBy(commentLike.countDistinct().desc())
                .limit(3)
                .fetch();

        popularComments.forEach(info -> info.setPopular(true));
        return popularComments;
    }

    /**
     * <h3>게시글 ID 목록에 대한 댓글 수 조회</h3>
     * <p>여러 게시글의 댓글 수를 배치로 조회합니다.</p>
     * <p>게시글 ID 목록을 한 번에 처리하여 각 게시글별 댓글 수를 반환합니다.</p>
     * <p>{@link CommentQueryService}에서 게시글 ID 목록에 대한 댓글 수 조회 시 호출됩니다.</p>
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
     * <h3>과거순 댓글 조회</h3>
     * <p>주어진 게시글의 댓글을 과거순으로 페이지네이션하여 조회합니다.</p>
     * <p>생성 시간이 오래된 댓글부터 최신 댓글까지 시간 순서대로 정렬합니다.</p>
     * <p>{@link CommentQueryService}에서 과거순 댓글 조회 시 호출됩니다.</p>
     *
     * @param postId   게시글 ID
     * @param pageable 페이지 정보
     * @param memberId   사용자 ID (추천 여부 확인용, null 가능)
     * @return Page<CommentInfo> 과거순 댓글 페이지
     * @author Jaeik
     * @since 2.0.0
     */
        public Page<CommentInfo> findCommentsWithOldestOrder(Long postId, Pageable pageable, Long memberId) {
        // N+1 문제 해결을 위한 별도 Q타입 생성
        QCommentClosure parentClosure = new QCommentClosure("parentClosure");
        QCommentLike userCommentLike = new QCommentLike("userCommentLike");

        // 쿼리 빌딩
        JPAQuery<CommentInfo> query = jpaQueryFactory
                .select(getCommentInfoProjectionWithUserLike(parentClosure, userCommentLike))
                .distinct()
                .from(comment)
                .leftJoin(comment.member, member)
                .leftJoin(commentLike).on(comment.id.eq(commentLike.comment.id))
                // parentId 조회를 위한 closure 조인 (depth=1)
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

        // 쿼리 실행
        List<CommentInfo> content = query
                .where(comment.post.id.eq(postId))
                .groupBy(comment.id, member.memberName, comment.createdAt,
                         parentClosure.ancestor.id, userCommentLike.id)
                .orderBy(comment.createdAt.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = countRootCommentsByPostId(postId);
        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * <h3>특정 사용자의 모든 댓글 조회</h3>
     * <p>사용자 탈퇴 시 댓글 처리를 위해 특정 사용자의 모든 댓글 엔티티를 조회합니다.</p>
     * <p>{@link CommentCommandService}에서 사용자 탈퇴 처리 시 호출됩니다.</p>
     *
     * @param memberId 조회할 사용자 ID
     * @return List<Comment> 사용자가 작성한 모든 댓글 엔티티 목록
     * @author Jaeik
     * @since 2.0.0
     */
        public List<Comment> findAllByMemberId(Long memberId) {
        return jpaQueryFactory
                .selectFrom(comment)
                .where(comment.member.id.eq(memberId))
                .fetch();
    }

    /**
     * <h3>특정 글의 모든 댓글 조회</h3>
     * <p>QueryDSL을 사용하여 특정 게시글의 모든 댓글을 조회합니다.</p>
     * <p>계층 구조와 무관하게 플랫한 리스트로 반환하며, 삭제 표시된 댓글도 포함합니다.</p>
     * <p>{@link CommentCommandService#deleteCommentsByPost}에서 게시글 삭제 시 호출됩니다.</p>
     *
     * @param postId 댓글을 조회할 게시글 ID
     * @return List<Comment> 해당 게시글의 모든 댓글 리스트
     * @author Jaeik
     * @since 2.0.0
     */
        public List<Comment> findAllByPostId(Long postId) {
        return jpaQueryFactory
                .selectFrom(comment)
                .where(comment.post.id.eq(postId))
                .fetch();
    }

    /**
     * <h3>자손 댓글 존재 여부 확인</h3>
     * <p>특정 댓글이 자손 댓글을 가지고 있는지 확인합니다.</p>
     * <p>클로저 테이블에서 depth > 0이고 ancestor가 해당 댓글인 경우가 있는지 확인합니다.</p>
     * <p>{@link CommentCommandService}에서 댓글 삭제 시 하드/소프트 삭제 결정을 위해 호출됩니다.</p>
     *
     * @param commentId 확인할 댓글 ID
     * @return boolean 자손 댓글이 있으면 true, 없으면 false
     * @author Jaeik
     * @since 2.0.0
     */
        public boolean hasDescendants(Long commentId) {
        Long count = jpaQueryFactory
                .select(closure.count())
                .from(closure)
                .where(closure.ancestor.id.eq(commentId)
                      .and(closure.depth.gt(0)))
                .fetchOne();
        
        return count != null && count > 0;
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
    private Long countRootCommentsByPostId(Long postId) {
        return jpaQueryFactory
                .select(comment.countDistinct())
                .from(comment)
                .join(closure).on(comment.id.eq(closure.descendant.id))
                .where(comment.post.id.eq(postId).and(closure.depth.eq(0)))
                .fetchOne();
    }
}
