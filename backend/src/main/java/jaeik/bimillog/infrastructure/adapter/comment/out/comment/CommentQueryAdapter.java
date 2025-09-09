package jaeik.bimillog.infrastructure.adapter.comment.out.comment;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.comment.application.port.out.CommentQueryPort;
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
 * <p>
 * 헥사고날 아키텍처의 Secondary Adapter로서 CommentQueryPort 인터페이스를 구현합니다.
 * </p>
 * <p>
 * JPA Repository와 QueryDSL을 사용하여 댓글 엔티티의 조회 작업을 수행합니다.
 * 복잡한 JOIN 쿼리와 페이지네이션, 집계 연산을 QueryDSL로 처리합니다.
 * </p>
 * <p>
 * 이 어댑터가 존재하는 이유: 댓글 조회는 댓글-사용자-추천-클로저 테이블간의
 * 복잡한 조인 연산이 필요하여, 단순한 CRUD 이상의 복잡성을 가지므로 별도 어댑터로 분리하였습니다.
 * </p>
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
     * <p>주어진 ID로 댓글을 JPA로 조회합니다.</p>
     * <p>CommentQueryUseCase가 단일 댓글 조회 시 호출합니다.</p>
     * <p>CommentDeleteUseCase가 댓글 삭제 전 존재 여부 확인을 위해 호출합니다.</p>
     *
     * @param commentId 댓글 ID
     * @return Comment 조회된 댓글 엔티티 (미존재 시 예외 발생)
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
     * <p>특정 사용자가 작성한 댓글 목록을 QueryDSL로 페이지네이션 조회합니다.</p>
     * <p>추천 수와 사용자 추천 여부도 한 번의 쿼리로 함께 조회합니다.</p>
     * <p>UserQueryUseCase가 마이페이지에서 사용자의 댓글 목록을 보여줄 때 호출합니다.</p>
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
     * <p>특정 사용자가 추천한 댓글 목록을 QueryDSL로 페이지네이션 조회합니다.</p>
     * <p>추천 수와 사용자 추천 여부도 한 번의 쿼리로 함께 조회합니다.</p>
     * <p>UserQueryUseCase가 마이페이지에서 사용자가 추천한 댓글 목록을 보여줄 때 호출합니다.</p>
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
     * <p>주어진 게시글의 인기 댓글 목록을 QueryDSL로 조회합니다.</p>
     * <p>추천 수 3개 이상인 댓글을 추천 수 내림차순으로 정렬하여 조회합니다.</p>
     * <p>사용자 추천 여부도 한 번의 쿼리로 함께 조회합니다.</p>
     * <p>PostQueryUseCase가 게시글 상세보기에서 인기 댓글을 보여줄 때 호출합니다.</p>
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
     * <h3>여러 게시글 ID에 대한 댓글 수 조회</h3>
     * <p>주어진 여러 게시글 ID에 해당하는 각 게시글의 댓글 수를 QueryDSL로 조회합니다.</p>
     * <p>PostQueryUseCase가 게시글 목록에서 각 게시글의 댓글 수를 보여줄 때 호출합니다.</p>
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
     * <p>주어진 게시글의 댓글을 QueryDSL로 과거순 페이지네이션 조회합니다.</p>
     * <p>사용자 추천 여부도 한 번의 쿼리로 함께 조회합니다.</p>
     * <p>CommentQueryUseCase가 댓글 목록을 시간순으로 보여줄 때 호출합니다.</p>
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
     * <h3>게시글 ID로 루트 댓글 수 조회 (페이징용)</h3>
     * <p>주어진 게시글 ID에 해당하는 최상위(루트) 댓글의 수를 QueryDSL로 조회합니다.</p>
     *
     * <p><strong>사용 목적</strong>: 댓글 목록 페이징 시 total count 계산</p>
     * <p><strong>현재 사용</strong>: findCommentsWithOldestOrder 메서드에서 PageImpl total 값 설정에 사용</p>
     * <p><strong>구현 세부</strong>: depth=0인 댓글(루트 댓글)만 카운트</p>
     * <p><strong>내부 메서드</strong>: Infrastructure layer 내부에서만 사용</p>
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
