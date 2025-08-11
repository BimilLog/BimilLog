package jaeik.growfarm.repository.post.user;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.dto.post.SimplePostResDTO;
import jaeik.growfarm.entity.post.QPost;
import jaeik.growfarm.entity.post.QPostLike;
import jaeik.growfarm.repository.post.PostBaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>사용자별 게시글 조회 및 관리 구현체</h2>
 * <p>
 * 사용자가 작성한 글, 추천한 글 조회 기능과 게시글 삭제 및 캐시 동기화 기능을 담당하는 레포지터리
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
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
     * @since 2.0.0
     */
    @Transactional(readOnly = true)
    public Page<SimplePostResDTO> findPostsByUserId(Long userId, Pageable pageable) {
        QPost post = QPost.post;
        return fetchPosts(post.user.id.eq(userId), pageable);
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
     * @since 2.0.0
     */
    @Transactional(readOnly = true)
    public Page<SimplePostResDTO> findLikedPostsByUserId(Long userId, Pageable pageable) {
        QPost post = QPost.post;
        QPostLike postLike = QPostLike.postLike;

        BooleanExpression likedPostCondition = post.id.in(
                jpaQueryFactory
                        .select(postLike.post.id)
                        .from(postLike)
                        .where(postLike.user.id.eq(userId))
        );

        return fetchPosts(likedPostCondition, pageable);
    }
}