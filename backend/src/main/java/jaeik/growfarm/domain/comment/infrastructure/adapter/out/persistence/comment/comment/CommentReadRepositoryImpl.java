package jaeik.growfarm.domain.comment.infrastructure.adapter.out.persistence.comment.comment;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.domain.comment.entity.QComment;
import jaeik.growfarm.domain.comment.entity.QCommentClosure;
import jaeik.growfarm.domain.comment.entity.QCommentLike;
import jaeik.growfarm.domain.user.entity.QUser;
import jaeik.growfarm.dto.comment.CommentDTO;
import jaeik.growfarm.dto.comment.SimpleCommentDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <h2>댓글 조회 레포지토리 구현체</h2>
 * <p>
 * `CommentReadRepository` 인터페이스의 구현체로, QueryDSL을 사용하여 댓글 데이터를 조회합니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class CommentReadRepositoryImpl implements CommentReadRepository {

    private final JPAQueryFactory jpaQueryFactory;

    /**
     * <h3>최신순 댓글 조회</h3>
     * <p>주어진 게시글의 댓글을 최신순으로 페이지네이션하여 조회합니다. 사용자가 추천를 누른 댓글 정보도 포함합니다.</p>
     *
     * @param postId          게시글 ID
     * @param pageable        페이지 정보
     * @param likedCommentIds 사용자가 추천한 댓글 ID 목록
     * @return Page<CommentDTO> 최신순 댓글 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<CommentDTO> findCommentsWithLatestOrder(Long postId, Pageable pageable, List<Long> likedCommentIds) {
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
                .groupBy(comment.id, user.userName, closure.ancestor.id)
                .orderBy(comment.createdAt.desc())
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
                .groupBy(comment.id, user.userName, closure.ancestor.id)
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
     * <h3>게시글 ID로 루트 댓글 수 조회 (페이징용)</h3>
     * <p>주어진 게시글 ID에 해당하는 최상위(루트) 댓글의 수를 조회합니다.</p>
     * 
     * <p><strong>사용 목적</strong>: 댓글 목록 페이징 시 total count 계산</p>
     * <p><strong>현재 사용</strong>: findCommentsWithLatestOrder 메서드에서 PageImpl total 값 설정에 사용</p>
     * <p><strong>구현 세부</strong>: QueryDSL로 구현, depth=0인 댓글(루트 댓글)만 카운트</p>
     * <p><strong>내부 메서드</strong>: Infrastructure layer 내부에서만 사용</p>
     *
     * @param postId 게시글 ID
     * @return Long 루트 댓글의 수
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Long countRootCommentsByPostId(Long postId) {
        QComment comment = QComment.comment;
        QCommentClosure closure = QCommentClosure.commentClosure;

        return jpaQueryFactory
                .select(comment.countDistinct())
                .from(comment)
                .join(closure).on(comment.id.eq(closure.descendant.id))
                .where(comment.post.id.eq(postId).and(closure.depth.eq(0)))
                .fetchOne();
    }

    /**
     * <h3>여러 게시글 ID에 대한 댓글 수 조회</h3>
     * <p>주어진 여러 게시글 ID에 해당하는 각 게시글의 댓글 수를 조회합니다.</p>
     * 
     * <p><strong>⚠️ TODO: 현재 미사용 구현 - N+1 문제 해결을 위한 배치 조회</strong></p>
     * <p>이 메서드는 QueryDSL로 정확하게 구현되어 있으며, 한 번의 쿼리로 여러 게시글의 댓글 수를 조회합니다.</p>
     * <p><strong>주의:</strong> 현재는 depth 구분 없이 모든 댓글을 카운트합니다. 루트 댓글만 필요하면 수정 필요.</p>
     * <p>PostQueryService 게시글 목록 조회 시 이 메서드를 사용하여 성능을 최적화할 수 있습니다.</p>
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
                        tuple -> tuple.get(comment.count()).intValue()
                ));
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
                        comment.content,
                        user.userName,
                        comment.createdAt,
                        comment.post.id,
                        comment.post.title))
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
                        comment.content,
                        user.userName,
                        comment.createdAt,
                        comment.post.id,
                        comment.post.title))
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
}