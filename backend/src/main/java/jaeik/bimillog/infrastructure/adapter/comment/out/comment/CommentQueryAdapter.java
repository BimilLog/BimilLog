package jaeik.bimillog.infrastructure.adapter.comment.out.comment;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.comment.application.port.out.CommentQueryPort;
import jaeik.bimillog.domain.comment.application.service.CommentCommandService;
import jaeik.bimillog.domain.comment.application.service.CommentQueryService;
import jaeik.bimillog.domain.comment.entity.*;
import jaeik.bimillog.domain.comment.exception.CommentCustomException;
import jaeik.bimillog.domain.comment.exception.CommentErrorCode;
import jaeik.bimillog.domain.user.entity.QUser;
import jaeik.bimillog.infrastructure.adapter.comment.out.jpa.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static jaeik.bimillog.infrastructure.adapter.comment.out.util.CommentProjection.getCommentInfoProjectionWithUserLike;
import static jaeik.bimillog.infrastructure.adapter.comment.out.util.CommentProjection.getSimpleCommentInfoProjection;

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
public class CommentQueryAdapter implements CommentQueryPort {

    private final CommentRepository commentRepository;
    private final JPAQueryFactory jpaQueryFactory;

    private static final QComment comment = QComment.comment;
    private static final QCommentLike commentLike = QCommentLike.commentLike;
    private static final QCommentClosure closure = QCommentClosure.commentClosure;
    private static final QUser user = QUser.user;

