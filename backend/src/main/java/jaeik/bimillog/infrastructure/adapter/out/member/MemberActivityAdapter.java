package jaeik.bimillog.infrastructure.adapter.out.member;

import jaeik.bimillog.domain.comment.application.port.in.CommentQueryUseCase;
import jaeik.bimillog.domain.comment.entity.SimpleCommentInfo;
import jaeik.bimillog.domain.post.application.port.in.PostQueryUseCase;
import jaeik.bimillog.domain.post.entity.PostSearchResult;
import jaeik.bimillog.domain.member.application.port.out.MemberActivityPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * <h2>댓글 조회 어댑터</h2>
 * <p>사용자 도메인에서 댓글 도메인으로의 출력 어댑터입니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class MemberActivityAdapter implements MemberActivityPort {

    private final PostQueryUseCase postQueryUseCase;
    private final CommentQueryUseCase commentQueryUseCase;

    /**
     * <h3>사용자 작성 게시글 목록 조회</h3>
     * <p>특정 사용자가 작성한 게시글 목록을 게시글 도메인을 통해 조회합니다.</p>
     *
     * @param memberId   사용자 ID
     * @param pageable 페이지 정보
     * @return Page<PostSearchResult> 작성한 게시글 목록 페이지
     * @author jaeik
     * @since 2.0.0
     */
    @Override
    public Page<PostSearchResult> findPostsByMemberId(Long memberId, Pageable pageable) {
        return postQueryUseCase.getMemberPosts(memberId, pageable);
    }

    /**
     * <h3>사용자 추천한 게시글 목록 조회</h3>
     * <p>특정 사용자가 추천한 게시글 목록을 게시글 도메인을 통해 조회합니다.</p>
     *
     * @param memberId   사용자 ID
     * @param pageable 페이지 정보
     * @return Page<PostSearchResult> 추천한 게시글 목록 페이지
     * @author jaeik
     * @since 2.0.0
     */
    @Override
    public Page<PostSearchResult> findLikedPostsByMemberId(Long memberId, Pageable pageable) {
        return postQueryUseCase.getMemberLikedPosts(memberId, pageable);
    }

    /**
     * <h3>사용자 작성 댓글 목록 조회</h3>
     * <p>특정 사용자가 작성한 댓글 목록을 Comment 도메인을 통해 조회합니다.</p>
     *
     * @param memberId   사용자 ID
     * @param pageable 페이지 정보
     * @return Page<SimpleCommentInfo> 작성한 댓글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<SimpleCommentInfo> findCommentsByMemberId(Long memberId, Pageable pageable) {
        return commentQueryUseCase.getMemberComments(memberId, pageable);
    }

    /**
     * <h3>사용자 추천한 댓글 목록 조회</h3>
     * <p>특정 사용자가 추천한 댓글 목록을 Comment 도메인을 통해 조회합니다.</p>
     *
     * @param memberId   사용자 ID
     * @param pageable 페이지 정보
     * @return Page<SimpleCommentInfo> 추천한 댓글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<SimpleCommentInfo> findLikedCommentsByMemberId(Long memberId, Pageable pageable) {
        return commentQueryUseCase.getMemberLikedComments(memberId, pageable);
    }

}