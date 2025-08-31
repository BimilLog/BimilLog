package jaeik.bimillog.domain.user.application.service;

import jaeik.bimillog.domain.user.application.port.in.UserActivityUseCase;
import jaeik.bimillog.domain.user.application.port.out.LoadCommentPort;
import jaeik.bimillog.domain.user.application.port.out.LoadPostPort;
import jaeik.bimillog.domain.comment.entity.SimpleCommentInfo;
import jaeik.bimillog.domain.post.entity.PostSearchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>사용자 활동 조회 서비스</h2>
 * <p>UserActivityUseCase의 구현체. 사용자의 게시글, 댓글 등 활동 내역 조회 로직을 담당합니다.</p>
 * <p>순환참조 해결을 위해 UserQueryService에서 활동 관련 메서드들을 분리한 서비스입니다.</p>
 *
 * @author jaeik
 * @version 2.0.0
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserActivityService implements UserActivityUseCase {

    private final LoadPostPort loadPostPort;
    private final LoadCommentPort loadCommentPort;

    /**
     * <h3>사용자 작성 게시글 목록 조회</h3>
     * <p>해당 사용자가 작성한 게시글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return 작성한 게시글 목록 페이지
     * @author jaeik
     * @since 2.0.0
     */
    @Override
    public Page<PostSearchResult> getUserPosts(Long userId, Pageable pageable) {
        return loadPostPort.findPostsByUserId(userId, pageable);
    }

    /**
     * <h3>사용자 추천한 게시글 목록 조회</h3>
     * <p>해당 사용자가 추천한 게시글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return 추천한 게시글 목록 페이지
     * @author jaeik
     * @since 2.0.0
     */
    @Override
    public Page<PostSearchResult> getUserLikedPosts(Long userId, Pageable pageable) {
        return loadPostPort.findLikedPostsByUserId(userId, pageable);
    }

    /**
     * <h3>사용자 작성 댓글 목록 조회</h3>
     * <p>해당 사용자가 작성한 댓글 목록을 페이지네이션으로 조회합니다.</p>
     * <p>헥사고날 아키텍처 원칙에 따라 LoadCommentPort(out포트) -> LoadCommentAdapter(out어댑터) -> CommentQueryUseCase(Comment의 in포트) 순서로 접근합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return 작성한 댓글 목록 페이지
     * @author jaeik
     * @since 2.0.0
     */
    @Override
    public Page<SimpleCommentInfo> getUserComments(Long userId, Pageable pageable) {
        return loadCommentPort.findCommentsByUserId(userId, pageable);
    }

    /**
     * <h3>사용자 추천한 댓글 목록 조회</h3>
     * <p>해당 사용자가 추천한 댓글 목록을 페이지네이션으로 조회합니다.</p>
     * <p>헥사고날 아키텍처 원칙에 따라 LoadCommentPort(out포트) -> LoadCommentAdapter(out어댑터) -> CommentQueryUseCase(Comment의 in포트) 순서로 접근합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return 추천한 댓글 목록 페이지
     * @author jaeik
     * @since 2.0.0
     */
    @Override
    public Page<SimpleCommentInfo> getUserLikedComments(Long userId, Pageable pageable) {
        return loadCommentPort.findLikedCommentsByUserId(userId, pageable);
    }
}