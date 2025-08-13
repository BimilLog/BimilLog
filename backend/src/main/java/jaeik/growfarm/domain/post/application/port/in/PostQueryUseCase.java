package jaeik.growfarm.domain.post.application.port.in;

import jaeik.growfarm.domain.post.entity.PostCacheFlag;
import jaeik.growfarm.dto.post.FullPostResDTO;
import jaeik.growfarm.dto.post.SimplePostResDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * <h2>PostQueryUseCase</h2>
 * <p>
 *     게시글 조회와 관련된 비즈니스 로직을 처리하는 UseCase 인터페이스입니다.
 * </p>
 * @author jaeik
 * @version 1.0
 */
public interface PostQueryUseCase {

    /**
     * <h3>게시판 조회</h3>
     * <p>
     *     최신순으로 게시글 목록을 페이지네이션으로 조회합니다.
     * </p>
     * @param pageable 페이지 정보
     * @return 게시글 목록 페이지
     */
    Page<SimplePostResDTO> getBoard(Pageable pageable);

    /**
     * <h3>게시글 상세 조회</h3>
     * <p>
     *     게시글 ID를 통해 게시글 상세 정보를 조회합니다.
     * </p>
     * @param postId      게시글 ID
     * @param userId     현재 로그인한 사용자 ID
     * @return 게시글 상세 정보 DTO
     */
    FullPostResDTO getPost(Long postId, Long userId);

    /**
     * <h3>게시글 검색</h3>
     * <p>
     *     검색 유형과 검색어를 통해 게시글을 검색하고 최신순으로 페이지네이션합니다.
     * </p>
     * @param type  검색 유형
     * @param query 검색어
     * @param pageable  페이지 정보
     * @return 검색된 게시글 목록 페이지
     */
    Page<SimplePostResDTO> searchPost(String type, String query, Pageable pageable);

    /**
     * <h3>인기 게시글 목록 조회</h3>
     * <p>
     *     캐시된 인기 게시글 목록(실시간, 주간, 레전드)을 조회합니다.
     * </p>
     * @param type 조회할 인기 게시글 유형
     * @return 인기 게시글 목록
     */
    List<SimplePostResDTO> getPopularPosts(PostCacheFlag type);

    /**
     * <h3>공지사항 목록 조회</h3>
     * <p>
     *     캐시된 공지사항 목록을 조회합니다.
     * </p>
     * @return 공지사항 목록
     */
    List<SimplePostResDTO> getNoticePosts();
}
