package jaeik.growfarm.repository.post;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.dto.post.SimplePostDTO;
import jaeik.growfarm.entity.post.QPost;
import jaeik.growfarm.entity.post.QPostLike;
import jaeik.growfarm.entity.user.QUsers;
import jaeik.growfarm.repository.comment.CommentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <h2>사용자별 게시글 조회 및 관리 구현체</h2>
 * <p>
 * 사용자가 작성한 글, 추천한 글 조회 기능과 게시글 삭제 및 캐시 동기화 기능을 담당하는 레포지터리
 * </p>
 *
 * @author Jaeik
 * @version 1.1.0
 */
@Slf4j
@Repository
public class PostUserRepositoryImpl extends PostBaseRepository implements PostUserRepository {

    public PostUserRepositoryImpl(JPAQueryFactory jpaQueryFactory, CommentRepository commentRepository) {
        super(jpaQueryFactory, commentRepository);
    }

    /**
     * <h3>사용자 작성 글 목록 조회</h3>
     * <p>
     * 사용자 ID를 기준으로 해당 사용자가 작성한 글 목록을 조회한다.
     * </p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return 사용자가 작성한 글 목록
     * @author Jaeik
     * @since 1.0.0
     */
    @Transactional(readOnly = true)
    public Page<SimplePostDTO> findPostsByUserId(Long userId, Pageable pageable) {
        QPost post = QPost.post;
        QUsers user = QUsers.users;

        BooleanExpression userCondition = post.user.id.eq(userId);
        BooleanExpression baseCondition = post.isNotice.eq(false);
        BooleanExpression finalCondition = baseCondition.and(userCondition);

        List<Tuple> postTuples = fetchPosts(post, user, finalCondition, pageable);
        Long total = fetchTotalCount(post, user, finalCondition);

        return processPostTuples(postTuples, post, user, pageable, false, total);
    }

    /**
     * <h3>사용자가 추천한 글 목록 조회</h3>
     * <p>
     * 사용자 ID를 기준으로 해당 사용자가 추천한 글 목록을 조회한다.
     * </p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return 사용자가 추천한 글 목록
     * @author Jaeik
     * @since 1.0.0
     */
    @Transactional(readOnly = true)
    public Page<SimplePostDTO> findLikedPostsByUserId(Long userId, Pageable pageable) {
        QPost post = QPost.post;
        QUsers user = QUsers.users;
        QPostLike postLike = QPostLike.postLike;

        List<Tuple> postTuples = jpaQueryFactory
                .select(
                        post.id,
                        post.title,
                        post.views.coalesce(0),
                        post.isNotice,
                        post.popularFlag,
                        post.createdAt,
                        post.user.id,
                        user.userName)
                .from(post)
                .join(post.user, user)
                .join(postLike).on(postLike.post.id.eq(post.id))
                .where(postLike.user.id.eq(userId))
                .orderBy(post.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = jpaQueryFactory
                .select(post.count())
                .from(post)
                .join(postLike).on(postLike.post.id.eq(post.id))
                .where(postLike.user.id.eq(userId))
                .fetchOne();

        return processPostTuples(postTuples, post, user, pageable, true, total);
    }
}