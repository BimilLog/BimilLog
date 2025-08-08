package jaeik.growfarm.repository.post;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.dto.post.PostDTO;
import jaeik.growfarm.dto.post.SimplePostDTO;
import jaeik.growfarm.entity.post.QPost;
import jaeik.growfarm.entity.post.QPostLike;
import jaeik.growfarm.entity.user.QUsers;
import jaeik.growfarm.repository.comment.CommentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * <h2>게시글 조회 구현체</h2>
 * <p>
 * 게시글 목록/상세 조회 기능을 담당한다.
 * </p>
 *
 * @author Jaeik
 * @version 1.1.0
 * @since 1.1.0
 */
@Repository
public class PostReadRepositoryImpl extends PostBaseRepository implements PostReadRepository {

    public PostReadRepositoryImpl(JPAQueryFactory jpaQueryFactory,
                                  CommentRepository commentRepository) {
        super(jpaQueryFactory, commentRepository);
    }

    /**
     * <h3>게시글 목록 조회</h3>
     * <p>
     * 최신순으로 페이징하여 게시글 목록을 조회한다. 공지글과 일반글을 모두 포함한다.
     * </p>
     * <p>각 게시글의 댓글 수와 추천 수도 함께 반환한다.</p>
     * @param pageable 페이지 정보
     * @return 게시글 목록 페이지
     * @author Jaeik
     * @since 1.1.0
     */
    @Transactional(readOnly = true)
    public Page<SimplePostDTO> findPostsWithCommentAndLikeCounts(Pageable pageable) {
        QPost post = QPost.post;
        QUsers user = QUsers.users;

        List<Tuple> postTuples = fetchPosts(post, user, null, pageable);
        Long total = fetchTotalCount(post, user, null);

        return processPostTuples(postTuples, post, user, pageable, false, total);
    }

    /**
     * <h3>게시글 상세 조회</h3>
     * <p>
     * 게시글 정보, 좋아요 수, 사용자 좋아요 여부를 조회한다.
     * </p>
     * @param postId 게시글 ID
     * @param userId 사용자 ID (null일 경우 좋아요 여부는 조회하지 않음)
     * @return 게시글 상세 정보 DTO
     * @author Jaeik
     * @since 1.1.0
     */
    @Transactional(readOnly = true)
    public PostDTO findPostById(Long postId, Long userId) {
        QPost post = QPost.post;
        QUsers user = QUsers.users;

        Tuple postTuple = jpaQueryFactory
                .select(
                        post.id,
                        post.title,
                        post.content,
                        post.views,
                        post.isNotice,
                        post.popularFlag,
                        post.createdAt,
                        post.user.id,
                        user.userName)
                .from(post)
                .leftJoin(post.user, user)
                .where(post.id.eq(postId))
                .fetchOne();

        if (postTuple == null) {
            return null;
        }

        List<Long> postIds = Collections.singletonList(postId);

        Map<Long, Integer> likeCounts = fetchLikeCounts(postIds);

        boolean userLike = false;
        if (userId != null) {
            QPostLike userPostLike = QPostLike.postLike;
            Long likeCount = jpaQueryFactory
                    .select(userPostLike.count())
                    .from(userPostLike)
                    .where(userPostLike.post.id.eq(postId).and(userPostLike.user.id.eq(userId)))
                    .fetchOne();
            userLike = likeCount != null && likeCount > 0;
        }

        Integer views = postTuple.get(post.views);
        String userName = postTuple.get(user.userName);
        return PostDTO.existedPost(
                postTuple.get(post.id),
                postTuple.get(post.user.id),
                userName != null ? userName : "익명",
                postTuple.get(post.title),
                postTuple.get(post.content),
                views != null ? views : 0,
                likeCounts.getOrDefault(postId, 0),
                Boolean.TRUE.equals(postTuple.get(post.isNotice)),
                postTuple.get(post.popularFlag),
                postTuple.get(post.createdAt),
                userLike);
    }
}


