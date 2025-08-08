package jaeik.growfarm.repository.post.popular;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.dto.post.SimplePostDTO;
import jaeik.growfarm.entity.comment.QComment;
import jaeik.growfarm.entity.post.PopularFlag;
import jaeik.growfarm.entity.post.QPost;
import jaeik.growfarm.entity.post.QPostLike;
import jaeik.growfarm.entity.user.QUsers;
import jaeik.growfarm.repository.comment.CommentRepository;
import jaeik.growfarm.repository.post.PostBaseRepository;
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
                .limit(50)
                .fetch();

        if (legendPostsData.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> legendPostIds = legendPostsData.stream()
                .map(tuple -> tuple.get(post.id))
                .collect(Collectors.toList());

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

    /**
     * <h3>인기글 플래그 초기화</h3>
     * <p>
     * 특정 인기글 플래그를 가진 게시글들의 플래그를 null로 초기화한다.
     * </p>
     *
     * @param popularFlag 초기화할 인기글 플래그
     * @author Jaeik
     * @since 1.1.0
     */
    private void resetPopularFlag(PopularFlag popularFlag) {
        QPost post = QPost.post;
        jpaQueryFactory.update(post)
                .set(post.popularFlag, (PopularFlag) null)
                .where(post.popularFlag.eq(popularFlag))
                .execute();
    }
    /**
     * <h3>인기글 플래그 설정 </h3>
     * <p>
     * 지정된 게시글들에 인기글 플래그를 설정한다.
     * </p>
     *
     * @param postIds     게시글 ID 목록
     * @param popularFlag 설정할 인기글 플래그
     * @author Jaeik
     * @since 1.1.0
     */
    private void applyPopularFlag(List<Long> postIds, PopularFlag popularFlag) {
        if (postIds == null || postIds.isEmpty()) {
            return;
        }
        QPost post = QPost.post;
        jpaQueryFactory.update(post)
                .set(post.popularFlag, popularFlag)
                .where(post.id.in(postIds))
                .execute();
    }
    /**
     * <h3>Tuple을 SimplePostDTO로 변환</h3>
     * <p>
     * 조회된 Tuple 데이터를 SimplePostDTO 리스트로 변환하는 공통 메서드
     * </p>
     *
     * @param tuples   조회된 Tuple 리스트
     * @param post     QPost 엔티티
     * @param user     QUsers 엔티티
     * @param comment  QComment 엔티티
     * @param postLike QPostLike 엔티티
     * @return SimplePostDTO 리스트
     * @author Jaeik
     * @since 1.0.0
     */
    private List<SimplePostDTO> convertTuplesToSimplePostDTOs(List<Tuple> tuples, QPost post, QUsers user,
                                                              QComment comment, QPostLike postLike) {
        return tuples.stream()
                .map(tuple -> SimplePostDTO.builder()
                        .postId(tuple.get(post.id))
                        .userId(tuple.get(post.user.id))
                        .userName(tuple.get(user.userName))
                        .title(tuple.get(post.title))
                        .commentCount(tuple.get(comment.count()) != null ? tuple.get(comment.count()).intValue() : 0)
                        .likes(tuple.get(postLike.count()) != null ? tuple.get(postLike.count()).intValue() : 0)
                        .views(tuple.get(post.views) != null ? tuple.get(post.views) : 0)
                        .createdAt(tuple.get(post.createdAt))
                        .is_notice(tuple.get(post.isNotice) != null && Boolean.TRUE.equals(tuple.get(post.isNotice)))
                        .user(tuple.get(user))
                        .build())
                .toList();
    }
}