package jaeik.bimillog.domain.member.application.port.in;

import jaeik.bimillog.domain.comment.entity.SimpleCommentInfo;
import jaeik.bimillog.domain.post.entity.PostSearchResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * <h2>사용자 활동 조회 유스케이스</h2>
 * <p>사용자의 게시글, 댓글 등 활동 내역 조회 기능을 정의합니다.</p>
 *
 * @author jaeik
 * @version 2.0.0
 */
public interface MemberActivityUseCase {

    /**
     * <h3>사용자 작성 게시글 목록 조회</h3>
     * <p>해당 사용자가 작성한 게시글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param memberId   사용자 ID
     * @param pageable 페이지 정보
     * @return 작성한 게시글 목록 페이지
     * @author jaeik
     * @since 2.0.0
     */
    Page<PostSearchResult> getMemberPosts(Long memberId, Pageable pageable);

    /**
     * <h3>사용자 추천한 게시글 목록 조회</h3>
     * <p>해당 사용자가 추천한 게시글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param memberId   사용자 ID
     * @param pageable 페이지 정보
     * @return 추천한 게시글 목록 페이지
     * @author jaeik
     * @since 2.0.0
     */
    Page<PostSearchResult> getMemberLikedPosts(Long memberId, Pageable pageable);

    /**
     * <h3>사용자 작성 댓글 목록 조회</h3>
     * <p>해당 사용자가 작성한 댓글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param memberId   사용자 ID
     * @param pageable 페이지 정보
     * @return 작성한 댓글 목록 페이지
     * @author jaeik
     * @since 2.0.0
     */
    Page<SimpleCommentInfo> getMemberComments(Long memberId, Pageable pageable);

    /**
     * <h3>사용자 추천한 댓글 목록 조회</h3>
     * <p>해당 사용자가 추천한 댓글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param memberId   사용자 ID
     * @param pageable 페이지 정보
     * @return 추천한 댓글 목록 페이지
     * @author jaeik
     * @since 2.0.0
     */
    Page<SimpleCommentInfo> getMemberLikedComments(Long memberId, Pageable pageable);
}