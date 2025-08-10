package jaeik.growfarm.repository.post.read;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.dto.post.FullPostResDTO;
import jaeik.growfarm.dto.post.SimplePostResDTO;
import jaeik.growfarm.repository.comment.CommentRepository;
import jaeik.growfarm.repository.post.PostBaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <h2>게시글 조회 구현체</h2>
 * <p>
 * 게시글 목록/상세 조회 기능을 담당한다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
public class PostReadRepositoryImpl extends PostBaseRepository implements PostReadRepository {

    public PostReadRepositoryImpl(JPAQueryFactory jpaQueryFactory,
                                  CommentRepository commentRepository) {
        super(jpaQueryFactory, commentRepository);
    }

    /**
     * <h3>게시판 조회</h3>
     * <p>
     * 최신순으로 페이징하여 게시판을 조회한다. 공지글은 제외한다.
     * </p>
     * @param pageable 페이지 정보
     * @return 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional(readOnly = true)
    public Page<SimplePostResDTO> findSimplePost(Pageable pageable) {
        return fetchPosts(null, pageable);
    }

    /**
     * <h3>게시글 조회</h3>
     * <p>
     * 게시글 정보, 좋아요 수, 사용자 좋아요 여부를 조회한다.
     * </p>
     * @param postId 게시글 ID
     * @param userId 사용자 ID (null일 경우 좋아요 여부는 조회하지 않음)
     * @return 게시글 상세 정보 DTO
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional(readOnly = true)
    public FullPostResDTO findPostById(Long postId, Long userId) {
        return fetchPostDetail(postId, userId);
    }

}


