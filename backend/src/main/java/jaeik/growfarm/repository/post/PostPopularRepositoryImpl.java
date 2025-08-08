package jaeik.growfarm.repository.post;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.dto.post.SimplePostDTO;
import jaeik.growfarm.entity.comment.QComment;
import jaeik.growfarm.entity.post.PopularFlag;
import jaeik.growfarm.entity.post.QPost;
import jaeik.growfarm.entity.post.QPostLike;
import jaeik.growfarm.entity.user.QUsers;
import jaeik.growfarm.repository.comment.CommentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <h2>인기글 관리 구현체</h2>
 * <p>
 * 인기글 선정 및 관리 기능을 담당하는 레포지터리
 * </p>
 *
 * @author Jaeik
 * @version 1.1.0
 */
@Slf4j
@Repository
public class PostPopularRepositoryImpl extends PostBaseRepository implements PostPopularRepository {

    public PostPopularRepositoryImpl(JPAQueryFactory jpaQueryFactory,
                                     CommentRepository commentRepository) {
        super(jpaQueryFactory, commentRepository);
    }

    /**
     * <h3>실시간 인기글 선정</h3>
     * <p>
     * 1일 이내의 글 중 추천 수가 가장 높은 상위 5개를 실시간 인기글로 등록한다.
     * </p>
     *
     * @return 실시간 인기글 목록
     * @author Jaeik
     * @since 1.0.0
     */
    @Transactional
    public List<SimplePostDTO> updateRealtimePopularPosts() {
        return updatePopularPosts(1, PopularFlag.REALTIME);
    }

    /**
     * <h3>주간 인기글 선정</h3>
     * <p>
     * 7일 이내의 글 중 추천 수가 가장 높은 상위 5개를 주간 인기글로 등록한다.
     * </p>
     *
     * @return 주간 인기글 목록
     * @author Jaeik
     * @since 1.0.0
     */
    @Transactional
    public List<SimplePostDTO> updateWeeklyPopularPosts() {
        return updatePopularPosts(7, PopularFlag.WEEKLY);
    }

    /**
     * <h3>레전드 게시글 선정</h3>
     * <p>
     * 추천 수가 20개 이상인 모든 게시글을 레전드 게시글로 등록한다.
     * </p>
     *
     * @return 레전드 게시글 목록
     * @author Jaeik
     * @since 1.0.0
     */
    @Transactional
    public List<SimplePostDTO> updateLegendPosts() {
        QPost post = QPost.post;
        QPostLike postLike = QPostLike.postLike;
        QComment comment = QComment.comment;
        QUsers user = QUsers.users;

        // 공통 메서드 사용: 기존 레전드 플래그 초기화
        resetPopularFlag(PopularFlag.LEGEND);

        List<Tuple> legendPostsData = jpaQueryFactory
                .select(
                        post.id,
                        post.user.id,
                        user.userName,
                        post.title,
                        post.views,
                        post.createdAt,
                        post.isNotice,
                        postLike.count().coalesce(0L),
                        comment.count().coalesce(0L),
                        user)
                .from(post)
                .leftJoin(user).on(post.user.id.eq(user.id))
                .join(postLike).on(post.id.eq(postLike.post.id))
                .leftJoin(comment).on(post.id.eq(comment.post.id))
                .groupBy(
                        post.id,
                        post.user.id,
                        user.userName,
                        post.title,
                        post.views,
                        post.createdAt,
                        post.isNotice,
                        user)
                .having(postLike.count().goe(20))
                .orderBy(postLike.count().desc())
                .fetch();

        if (legendPostsData.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> legendPostIds = legendPostsData.stream()
                .map(tuple -> tuple.get(post.id))
                .collect(Collectors.toList());

        // 공통 메서드 사용: 레전드 플래그 설정
        applyPopularFlag(legendPostIds, PopularFlag.LEGEND);

        return convertTuplesToSimplePostDTOs(legendPostsData, post, user, comment, postLike);
    }

    /**
     * <h3>인기글 선정 공통 로직</h3>
     * <p>
     * 지정된 기간 이내의 글 중 추천 수가 가장 높은 상위 5개를 인기글로 등록한다.
     * </p>
     *
     * @param days        조회 기간 (일 단위)
     * @param popularFlag 설정할 인기글 플래그
     * @return 인기글 목록
     * @author Jaeik
     * @since 1.0.0
     */
    private List<SimplePostDTO> updatePopularPosts(int days, PopularFlag popularFlag) {
        QPost post = QPost.post;
        QPostLike postLike = QPostLike.postLike;
        QComment comment = QComment.comment;
        QUsers user = QUsers.users;

        resetPopularFlag(popularFlag);

        List<Tuple> popularPostsData = jpaQueryFactory
                .select(
                        post.id,
                        post.user.id,
                        user.userName,
                        post.title,
                        post.views,
                        post.createdAt,
                        post.isNotice,
                        postLike.count().coalesce(0L),
                        comment.count().coalesce(0L),
                        user)
                .from(post)
                .leftJoin(user).on(post.user.id.eq(user.id))
                .join(postLike).on(post.id.eq(postLike.post.id))
                .leftJoin(comment).on(post.id.eq(comment.post.id))
                .where(post.createdAt.after(Instant.now().minus(days, ChronoUnit.DAYS)))
                .groupBy(
                        post.id,
                        post.user.id,
                        user.userName,
                        post.title,
                        post.views,
                        post.createdAt,
                        post.isNotice,
                        user)
                .orderBy(postLike.count().desc())
                .limit(5)
                .fetch();

        if (popularPostsData.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> popularPostIds = popularPostsData.stream()
                .map(tuple -> tuple.get(post.id))
                .collect(Collectors.toList());

        applyPopularFlag(popularPostIds, popularFlag);

        return convertTuplesToSimplePostDTOs(popularPostsData, post, user, comment, postLike);
    }
}