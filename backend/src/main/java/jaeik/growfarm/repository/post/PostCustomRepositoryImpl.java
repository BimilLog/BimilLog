package jaeik.growfarm.repository.post;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.dto.board.SimplePostDTO;
import jaeik.growfarm.entity.comment.QComment;
import jaeik.growfarm.entity.post.Post;
import jaeik.growfarm.entity.post.QPost;
import jaeik.growfarm.entity.post.QPostLike;
import jaeik.growfarm.entity.user.QUsers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * <h2>PostCustomRepositoryImpl</h2>
 * <p>게시글 관련 커스텀 쿼리 메소드 구현체</p>
 *
 * @author jaeik
 * @version 1.0
 */
@Repository
public class PostCustomRepositoryImpl implements PostCustomRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public PostCustomRepositoryImpl(JPAQueryFactory jpaQueryFactory) {
        this.jpaQueryFactory = jpaQueryFactory;
    }

    /**
     * <h3>게시글 목록 조회</h3>
     * <p>최신순으로 페이징하여 게시글 목록을 조회한다.</p>
     * <p>게시글 마다의 총 댓글 수, 총 추천 수를 반환한다.</p>
     * <p>4개의 쿼리를 사용한다.</p>
     * <p>게시글의 idx_post_notice_created 인덱스 활용</p>
     * <p>댓글의 idx_comment_post_deleted 인덱스 활용</p>
     * <p>유저의 기본키 인덱스 활용</p>
     * <p>글 추천의 외래키 post_id 인덱스 활용</p>
     * <p>OFFSET이 높아졌을 때를 대비하여 커서 기반 페이지네이션으로 리팩토링 필요</p>
     * @param pageable 페이지 정보
     * @return 게시글 목록과 댓글, 좋아요 수를 포함한 게시판 용 게시글 DTO
     * @author Jaeik
     * @since 1.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public Page<SimplePostDTO> findPostsWithCommentAndLikeCounts(Pageable pageable) {
        QPost post = QPost.post;
        QUsers user = QUsers.users;
        QComment comment = QComment.comment;
        QPostLike postLike = QPostLike.postLike;

        /* 게시글 조회 */
        List<Tuple> postTuples = jpaQueryFactory
                .select(
                        post.id,
                        post.title,
                        post.views.coalesce(0),
                        post.isNotice,
                        post.popularFlag,
                        post.createdAt,
                        post.user.id,
                        user.userName
                )
                .from(post)
                .join(post.user, user)
                .where(post.isNotice.eq(false))
                .orderBy(post.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        if (postTuples.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        List<Long> postIds = postTuples.stream()
                .map(tuple -> tuple.get(post.id))
                .collect(Collectors.toList());

        /* 댓글 수 집계 */
        NumberExpression<Long> commentCountExpr = comment.count().coalesce(0L).as("commentCount");

        Map<Long, Integer> commentCounts = jpaQueryFactory
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
                        }
                ));

        /* 좋아요 수 집계 */
        NumberExpression<Long> likeCountExpr = postLike.count().coalesce(0L).as("likeCount");

        Map<Long, Integer> likeCounts = jpaQueryFactory
                .select(postLike.post.id, likeCountExpr)
                .from(postLike)
                .where(postLike.post.id.in(postIds))
                .groupBy(postLike.post.id)
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(postLike.post.id),
                        tuple -> {
                            Long count = tuple.get(likeCountExpr);
                            return count != null ? Math.toIntExact(count) : 0;
                        }
                ));

        /* DTO 생성 */
        List<SimplePostDTO> results = postTuples.stream()
                .map(tuple -> {
                    Integer views = tuple.get(post.views.coalesce(0));
                    return new SimplePostDTO(
                            tuple.get(post.id),
                            tuple.get(post.user.id),
                            tuple.get(user.userName),
                            tuple.get(post.title),
                            commentCounts.getOrDefault(tuple.get(post.id), 0),
                            likeCounts.getOrDefault(tuple.get(post.id), 0),
                            views != null ? views : 0,
                            tuple.get(post.createdAt),
                            false
                    );
                })
                .collect(Collectors.toList());

        /* 전체 카운트 */
        Long total = jpaQueryFactory
                .select(post.count())
                .from(post)
                .where(post.isNotice.eq(false))
                .fetchOne();

        return new PageImpl<>(results, pageable, total != null ? total : 0L);
    }


    // 1일 이내의 글 중에서 추천 수가 가장 높은 글 상위 5개를 실시간 인기글로 등록
    @Override
    public void updateRealtimePopularPosts() {
        QPost post = QPost.post;
        QPostLike postLike = QPostLike.postLike;

        // 1. 모든 게시글의 실시간 인기글 컬럼 초기화
        jpaQueryFactory.update(post)
                .set(post.popularFlag, null)
                .execute();

        // 2. 실시간 인기글 조건에 맞는 상위 5개 조회
        List<Long> popularPostIds = jpaQueryFactory
                .select(post.id)
                .from(post)
                .leftJoin(postLike).on(post.id.eq(postLike.post.id))
                .where(post.createdAt.after(Instant.now().minus(1, ChronoUnit.DAYS)))
                .groupBy(post.id)
                .orderBy(postLike.count().desc())
                .limit(5)
                .fetch();

        // 3. 해당 게시글들의 실시간 인기글 컬럼 true로 설정
        if (!popularPostIds.isEmpty()) {
            jpaQueryFactory.update(post)
                    .set(post.isRealtimePopular, true)
                    .where(post.id.in(popularPostIds))
                    .execute();
        }
    }


    // 7일 이내의 글 중에서 추천 수가 가장 높은 글 상위 5개를 주간 인기글로 등록
    @Override
    public List<Post> updateWeeklyPopularPosts() {
        QPost post = QPost.post;
        QPostLike postLike = QPostLike.postLike;

        // 기존 주간 인기글 초기화
        jpaQueryFactory.update(post)
                .set(post.isWeeklyPopular, false)
                .execute();

        // 7일 이내의 글 중 추천 수 많은 상위 5개 선정
        List<Long> popularPostIds = jpaQueryFactory
                .select(post.id)
                .from(post)
                .leftJoin(postLike).on(post.id.eq(postLike.post.id))
                .where(post.createdAt.after(Instant.now().minus(7, ChronoUnit.DAYS)))
                .groupBy(post.id)
                .orderBy(postLike.count().desc())
                .limit(5)
                .fetch();

        if (!popularPostIds.isEmpty()) {
            jpaQueryFactory.update(post)
                    .set(post.isWeeklyPopular, true)
                    .where(post.id.in(popularPostIds))
                    .execute();
        }

        if (popularPostIds.isEmpty()) {
            return Collections.emptyList();
        }

        return jpaQueryFactory
                .selectFrom(post)
                .where(post.id.in(popularPostIds))
                .fetch();
    }


    // 추천 수가 20개 이상인 글을 명예의 전당에 등록
    @Override
    public List<Post> updateHallOfFamePosts() {
        QPost post = QPost.post;
        QPostLike postLike = QPostLike.postLike;

        // 추천 수 20개 이상인 게시글 ID 추출
        List<Long> hofPostIds = jpaQueryFactory
                .select(post.id)
                .from(post)
                .leftJoin(postLike).on(post.id.eq(postLike.post.id))
                .groupBy(post.id)
                .having(postLike.count().goe(20))
                .fetch();

        // 먼저 기존 컬럼 초기화
        jpaQueryFactory.update(post)
                .set(post.isHallOfFame, false)
                .execute();

        if (!hofPostIds.isEmpty()) {
            jpaQueryFactory.update(post)
                    .set(post.isHallOfFame, true)
                    .where(post.id.in(hofPostIds))
                    .execute();
        }

        if (hofPostIds.isEmpty()) {
            return Collections.emptyList();
        }

        return jpaQueryFactory
                .selectFrom(post)
                .where(post.id.in(hofPostIds))
                .fetch();
    }


    // 해당 유저가 추천 누른 글 목록 반환
    @Override
    public Page<Post> findByLikedPosts(Long userId, Pageable pageable) {
        QPost post = QPost.post;
        QPostLike postLike = QPostLike.postLike;

        // 사용자가 좋아요한 게시물 ID 목록 조회 (페이징 적용)
        List<Post> posts = jpaQueryFactory
                .selectFrom(post)
                .join(postLike)
                .on(post.id.eq(postLike.post.id))
                .where(postLike.user.id.eq(userId))
                .orderBy(post.createdAt.desc()) // 최신순으로 정렬
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 카운트 쿼리 실행
        Long total = jpaQueryFactory
                .select(post.count())
                .from(post)
                .join(postLike)
                .on(post.id.eq(postLike.post.id))
                .where(postLike.user.id.eq(userId))
                .fetchOne();

        // null 안전성을 고려한 코드로 수정
        return new PageImpl<>(posts, pageable, total == null ? 0L : total);
    }
}