    /**
     * <h3>ID로 댓글 조회</h3>
     * <p>댓글 ID로 댓글을 조회합니다.</p>
     * <p>존재하지 않는 댓글 ID인 경우 예외를 발생시킵니다.</p>
     * <p>{@link CommentQueryService}에서 댓글 ID로 댓글 조회 시 호출됩니다.</p>
     *
     * @param commentId 댓글 ID
     * @return Comment 조회된 댓글 엔티티
     * @throws CommentCustomException 댓글이 존재하지 않을 때
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Comment findById(Long commentId) {
        return commentRepository.findById(commentId).orElseThrow(() -> new CommentCustomException(CommentErrorCode.COMMENT_NOT_FOUND));
    }


    /**
     * <h3>사용자 작성 댓글 목록 조회</h3>
     * <p>특정 사용자가 작성한 댓글 목록을 페이지네이션으로 조회합니다.</p>
     * <p>최신 작성 댓글부터 과거 순서로 정렬하여 반환</p>
     * <p>{@link CommentQueryService}에서 사용자 작성 댓글 목록 조회 시 호출됩니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return Page<SimpleCommentInfo> 작성한 댓글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<SimpleCommentInfo> findCommentsByUserId(Long userId, Pageable pageable) {

        List<SimpleCommentInfo> content = jpaQueryFactory
                .select(getSimpleCommentInfoProjection(userId)) // userId를 매개변수로 전달
                .from(comment)
                .leftJoin(comment.user, user)
                .leftJoin(commentLike).on(comment.id.eq(commentLike.comment.id))
                .where(comment.user.id.eq(userId))
                .groupBy(comment.id, user.userName, comment.createdAt)
                .orderBy(comment.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = jpaQueryFactory
                .select(comment.count())
                .from(comment)
                .where(comment.user.id.eq(userId))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * <h3>사용자 추천한 댓글 목록 조회</h3>
     * <p>특정 사용자가 추천한 댓글 목록을 페이지네이션으로 조회합니다.</p>
     * <p>최신 추천 댓글부터 과거 순서로 정렬하여 반환</p>
     * <p>{@link CommentQueryService}에서 사용자 추천한 댓글 목록 조회 시 호출됩니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return Page<SimpleCommentInfo> 추천한 댓글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<SimpleCommentInfo> findLikedCommentsByUserId(Long userId, Pageable pageable) {

        List<SimpleCommentInfo> content = jpaQueryFactory
                .select(getSimpleCommentInfoProjection(userId)) // userId를 매개변수로 전달
                .from(comment)
                .join(commentLike).on(comment.id.eq(commentLike.comment.id))
                .leftJoin(comment.user, user)
                .where(commentLike.user.id.eq(userId))
                .groupBy(comment.id, user.userName, comment.createdAt)
                .orderBy(comment.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = jpaQueryFactory
                .select(comment.countDistinct())
                .from(comment)
                .join(commentLike).on(comment.id.eq(commentLike.comment.id))
                .where(commentLike.user.id.eq(userId))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * <h3>인기 댓글 조회</h3>
     * <p>주어진 게시글의 인기 댓글 목록을 조회합니다.</p>
     * <p>추천 수가 높은 댓글들을 우선순위로 정렬하여 반환</p>
     * <p>{@link CommentQueryService}에서 인기 댓글 조회 시 호출됩니다.</p>
     *
     * @param postId 게시글 ID
     * @param userId 사용자 ID (추천 여부 확인용, null 가능)
     * @return List<CommentInfo> 인기 댓글 정보 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<CommentInfo> findPopularComments(Long postId, Long userId) {

        List<CommentInfo> popularComments = jpaQueryFactory
                .select(getCommentInfoProjectionWithUserLike(userId))
                .from(comment)
                .leftJoin(comment.user, user)
                .leftJoin(commentLike).on(comment.id.eq(commentLike.comment.id))
                .leftJoin(closure).on(comment.id.eq(closure.descendant.id).and(closure.depth.eq(0)))
                .where(comment.post.id.eq(postId))
                .groupBy(comment.id, user.userName, closure.ancestor.id, comment.createdAt)
                .having(commentLike.countDistinct().goe(3)) // 추천 3개 이상
                .orderBy(commentLike.countDistinct().desc())
                .limit(5)
                .fetch();

        popularComments.forEach(info -> info.setPopular(true));
        return popularComments;
    }

    /**
     * <h3>게시글 ID 목록에 대한 댓글 수 조회</h3>
     * <p>여러 게시글의 댓글 수를 배치로 조회합니다.</p>
     * <p>게시글 ID 목록을 한 번에 처리하여 각 게시글별 댓글 수를 반환</p>
     * <p>{@link CommentQueryService}에서 게시글 ID 목록에 대한 댓글 수 조회 시 호출됩니다.</p>
     *
     * @param postIds 게시글 ID 목록
     * @return Map<Long, Integer> 게시글 ID를 키로, 댓글 수를 값으로 하는 맵
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
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
     * <p>생성 시간이 오래된 댓글부터 최신 댓글까지 시간 순서대로 정렬</p>
     * <p>{@link CommentQueryService}에서 과거순 댓글 조회 시 호출됩니다.</p>
     *
     * @param postId   게시글 ID
     * @param pageable 페이지 정보
     * @param userId   사용자 ID (추천 여부 확인용, null 가능)
     * @return Page<CommentInfo> 과거순 댓글 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<CommentInfo> findCommentsWithOldestOrder(Long postId, Pageable pageable, Long userId) {
        List<CommentInfo> content = jpaQueryFactory
                .select(getCommentInfoProjectionWithUserLike(userId))
                .from(comment)
                .leftJoin(comment.user, user)
                .leftJoin(closure).on(comment.id.eq(closure.descendant.id))
                .leftJoin(commentLike).on(comment.id.eq(commentLike.comment.id))
                .where(comment.post.id.eq(postId))
                .groupBy(comment.id, user.userName, closure.ancestor.id, comment.createdAt)
                .orderBy(comment.createdAt.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = countRootCommentsByPostId(postId);
        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * <h3>루트 댓글 수 조회</h3>
     * <p>주어진 게시글 ID에 해당하는 최상위(루트) 댓글의 수를 조회합니다.</p>
     * <p>depth=0인 댓글(루트 댓글)만 카운트</p>
     * <p>과거순 댓글 조회 메서드에서 호출되어 페이지네이션 total 값 계산을 담당합니다.</p>
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
