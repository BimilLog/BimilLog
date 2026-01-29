package jaeik.bimillog.domain.comment.service;

import jaeik.bimillog.application.comment.dto.CommentDTO;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.comment.entity.CommentInfo;
import jaeik.bimillog.domain.comment.entity.MemberActivityComment;
import jaeik.bimillog.domain.comment.repository.CommentQueryRepository;
import jaeik.bimillog.domain.comment.repository.CommentRepository;
import jaeik.bimillog.domain.global.entity.CustomUserDetails;
import jaeik.bimillog.domain.post.adapter.PostToCommentAdapter;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * <h2>댓글 조회 서비스</h2>
 * <p>댓글 조회 유스케이스의 구현체입니다.</p>
 * <p>인기 댓글 조회, 과거순 댓글 조회, 사용자별 댓글 조회</p>
 * <p>게시글별 댓글 수 조회, 페이지네이션 지원</p>
 * <p>QueryDSL을 사용한 타입 안전한 쿼리 처리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommentQueryService {
    private final CommentQueryRepository commentQueryRepository;
    private final CommentRepository commentRepository;

    /**
     * <h3>댓글 조회</h3>
     * <p>인기 댓글과 일반 댓글 함께 조회</p>
     *
     * @param postId      게시글 ID
     * @param pageable    페이지 정보
     * @param userDetails 사용자 인증 정보
     * @return Page<CommentInfo> 과거순 댓글 페이지
     * @author Jaeik
     * @since 2.7.0
     */
    public CommentDTO findComments(Long postId, Pageable pageable, CustomUserDetails userDetails) {
        Long memberId = userDetails != null ? userDetails.getMemberId() : null;
        Page<CommentInfo> comments = commentQueryRepository.findComments(postId, pageable, memberId);
        List<CommentInfo> popularComments = commentQueryRepository.findPopularComments(postId, memberId);
        return CommentDTO.from(popularComments, comments);
    }

    /**
     * <h3>댓글 ID로 댓글 조회</h3>
     * <p>댓글 ID로 댓글을 조회합니다.</p>
     * <p>존재하지 않는 댓글 ID인 경우 예외를 발생시킵니다.</p>
     * <p>CommentCommandService에서 권한 검증과 AdminCommandService에서 신고 처리용으로 사용됩니다.</p>
     *
     * @param commentId 댓글 ID
     * @return Comment 조회된 댓글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    public Comment findById(Long commentId) {
        return commentRepository.findById(commentId).orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
    }

    /**
     * <h3>사용자 작성, 추천 댓글 목록 조회</h3>
     * <p>특정 사용자가 작성, 추천한 댓글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param memberId   사용자 ID
     * @param pageable 페이지 정보
     * @return MemberActivityComment 마이페이지 댓글 정보
     * @author Jaeik
     * @since 2.0.0
     */
    public MemberActivityComment getMemberActivityComments(Long memberId, Pageable pageable) {
        Page<MemberActivityComment.SimpleCommentInfo> writeComments = commentQueryRepository.findCommentsByMemberId(memberId, pageable);
        Page<MemberActivityComment.SimpleCommentInfo> likedComments = commentQueryRepository.findLikedCommentsByMemberId(memberId, pageable);
        return new MemberActivityComment(writeComments, likedComments);
    }

    /**
     * <h3>게시글 ID 목록에 대한 댓글 수 조회</h3>
     * <p>여러 게시글의 댓글 수를 배치로 조회합니다.</p>
     * <p>게시글 ID 목록을 한 번에 처리하여 각 게시글별 댓글 수를 반환합니다.</p>
     * <p>{@link PostToCommentAdapter}에서 게시글별 댓글 수 배치 조회 시 호출됩니다.</p>
     *
     * @param postIds 게시글 ID 목록
     * @return Map<Long, Integer> 게시글 ID를 키로, 댓글 수를 값으로 하는 맵
     * @author Jaeik
     * @since 2.0.0
     */
    public Map<Long, Integer> findCommentCountsByPostIds(List<Long> postIds) {
        return commentQueryRepository.findCommentCountsByPostIds(postIds);
    }
}
