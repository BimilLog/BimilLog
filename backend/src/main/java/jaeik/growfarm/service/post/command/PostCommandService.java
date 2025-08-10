package jaeik.growfarm.service.post.command;

import jaeik.growfarm.dto.post.FullPostResDTO;
import jaeik.growfarm.dto.post.PostReqDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;

/**
 * <h2>게시글 명령 서비스 인터페이스</h2>
 * <p>
 * 게시글 CUD(Create, Update, Delete) 및 좋아요 기능을 담당하는 서비스 인터페이스
 * </p>
 * 
 * @author Jaeik
 * @version 2.0.0
 */
public interface PostCommandService {

    /**
     * <h3>게시글 작성</h3>
     * <p>
     * 새로운 게시글을 작성하고 저장한다.
     * </p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @param postReqDTO  게시글 작성 요청 DTO
     * @return 작성된 게시글 DTO
     * @author Jaeik
     * @since 2.0.0
     */
    FullPostResDTO writePost(CustomUserDetails userDetails, PostReqDTO postReqDTO);

    /**
     * <h3>게시글 수정</h3>
     * <p>
     * 게시글 작성자만 게시글을 수정할 수 있습니다.
     * </p>
     *
     * @param userDetails 현재 로그인 한 사용자 정보
     * @param postReqDTO     수정할 게시글 정보 DTO
     * @author Jaeik
     * @since 2.0.0
     */
    void updatePost(CustomUserDetails userDetails, PostReqDTO postReqDTO);

    /**
     * <h3>게시글 삭제</h3>
     * <p>
     * 게시글 작성자만 게시글을 삭제할 수 있습니다.
     * </p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @param postReqDTO     게시글 정보 DTO
     * @author Jaeik
     * @since 2.0.0
     */
    void deletePost(CustomUserDetails userDetails, PostReqDTO postReqDTO);

    /**
     * <h3>게시글 추천</h3>
     * <p>
     * 게시글을 추천하거나 추천 취소한다.
     * </p>
     *
     * @param fullPostResDTO     추천할 게시글 정보 DTO
     * @param userDetails 현재 로그인한 사용자 정보
     * @author Jaeik
     * @since 2.0.0
     */
    void likePost(FullPostResDTO fullPostResDTO, CustomUserDetails userDetails);
}