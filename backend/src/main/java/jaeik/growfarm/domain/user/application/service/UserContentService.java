package jaeik.growfarm.domain.user.application.service;

import jaeik.growfarm.domain.comment.application.port.out.LoadCommentPort;
import jaeik.growfarm.domain.post.application.port.out.LoadPostPort;
import jaeik.growfarm.dto.comment.SimpleCommentDTO;
import jaeik.growfarm.dto.post.SimplePostResDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>사용자 콘텐츠 서비스</h2>
 * <p>
 * 사용자가 작성한 게시글/댓글과 좋아요한 콘텐츠 조회를 담당
 * SRP: 사용자 콘텐츠 조회 기능만 담당
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserContentService {

    private final LoadPostPort loadPostPort;
    private final LoadCommentPort loadCommentPort;

    /**
     * <h3>유저 작성 글 목록 조회</h3>
     *
     * <p>
     * 해당 유저의 작성 글 목록을 페이지네이션으로 반환한다.
     * </p>
     *
     * @param page        페이지 번호
     * @param size        페이지 크기
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 작성 글 목록 페이지
     * @author Jaeik
     * @version 2.0.0
     * @since 2.0.0
     */
    public Page<SimplePostResDTO> getPostList(int page, int size, CustomUserDetails userDetails) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return loadPostPort.findPostsByUserId(userDetails.getUserId(), pageable);
    }

    /**
     * <h3>유저 작성 댓글 목록 조회</h3>
     *
     * <p>
     * 해당 유저의 작성 댓글 목록을 페이지네이션으로 반환한다.
     * </p>
     *
     * @param page        페이지 번호
     * @param size        페이지 크기
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 작성 댓글 목록 페이지
     * @author Jaeik
     * @version 2.0.0
     * @since 2.0.0
     */
    public Page<SimpleCommentDTO> getCommentList(int page, int size, CustomUserDetails userDetails) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return loadCommentPort.findCommentsByUserId(userDetails.getUserId(), pageable);
    }

    /**
     * <h3>사용자 추천한 글 목록 조회</h3>
     *
     * <p>
     * 해당 유저가 좋아요한 글 목록을 페이지네이션으로 반환한다.
     * </p>
     *
     * @param page        페이지 번호
     * @param size        페이지 크기
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 좋아요한 글 목록 페이지
     * @author Jaeik
     * @version 2.0.0
     * @since 2.0.0
     */
    public Page<SimplePostResDTO> getLikedPosts(int page, int size, CustomUserDetails userDetails) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return loadPostPort.findLikedPostsByUserId(userDetails.getUserId(), pageable);
    }

    /**
     * <h3>사용자 추천한 댓글 목록 조회</h3>
     *
     * <p>
     * 해당 유저가 추천한 댓글 목록을 페이지네이션으로 반환한다.
     * </p>
     *
     * @param page        페이지 번호
     * @param size        페이지 크기
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 추천한 댓글 목록 페이지
     * @author Jaeik
     * @version 2.0.0
     * @since 2.0.0
     */
    public Page<SimpleCommentDTO> getLikedComments(int page, int size, CustomUserDetails userDetails) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return loadCommentPort.findLikedCommentsByUserId(userDetails.getUserId(), pageable);
    }
}