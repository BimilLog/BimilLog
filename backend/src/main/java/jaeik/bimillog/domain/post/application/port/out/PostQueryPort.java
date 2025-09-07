package jaeik.bimillog.domain.post.application.port.out;

import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSearchResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * <h2>게시글 조회 인터페이스</h2>
 * <p>
 *     게시글 데이터를 조회하는 Port 인터페이스입니다.
 * </p>
 *
 * @author jaeik
 * @version 2.0.0
 */
public interface PostQueryPort {

    /**
     * <h3>ID로 게시글 조회</h3>
     * <p>
     *     게시글 ID를 이용하여 특정 게시글을 조회합니다.
     * </p>
     * @param id 조회할 게시글 ID
     * @return Optional<Post>
     */
    Post findById(Long id);

    /**
     * <h3>게시판 목록 조회</h3>
     * <p>
     *     최신순으로 게시글 목록을 페이지네이션하여 조회합니다.
     * </p>
     * @param pageable 페이지 정보
     * @return 게시글 목록 페이지
     */
    Page<PostSearchResult> findByPage(Pageable pageable);

    /**
     * <h3>게시글 검색</h3>
     * <p>
     *     검색 유형과 검색어를 통해 게시글을 검색하고 최신순으로 페이지네이션합니다.
     * </p>
     * @param type 검색 유형
     * @param query 검색어
     * @param pageable 페이지 정보
     * @return 검색된 게시글 목록 페이지
     */
    Page<PostSearchResult> findBySearch(String type, String query, Pageable pageable);

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
    Page<PostSearchResult> findPostsByUserId(Long userId, Pageable pageable);

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
    Page<PostSearchResult> findLikedPostsByUserId(Long userId, Pageable pageable);

    /**
     * <h3>게시글 상세 정보 조회 </h3>
     * <p>게시글, 좋아요 수, 댓글 수, 사용자 좋아요 여부를 JOIN을 통해 한 번의 쿼리로 조회합니다.</p>
     * <p>기존의 4개 개별 쿼리를 1개 JOIN 쿼리로 최적화하여 성능을 개선합니다.</p>
     *
     * @param postId 조회할 게시글 ID
     * @param userId 현재 사용자 ID (좋아요 여부 확인용, null 가능)
     * @return 게시글 상세 정보 프로젝션 (게시글이 없으면 empty)
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<PostDetail> findPostDetailWithCounts(Long postId, Long userId);

}
