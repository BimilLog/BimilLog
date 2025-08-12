package jaeik.growfarm.domain.post.application.port.in;

import jaeik.growfarm.dto.post.PostReqDTO;
import jaeik.growfarm.domain.user.domain.User;

/**
 * <h2>PostCommandUseCase</h2>
 * <p>
 *     게시글 생성, 수정, 삭제와 관련된 비즈니스 로직을 처리하는 UseCase 인터페이스입니다.
 * </p>
 * @author jaeik
 * @version 1.0
 */
public interface PostCommandUseCase {

    /**
     * <h3>게시글 작성</h3>
     * <p>
     *     새로운 게시글을 작성하고 저장합니다.
     * </p>
     * @param userId 현재 로그인한 사용자 ID
     * @param postReqDTO  게시글 작성 요청 DTO
     * @return 생성된 게시글의 ID
     */
    Long writePost(Long userId, PostReqDTO postReqDTO);

    /**
     * <h3>게시글 공지 설정</h3>
     * <p>
     *     특정 게시글을 공지로 설정합니다. 관리자 권한이 필요합니다.
     * </p>
     * @param postId 공지로 설정할 게시글 ID
     */
    void setPostAsNotice(Long postId);

    /**
     * <h3>게시글 공지 해제</h3>
     * <p>
     *     게시글의 공지 설정을 해제합니다. 관리자 권한이 필요합니다.
     * </p>
     * @param postId 공지 설정을 해제할 게시글 ID
     */
    void unsetPostAsNotice(Long postId);

    /**
     * <h3>게시글 수정</h3>
     * <p>
     *     게시글 작성자만 게시글을 수정할 수 있습니다.
     * </p>
     * @param userId 현재 로그인 한 사용자 ID
     * @param postId 수정할 게시글 ID
     * @param postReqDTO 수정할 게시글 정보 DTO
     */
    void updatePost(Long userId, Long postId, PostReqDTO postReqDTO);

    /**
     * <h3>게시글 삭제</h3>
     * <p>
     *     게시글 작성자만 게시글을 삭제할 수 있습니다.
     * </p>
     * @param userId 현재 로그인한 사용자 ID
     * @param postId 삭제할 게시글 ID
     */
    void deletePost(Long userId, Long postId);

    /**
     * <h3>게시글 추천</h3>
     * <p>
     *     게시글을 추천하거나 추천 취소합니다.
     * </p>
     * @param userId 현재 로그인한 사용자 ID
     * @param postId 추천할 게시글 ID
     */
    void likePost(Long userId, Long postId);

    /**
     * <h3>게시글 조회수 증가</h3>
     * <p>
     *     게시글의 조회수를 1 증가시킵니다.
     * </p>
     * @param postId 조회수를 증가시킬 게시글 ID
     */
    void incrementViewCount(Long postId);
}
