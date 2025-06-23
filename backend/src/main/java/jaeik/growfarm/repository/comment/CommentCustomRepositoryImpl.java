package jaeik.growfarm.repository.comment;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.dto.comment.SimpleCommentDTO;
import jaeik.growfarm.entity.comment.Comment;
import jaeik.growfarm.entity.comment.QComment;
import jaeik.growfarm.entity.comment.QCommentClosure;
import jaeik.growfarm.entity.comment.QCommentLike;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <h2>커스텀 댓글 저장소 구현 클래스</h2>
 * <p>
 * 댓글 관련 데이터베이스 작업을 수행하며 커스텀한 쿼리메소드가 포함되어 있습니다.
 * </p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class CommentCustomRepositoryImpl implements CommentCustomRepository {

        private final JPAQueryFactory jpaQueryFactory;
        private final CommentClosureRepository commentClosureRepository;

        /**
         * <h3>루트 댓글 총 개수 조회</h3>
         * <p>
         * 삭제된 댓글 포함하여 전체 루트 댓글 개수 조회
         * </p>
         *
         * @param postId 게시글 ID
         * @return 전체 루트 댓글 개수
         */
        @Override
        public Long countRootCommentsByPostId(Long postId) {

                QComment comment = QComment.comment;
                QCommentClosure commentClosure = QCommentClosure.commentClosure;

                Long count = jpaQueryFactory
                                .select(comment.count())
                                .from(comment)
                                .join(commentClosure).on(commentClosure.descendant.eq(comment))
                                .where(comment.post.id.eq(postId).and(commentClosure.depth.eq(0)))
                                .fetchOne();

                return count != null ? count : 0L;
        }

        /**
         * <h3>게시글별 댓글 수 배치 조회</h3>
         * <p>
         * 여러 게시글의 댓글 수를 한 번에 조회 (삭제된 댓글 제외)
         * </p>
         *
         * @param postIds 게시글 ID 리스트
         * @return 게시글 ID와 댓글 수의 매핑
         */
        @Override
        public Map<Long, Integer> findCommentCountsByPostIds(List<Long> postIds) {
                if (postIds == null || postIds.isEmpty()) {
                        return Map.of();
                }

                QComment comment = QComment.comment;
                NumberExpression<Long> commentCountExpr = comment.count().coalesce(0L);

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
                                                        return count != null && count <= Integer.MAX_VALUE
                                                                        ? count.intValue()
                                                                        : 0;
                                                }));
        }

        /**
         * <h3>인기댓글 조회</h3>
         * <p>
         * 해당 게시글의 댓글 중에서 추천수 3개 이상인 상위 3개를 조회
         * </p>
         *
         * @param postId 게시글 ID
         * @return 인기댓글 리스트 (추천수 포함)
         */
        @Override
        public List<Tuple> findPopularComments(Long postId) {
                QComment comment = QComment.comment;
                QCommentLike commentLike = QCommentLike.commentLike;
                QCommentClosure parentClosure = new QCommentClosure("parentClosure");

                return jpaQueryFactory
                                .select(
                                                comment,
                                                commentLike.count().coalesce(0L).as("likeCount"),
                                                parentClosure.ancestor.id.as("parentId"))
                                .from(comment)
                                .leftJoin(commentLike).on(commentLike.comment.eq(comment))
                                .leftJoin(parentClosure)
                                .on(parentClosure.descendant.eq(comment).and(parentClosure.depth.eq(1)))
                                .where(comment.post.id.eq(postId))
                                .groupBy(comment.id, parentClosure.ancestor.id)
                                .having(commentLike.count().goe(3))
                                .orderBy(commentLike.count().desc())
                                .limit(3)
                                .fetch();
        }

        /**
         * <h3>댓글 조회</h3>
         * <p>
         * 루트댓글을 최신순으로 조회하고 자손댓글도 함께 반환
         * </p>
         *
         * @param postId   게시글 ID
         * @param pageable 페이징 정보
         * @return 최신순 정렬된 루트 댓글과 자손댓글 리스트
         */
        @Override
        public List<Tuple> findCommentsWithLatestOrder(Long postId, Pageable pageable) {
                QComment comment = QComment.comment;
                QCommentLike commentLike = QCommentLike.commentLike;
                QCommentClosure commentClosure = QCommentClosure.commentClosure;
                QCommentClosure parentClosure = new QCommentClosure("parentClosure");

                List<Long> rootCommentIds = jpaQueryFactory
                                .select(comment.id)
                                .from(comment)
                                .join(commentClosure).on(commentClosure.descendant.eq(comment))
                                .where(comment.post.id.eq(postId).and(commentClosure.depth.eq(0)))
                                .orderBy(comment.createdAt.desc()) // 최신순
                                .offset(pageable.getOffset())
                                .limit(pageable.getPageSize())
                                .fetch();

                if (rootCommentIds.isEmpty()) {
                        return List.of();
                }

                return jpaQueryFactory
                                .select(
                                                comment,
                                                commentLike.count().coalesce(0L).as("likeCount"),
                                                commentClosure.ancestor.id.as("rootCommentId"),
                                                commentClosure.depth.as("depth"),
                                                parentClosure.ancestor.id.as("parentId"))
                                .from(comment)
                                .leftJoin(commentLike).on(commentLike.comment.eq(comment))
                                .innerJoin(commentClosure).on(commentClosure.descendant.eq(comment))
                                .leftJoin(parentClosure)
                                .on(parentClosure.descendant.eq(comment).and(parentClosure.depth.eq(1)))
                                .where(commentClosure.ancestor.id.in(rootCommentIds))
                                .groupBy(comment.id, commentClosure.ancestor.id, commentClosure.depth,
                                                parentClosure.ancestor.id)
                                .orderBy(
                                                commentClosure.ancestor.createdAt.desc(),
                                                commentClosure.depth.asc(),
                                                comment.createdAt.asc())
                                .fetch();
        }

        /**
         * <h3>사용자 작성 댓글 조회</h3>
         * <p>
         * 사용자 ID를 기준으로 해당 사용자가 작성한 댓글 목록을 조회한다.
         * </p>
         *
         * @param userId   사용자 ID
         * @param pageable 페이징 정보
         * @return 사용자가 작성한 댓글 페이지
         */
        @Override
        @Transactional(readOnly = true)
        public Page<SimpleCommentDTO> findCommentsByUserId(Long userId, Pageable pageable) {
                QComment comment = QComment.comment;
                QCommentLike commentLike = QCommentLike.commentLike;

                List<Tuple> commentTuples = jpaQueryFactory
                                .select(
                                                comment,
                                                commentLike.count().coalesce(0L).as("likeCount"))
                                .from(comment)
                                .leftJoin(commentLike).on(commentLike.comment.eq(comment))
                                .where(comment.user.id.eq(userId).and(comment.deleted.eq(false)))
                                .groupBy(comment.id)
                                .orderBy(comment.createdAt.desc())
                                .offset(pageable.getOffset())
                                .limit(pageable.getPageSize())
                                .fetch();

                if (commentTuples.isEmpty()) {
                        return new PageImpl<>(Collections.emptyList(), pageable, 0);
                }

                List<SimpleCommentDTO> commentDTOs = commentTuples.stream()
                                .map(tuple -> {
                                        var commentEntity = tuple.get(comment);
                                        Long likeCount = tuple.get(1, Long.class);
                                        return new SimpleCommentDTO(
                                                        Objects.requireNonNull(commentEntity).getId(),
                                                        commentEntity.getPost().getId(),
                                                        commentEntity.getUser().getUserName(),
                                                        commentEntity.getContent(),
                                                        commentEntity.getCreatedAt(),
                                                        likeCount != null ? Math.toIntExact(likeCount) : 0,
                                                        false);
                                })
                                .collect(Collectors.toList());

                Long totalCount = jpaQueryFactory
                                .select(comment.count())
                                .from(comment)
                                .where(comment.user.id.eq(userId).and(comment.deleted.eq(false)))
                                .fetchOne();

                return new PageImpl<>(commentDTOs, pageable, totalCount != null ? totalCount : 0L);
        }

        /**
         * <h3>사용자가 추천한 댓글 조회</h3>
         * <p>
         * 사용자 ID를 기준으로 해당 사용자가 추천한 댓글 목록을 조회한다.
         * </p>
         *
         * @param userId   사용자 ID
         * @param pageable 페이징 정보
         * @return 사용자가 추천한 댓글 페이지
         */
        @Override
        @Transactional(readOnly = true)
        public Page<SimpleCommentDTO> findLikedCommentsByUserId(Long userId, Pageable pageable) {
                QComment comment = QComment.comment;
                QCommentLike commentLike = QCommentLike.commentLike;

                // 사용자가 추천한 댓글 조회
                List<Tuple> commentTuples = jpaQueryFactory
                                .select(
                                                comment,
                                                commentLike.count().coalesce(0L).as("likeCount"))
                                .from(comment)
                                .join(commentLike).on(commentLike.comment.eq(comment))
                                .where(commentLike.user.id.eq(userId).and(comment.deleted.eq(false)))
                                .groupBy(comment.id)
                                .orderBy(comment.createdAt.desc()) // 댓글 생성일 기준 최신순 정렬
                                .offset(pageable.getOffset())
                                .limit(pageable.getPageSize())
                                .fetch();

                if (commentTuples.isEmpty()) {
                        return new PageImpl<>(Collections.emptyList(), pageable, 0);
                }

                List<SimpleCommentDTO> commentDTOs = commentTuples.stream()
                                .map(tuple -> {
                                        var commentEntity = tuple.get(comment);
                                        Long likeCount = tuple.get(1, Long.class);
                                        return new SimpleCommentDTO(
                                                        Objects.requireNonNull(commentEntity).getId(),
                                                        commentEntity.getPost().getId(),
                                                        commentEntity.getUser().getUserName(),
                                                        commentEntity.getContent(),
                                                        commentEntity.getCreatedAt(),
                                                        likeCount != null ? Math.toIntExact(likeCount) : 0,
                                                        true);
                                })
                                .collect(Collectors.toList());

                Long totalCount = jpaQueryFactory
                                .select(comment.count())
                                .from(comment)
                                .join(commentLike).on(commentLike.comment.eq(comment))
                                .where(commentLike.user.id.eq(userId).and(comment.deleted.eq(false)))
                                .fetchOne();

                return new PageImpl<>(commentDTOs, pageable, totalCount != null ? totalCount : 0L);
        }

        /**
         * <h3>회원탈퇴 시 댓글 처리</h3>
         * <p>
         * 탈퇴하는 사용자의 댓글을 처리한다.
         * </p>
         * <p>
         * 자손이 있는 댓글: 논리적 삭제 + userId null로 변경
         * </p>
         * <p>
         * 자손이 없는 댓글: 물리적 삭제
         * </p>
         *
         * @param userId 탈퇴하는 사용자 ID
         * @author Jaeik
         * @since 1.0.0
         */
        @Override
        @Transactional
        public void processUserCommentsOnWithdrawal(Long userId) {
                QComment comment = QComment.comment;

                List<Comment> userComments = jpaQueryFactory
                                .selectFrom(comment)
                                .where(comment.user.id.eq(userId).and(comment.deleted.eq(false)))
                                .fetch();

                for (Comment userComment : userComments) {
                        Long commentId = userComment.getId();

                        boolean hasDescendants = commentClosureRepository.hasDescendants(commentId);

                        if (hasDescendants) {
                                jpaQueryFactory
                                                .update(comment)
                                                .set(comment.deleted, true)
                                                .set(comment.content, "탈퇴한 회원의 댓글입니다.")
                                                .setNull(comment.user)
                                                .where(comment.id.eq(commentId))
                                                .execute();
                        } else {
                                commentClosureRepository.deleteByDescendantId(commentId);
                                jpaQueryFactory
                                                .delete(comment)
                                                .where(comment.id.eq(commentId))
                                                .execute();
                        }
                }
        }
}
