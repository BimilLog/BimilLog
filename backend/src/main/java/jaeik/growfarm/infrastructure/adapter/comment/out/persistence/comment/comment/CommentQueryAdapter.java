package jaeik.growfarm.infrastructure.adapter.comment.out.persistence.comment.comment;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.domain.comment.application.port.out.CommentQueryPort;
import jaeik.growfarm.domain.comment.entity.Comment;
import jaeik.growfarm.domain.comment.entity.QComment;
import jaeik.growfarm.domain.comment.entity.QCommentClosure;
import jaeik.growfarm.domain.comment.entity.QCommentLike;
import jaeik.growfarm.domain.user.entity.QUser;
import jaeik.growfarm.infrastructure.adapter.comment.in.web.dto.CommentDTO;
import jaeik.growfarm.infrastructure.adapter.comment.in.web.dto.SimpleCommentDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <h2>댓글 쿼리 어댑터</h2>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class CommentQueryAdapter implements CommentQueryPort {


    private final CommentRepository commentRepository;
    private final JPAQueryFactory jpaQueryFactory;

    /**
     * <h3>ID로 댓글 조회</h3>
     * <p>주어진 ID로 댓글을 조회합니다.</p>
     *
     * @param commentId 댓글 ID
     * @return Optional<Comment> 조회된 댓글 엔티티. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<Comment> findById(Long commentId) {
        return commentRepository.findById(commentId);
    }

    /**
     * <h3>사용자가 추천한 댓글 ID 목록 조회</h3>
     * <p>주어진 댓글 ID 목록 중 사용자가 추천를 누른 댓글의 ID 목록을 조회합니다.</p>
     *
     * @param commentIds 댓글 ID 목록
     * @param userId     사용자 ID
     * @return List<Long> 사용자가 추천를 누른 댓글 ID 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<Long> findUserLikedCommentIds(List<Long> commentIds, Long userId) {
        return commentRepository.findUserLikedCommentIds(commentIds, userId);
    }

    /**
     * <h3>사용자 작성 댓글 목록 조회</h3>
     * <p>특정 사용자가 작성한 댓글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return Page<SimpleCommentDTO> 작성한 댓글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<SimpleCommentDTO> findCommentsByUserId(Long userId, Pageable pageable) {
        QComment comment = QComment.comment;
        QUser user = QUser.user;

        List<SimpleCommentDTO> content = jpaQueryFactory
                .select(Projections.constructor(SimpleCommentDTO.class,
                        comment.id,
                        comment.post.id,
                        user.userName,
                        comment.content,
                        comment.createdAt,
                        Expressions.constant(0),
                        Expressions.constant(false)))
                .from(comment)
                .leftJoin(comment.user, user)
                .where(comment.user.id.eq(userId))
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
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return Page<SimpleCommentDTO> 추천한 댓글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<SimpleCommentDTO> findLikedCommentsByUserId(Long userId, Pageable pageable) {
        QComment comment = QComment.comment;
        QCommentLike commentLike = QCommentLike.commentLike;
        QUser user = QUser.user;

        List<SimpleCommentDTO> content = jpaQueryFactory
                .select(Projections.constructor(SimpleCommentDTO.class,
                        comment.id,
                        comment.post.id,
                        user.userName,
                        comment.content,
                        comment.createdAt,
                        Expressions.constant(0),
                        Expressions.constant(false)))
                .from(comment)
                .join(commentLike).on(comment.id.eq(commentLike.comment.id))
                .leftJoin(comment.user, user)
                .where(commentLike.user.id.eq(userId))
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
     * <p>주어진 게시글의 인기 댓글 목록을 조회합니다. 사용자가 추천를 누른 댓글 정보도 포함합니다.</p>
     *
     * @param postId          게시글 ID
     * @param likedCommentIds 사용자가 추천한 댓글 ID 목록
     * @return List<CommentDTO> 인기 댓글 DTO 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<CommentDTO> findPopularComments(Long postId, List<Long> likedCommentIds) {
        QComment comment = QComment.comment;
        QCommentLike commentLike = QCommentLike.commentLike;
        QCommentClosure closure = QCommentClosure.commentClosure;
        QUser user = QUser.user;

        List<CommentDTO> popularComments = jpaQueryFactory
                .select(Projections.constructor(CommentDTO.class,
                        comment.id,
                        comment.post.id,
                        comment.user.id,
                        user.userName,
                        comment.content,
                        comment.deleted,
                        comment.password,
                        comment.createdAt,
                        closure.ancestor.id,
                        commentLike.countDistinct().coalesce(0L).intValue() // 추천수 한번에 조회, null 방지
                ))
                .from(comment)
                .leftJoin(comment.user, user)
                .leftJoin(commentLike).on(comment.id.eq(commentLike.comment.id))
                .leftJoin(closure).on(comment.id.eq(closure.descendant.id).and(closure.depth.eq(0)))
                .where(comment.post.id.eq(postId))
                .groupBy(comment.id, user.userName, closure.ancestor.id, comment.createdAt)
                .having(commentLike.countDistinct().goe(5)) // 추천 5개 이상
                .orderBy(commentLike.countDistinct().desc())
                .limit(5)
                .fetch();

        popularComments.forEach(dto -> {
            dto.setUserLike(likedCommentIds.contains(dto.getId()));
            dto.setPopular(true);
            // 추천수는 이미 쿼리에서 설정됨
        });
        return popularComments;
    }

    /**
     * <h3>여러 게시글 ID에 대한 댓글 수 조회</h3>
     * <p>주어진 여러 게시글 ID에 해당하는 각 게시글의 댓글 수를 조회합니다.</p>
     *
     * @param postIds 게시글 ID 목록
     * @return Map<Long, Integer> 게시글 ID를 키로, 댓글 수를 값으로 하는 맵
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Map<Long, Integer> findCommentCountsByPostIds(List<Long> postIds) {
        QComment comment = QComment.comment;

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
     * <p>주어진 게시글의 댓글을 과거순으로 페이지네이션하여 조회합니다. 사용자가 추천를 누른 댓글 정보도 포함합니다.</p>
     *
     * @param postId          게시글 ID
     * @param pageable        페이지 정보
     * @param likedCommentIds 사용자가 추천한 댓글 ID 목록
     * @return Page<CommentDTO> 과거순 댓글 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<CommentDTO> findCommentsWithOldestOrder(Long postId, Pageable pageable, List<Long> likedCommentIds) {
        QComment comment = QComment.comment;
        QCommentLike commentLike = QCommentLike.commentLike;
        QCommentClosure closure = QCommentClosure.commentClosure;
        QUser user = QUser.user;

        List<CommentDTO> content = jpaQueryFactory
                .select(Projections.constructor(CommentDTO.class,
                        comment.id,
                        comment.post.id,
                        comment.user.id,
                        user.userName,
                        comment.content,
                        comment.deleted,
                        comment.password,
                        comment.createdAt,
                        closure.ancestor.id,
                        commentLike.countDistinct().coalesce(0L).intValue()
                ))
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

        content.forEach(dto -> {
            dto.setUserLike(likedCommentIds.contains(dto.getId()));
        });

        Long total = countRootCommentsByPostId(postId);

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * <h3>게시글 ID로 루트 댓글 수 조회 (페이징용)</h3>
     * <p>주어진 게시글 ID에 해당하는 최상위(루트) 댓글의 수를 조회합니다.</p>
     *
     * <p><strong>사용 목적</strong>: 댓글 목록 페이징 시 total count 계산</p>
     * <p><strong>현재 사용</strong>: findCommentsWithOldestOrder 메서드에서 PageImpl total 값 설정에 사용</p>
     * <p><strong>구현 세부</strong>: QueryDSL로 구현, depth=0인 댓글(루트 댓글)만 카운트</p>
     * <p><strong>내부 메서드</strong>: Infrastructure layer 내부에서만 사용</p>
     *
     * @param postId 게시글 ID
     * @return Long 루트 댓글의 수
     * @author Jaeik
     * @since 2.0.0
     */
    private Long countRootCommentsByPostId(Long postId) {
        QComment comment = QComment.comment;
        QCommentClosure closure = QCommentClosure.commentClosure;

        return jpaQueryFactory
                .select(comment.countDistinct())
                .from(comment)
                .join(closure).on(comment.id.eq(closure.descendant.id))
                .where(comment.post.id.eq(postId).and(closure.depth.eq(0)))
                .fetchOne();
    }
}
