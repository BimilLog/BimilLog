package jaeik.bimillog.domain.post.application.port.out;

import jaeik.bimillog.domain.post.application.service.PostCommandService;

import java.util.List;
import java.util.Map;

/**
 * <h2>게시글 댓글 조회 포트</h2>
 * <p>
 * Post 도메인에서 Comment 도메인의 데이터를 조회하기 위한 포트입니다.
 * </p>
 * <p>게시글 목록과 함께 댓글 수 표시</p>
 * <p>댓글 수 배치 조회</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PostToCommentPort {

    /**
     * <h3>게시글 ID 목록별 댓글 수 배치 조회</h3>
     * <p>여러 게시글에 속한 댓글 수를 한 번의 쿼리로 배치 조회합니다.</p>
     * <p>게시판 목록 조회 시 각 게시글의 댓글 수를 표시하기 위해 호출됩니다.</p>
     *
     * @param postIds 댓글 수를 조회할 게시글 ID 목록
     * @return 게시글 ID를 키로, 해당 게시글의 댓글 수를 값으로 하는 맵
     * @author Jaeik
     * @since 2.0.0
     */
    Map<Long, Integer> findCommentCountsByPostIds(List<Long> postIds);

    /**
     * <h3>특정 글의 모든 댓글 삭제</h3>
     * <p>Post 도메인에서 Comment 도메인으로 댓글 삭제를 요청하는 아웃바운드 포트입니다.</p>
     * <p>헥사고날 아키텍처에서 도메인 간 의존성을 역전시켜 Post가 Comment를 직접 참조하지 않도록 합니다.</p>
     * <p>{@link PostCommandService#deletePost}에서 게시글 삭제 시 연관 댓글 제거를 위해 호출됩니다.</p>
     *
     * @param postId 댓글을 삭제할 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteCommentInPost(Long postId);
}