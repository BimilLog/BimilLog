package jaeik.growfarm.service.post.read;

import jaeik.growfarm.dto.post.PostDTO;
import jaeik.growfarm.dto.post.SimplePostDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;

/**
 * <h2>게시글 조회 서비스 인터페이스</h2>
 * <p>
 * 게시글 조회 관련 기능을 담당하는 서비스 인터페이스
 * </p>
 * 
 * @author Jaeik
 * @version 1.1.0
 */
public interface PostReadService {

    /**
     * <h3>게시판 조회</h3>
     * <p>
     * 최신순으로 게시글 목록을 페이지네이션으로 조회한다.
     * </p>
     *
     * @param page 페이지 번호
     * @param size 페이지 사이즈
     * @return 게시글 목록 페이지
     * @author Jaeik
     * @since 1.1.0
     */
    Page<SimplePostDTO> getBoard(int page, int size);

    /**
     * <h3>게시글 조회</h3>
     * <p>
     * 게시글 ID를 통해 게시글 상세 정보를 조회한다.
     * </p>
     *
     * @param postId      게시글 ID
     * @param userDetails 현재 로그인 한 사용자 정보
     * @return 게시글 상세 DTO
     * @author Jaeik
     * @since 1.1.0
     */
    PostDTO getPost(Long postId, CustomUserDetails userDetails);

    /**
     * <h3>게시글 조회수 증가</h3>
     * <p>
     * 게시글 조회 시 조회수를 증가시키고, 쿠키에 해당 게시글 ID를 저장한다.
     * </p>
     *
     * @param postId   게시글 ID
     * @param request  HTTP 요청 객체
     * @param response HTTP 응답 객체
     * @author Jaeik
     * @since 1.1.0
     */
    void incrementViewCount(Long postId, HttpServletRequest request, HttpServletResponse response);
}