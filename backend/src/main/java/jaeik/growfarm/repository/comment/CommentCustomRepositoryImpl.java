package jaeik.growfarm.repository.comment;

import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.entity.comment.Comment;
import jaeik.growfarm.entity.comment.QComment;
import jaeik.growfarm.entity.comment.QCommentLike;
import jaeik.growfarm.entity.post.QPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*
 * 커스텀 댓글 Repository 구현체
 * 댓글 관련 데이터베이스 작업을 수행하는 Repository 구현체
 * 수정일 : 2025-05-03
 */
@Repository
public class CommentCustomRepositoryImpl implements CommentCustomRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public CommentCustomRepositoryImpl(JPAQueryFactory jpaQueryFactory) {
        this.jpaQueryFactory = jpaQueryFactory;
    }

    // 글의 댓글 목록 가져 오기
    @Override
    public List<Comment> findByCommentList(Long postId) {
        QComment comment = QComment.comment;
        QPost post = QPost.post;

        return jpaQueryFactory
                .selectFrom(comment)
                .join(comment.post, post)
                .where(post.id.eq(postId)) // 특정 게시글의 댓글만 필터링
                .orderBy(comment.createdAt.desc())
                .fetch();
    }

    // 해당 유저가 추천 누른 댓글 목록 반환
    @Override
    public Page<Comment> findByLikedComments(Long userId, Pageable pageable) {
        QComment comment = QComment.comment;
        QCommentLike commentLike = QCommentLike.commentLike;

        // 사용자가 좋아요한 댓글 목록 조회 (페이징 적용)
        List<Comment> comments = jpaQueryFactory
                .selectFrom(comment)
                .join(commentLike)
                .on(comment.id.eq(commentLike.comment.id))
                .where(commentLike.user.id.eq(userId))
                .orderBy(comment.createdAt.desc()) // 최신순으로 정렬
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 카운트 쿼리 실행
        Long total = jpaQueryFactory
                .select(comment.count())
                .from(comment)
                .join(commentLike)
                .on(comment.id.eq(commentLike.comment.id))
                .where(commentLike.user.id.eq(userId))
                .fetchOne();

        // null 안전성을 고려한 코드로 수정
        return new PageImpl<>(comments, pageable, total == null ? 0L : total);
    }

    /**
     * <h3>게시글별 댓글 수 조회</h3>
     * <p>
     * 게시글 ID 리스트로 각 게시글의 댓글 수를 조회한다.
     * </p>
     *
     * @param postIds 게시글 ID 리스트
     * @return 게시글 ID와 댓글 수의 맵
     * @author Jaeik
     * @since 1.0.0
     */
    @Override
    public Map<Long, Integer> findCommentCountsByPostIds(List<Long> postIds) {
        QComment comment = QComment.comment;
        NumberExpression<Long> commentCountExpr = comment.count().coalesce(0L).as("commentCount");

        return jpaQueryFactory
                .select(comment.post.id, commentCountExpr)
                .from(comment)
                .where(comment.post.id.in(postIds).and(comment.deleted.eq(false)))
                .groupBy(comment.post.id)
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(comment.post.id),
                        tuple -> {
                            Long count = tuple.get(commentCountExpr);
                            return count != null ? Math.toIntExact(count) : 0;
                        }));
    }
}
