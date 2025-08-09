package jaeik.growfarm.repository.post.user;

import jaeik.growfarm.dto.post.SimplePostResDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/**
 * <h2>게시글 사용자별 조회 저장소</h2>
 * <p>
 * 사용자별 게시글 조회 기능을 담당
 * </p>
 * 
 * @author jaeik
 * @version 2.0.0
 */
@Repository
public interface PostUserRepository {

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
    Page<SimplePostResDTO> findPostsByUserId(Long userId, Pageable pageable);

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
    Page<SimplePostResDTO> findLikedPostsByUserId(Long userId, Pageable pageable);
}