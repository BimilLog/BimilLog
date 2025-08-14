package jaeik.growfarm.domain.post.infrastructure.adapter.out.persistence;

import jaeik.growfarm.domain.post.application.port.out.PostCommandPort;
import jaeik.growfarm.domain.post.application.port.out.PostLikeCommandPort;
import jaeik.growfarm.domain.post.application.port.out.PostLikeQueryPort;
import jaeik.growfarm.domain.post.application.port.out.PostQueryPort;
import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.domain.post.entity.PostLike;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.dto.post.SimplePostResDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * <h2>게시글 영속성 어댑터</h2>
 * <p>게시글 관련 데이터의 저장, 조회, 삭제, 추천 관리 등을 처리하는 영속성 어댑터입니다.</p>
 * <p>통합된 Port 인터페이스들을 구현하여 CQRS 패턴을 지원합니다:</p>
 * <ul>
 *     <li>PostCommandPort: 게시글 생성/수정/삭제 관리</li>
 *     <li>LoadPostPort: 게시글 조회 기능</li>
 *     <li>PostLikeCommandPort: 추천 생성/삭제 관리</li>
 *     <li>PostLikeQueryPort: 추천 존재여부/개수 조회</li>
 * </ul>
 * <p>ISP(Interface Segregation Principle)를 따라 각 책임을 명확히 분리합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class PostQueryPersistenceAdapter implements
        PostCommandPort, PostQueryPort,
        PostLikeCommandPort, PostLikeQueryPort {

    private final PostJpaRepository postJpaRepository;
    private final PostLikeJpaRepository postLikeJpaRepository;
    private final PostQueryDslRepository postQueryDslRepository;

    /**
     * <h3>게시글 저장</h3>
     * <p>새로운 게시글 엔티티를 데이터베이스에 저장합니다.</p>
     *
     * @param post 저장할 게시글 엔티티
     * @return 저장된 게시글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Post save(Post post) {
        return postJpaRepository.save(post);
    }

    /**
     * <h3>ID로 게시글 조회</h3>
     * <p>주어진 ID를 사용하여 게시글을 조회합니다.</p>
     *
     * @param id 조회할 게시글 ID
     * @return 조회된 게시글 (Optional)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<Post> findById(Long id) {
        return postJpaRepository.findById(id);
    }

    /**
     * <h3>페이지별 게시글 조회</h3>
     * <p>페이지 정보에 따라 게시글 목록을 조회합니다.</p>
     *
     * @param pageable 페이지 정보
     * @return 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<SimplePostResDTO> findByPage(Pageable pageable) {
        return postQueryDslRepository.findSimplePost(pageable);
    }

    /**
     * <h3>검색을 통한 게시글 조회</h3>
     * <p>검색 유형과 쿼리에 따라 게시글을 검색하고 페이지네이션합니다.</p>
     *
     * @param type     검색 유형
     * @param query    검색어
     * @param pageable 페이지 정보
     * @return 검색된 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<SimplePostResDTO> findBySearch(String type, String query, Pageable pageable) {
        return postQueryDslRepository.searchPosts(query, type, pageable);
    }

    /**
     * <h3>조회수 증가</h3>
     * <p>주어진 게시글의 조회수를 1 증가시키고 저장합니다.</p>
     *
     * @param post 조회수를 증가시킬 게시글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void incrementView(Post post) {
        post.incrementView();
        postJpaRepository.save(post);
    }

    /**
     * <h3>게시글 삭제</h3>
     * <p>주어진 게시글 엔티티를 데이터베이스에서 삭제합니다.</p>
     *
     * @param post 삭제할 게시글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void delete(Post post) {
        postJpaRepository.delete(post);
    }

    /**
     * <h3>게시글 추천 저장</h3>
     * <p>새로운 게시글 추천 엔티티를 저장합니다.</p>
     *
     * @param postLike 저장할 PostLike 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void save(PostLike postLike) {
        postLikeJpaRepository.save(postLike);
    }

    /**
     * <h3>사용자와 게시글로 추천 삭제</h3>
     * <p>특정 사용자 및 게시글에 해당하는 추천를 삭제합니다.</p>
     *
     * @param user 삭제할 추천의 사용자 엔티티
     * @param post 삭제할 추천의 게시글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteByUserAndPost(User user, Post post) {
        postLikeJpaRepository.deleteByUserAndPost(user, post);
    }

    /**
     * <h3>게시글 ID로 모든 추천 삭제</h3>
     * <p>주어진 게시글 ID에 해당하는 모든 추천 기록을 삭제합니다.</p>
     *
     * @param postId 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteAllByPostId(Long postId) {
        postLikeJpaRepository.deleteAllByPostId(postId);
    }

    /**
     * <h3>사용자와 게시글로 추천 존재 여부 확인</h3>
     * <p>특정 사용자 및 게시글에 추천가 존재하는지 확인합니다.</p>
     *
     * @param user 사용자 엔티티
     * @param post 게시글 엔티티
     * @return 추천가 존재하면 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public boolean existsByUserAndPost(User user, Post post) {
        return postLikeJpaRepository.existsByUserAndPost(user, post);
    }

    /**
     * <h3>게시글 추천 수 조회</h3>
     * <p>특정 게시글의 추천 총 개수를 조회합니다.</p>
     *
     * @param post 게시글 엔티티
     * @return 해당 게시글의 추천 개수
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public long countByPost(Post post) {
        return postLikeJpaRepository.countByPost(post);
    }

    /**
     * <h3>사용자 작성 게시글 목록 조회</h3>
     * <p>특정 사용자가 작성한 게시글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return 작성한 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<SimplePostResDTO> findPostsByUserId(Long userId, Pageable pageable) {
        return postQueryDslRepository.findPostsByUserId(userId, pageable);
    }

    /**
     * <h3>사용자 추천한 게시글 목록 조회</h3>
     * <p>특정 사용자가 추천한 게시글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return 추천한 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<SimplePostResDTO> findLikedPostsByUserId(Long userId, Pageable pageable) {
        return postQueryDslRepository.findLikedPostsByUserId(userId, pageable);
    }


}
