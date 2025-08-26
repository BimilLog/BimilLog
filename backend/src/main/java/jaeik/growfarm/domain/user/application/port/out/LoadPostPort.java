package jaeik.growfarm.domain.user.application.port.out;

import jaeik.growfarm.domain.post.entity.PostSearchResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * <h2>게시글 조회 포트</h2>
 * <p>User 도메인에서 Post 도메인의 데이터에 접근하기 위한 Out-Port</p>
 *
 * @author jaeik
 * @version 2.0.0
 */
public interface LoadPostPort {

    /**
     * <h3>사용자 작성 게시글 목록 조회</h3>
     * <p>특정 사용자가 작성한 게시글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return 작성한 게시글 목록 페이지
     * @author jaeik
     * @version 2.0.0
     */
    Page<PostSearchResult> findPostsByUserId(Long userId, Pageable pageable);

    /**
     * <h3>사용자 추천한 게시글 목록 조회</h3>
     * <p>특정 사용자가 추천한 게시글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return 추천한 게시글 목록 페이지
     * @author jaeik
     * @version 2.0.0
     */
    Page<PostSearchResult> findLikedPostsByUserId(Long userId, Pageable pageable);
}