package jaeik.growfarm.domain.post.application.port.in;

import jaeik.growfarm.dto.post.PostReqDTO;

/**
 * <h2>게시글 기본 명령 유스케이스</h2>
 * <p>게시글의 생성, 수정, 삭제 등 기본적인 CRUD 비즈니스 로직을 처리하는 유스케이스 인터페이스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PostCommandUseCase {

    /**
     * <h3>게시글 작성</h3>
     * <p>새로운 게시글을 작성하고 저장합니다.</p>
     * <p>작성자 정보와 함께 게시글을 생성하고 데이터베이스에 저장합니다.</p>
     *
     * @param userId     현재 로그인한 사용자 ID
     * @param postReqDTO 게시글 작성 요청 DTO
     * @return 생성된 게시글의 ID
     * @since 2.0.0
     * @author Jaeik
     */
    Long writePost(Long userId, PostReqDTO postReqDTO);

    /**
     * <h3>게시글 수정</h3>
     * <p>게시글 작성자만 게시글을 수정할 수 있습니다.</p>
     * <p>권한 검증 후 게시글 내용을 업데이트합니다.</p>
     *
     * @param userId     현재 로그인한 사용자 ID
     * @param postId     수정할 게시글 ID
     * @param postReqDTO 수정할 게시글 정보 DTO
     * @since 2.0.0
     * @author Jaeik
     */
    void updatePost(Long userId, Long postId, PostReqDTO postReqDTO);

    /**
     * <h3>게시글 삭제</h3>
     * <p>게시글 작성자만 게시글을 삭제할 수 있습니다.</p>
     * <p>권한 검증 후 게시글을 삭제하고 관련 데이터 정리 이벤트를 발행합니다.</p>
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param postId 삭제할 게시글 ID
     * @since 2.0.0
     * @author Jaeik
     */
    void deletePost(Long userId, Long postId);
}
