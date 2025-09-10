package jaeik.bimillog.domain.comment.application.service;

import jaeik.bimillog.domain.comment.application.port.in.CommentQueryUseCase;
import jaeik.bimillog.domain.comment.application.port.out.CommentQueryPort;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.comment.entity.CommentInfo;
import jaeik.bimillog.domain.comment.entity.SimpleCommentInfo;
import jaeik.bimillog.infrastructure.adapter.comment.in.web.CommentQueryController;
import jaeik.bimillog.infrastructure.adapter.post.out.comment.PostToCommentAdapter;
import jaeik.bimillog.infrastructure.adapter.user.out.comment.UserToCommentAdapter;
import jaeik.bimillog.infrastructure.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CommentQueryService implements CommentQueryUseCase {

    private final CommentQueryPort commentQueryPort;

    /**
     * <h3>인기 댓글 조회</h3>
     * <p>주어진 게시글의 인기 댓글 목록을 조회합니다.</p>
     * <p>추천 수가 높은 댓글들을 우선순위로 정렬하여 반환</p>
     * <p>{@link CommentQueryController}에서 인기 댓글 조회 API 처리 시 호출합니다.</p>
     *
     * @param postId      게시글 ID
     * @param userDetails 사용자 인증 정보
     * @return List<CommentInfo> 인기 댓글 정보 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public List<CommentInfo> getPopularComments(Long postId, CustomUserDetails userDetails) {
        Long userId = userDetails != null ? userDetails.getUserId() : null;
        return commentQueryPort.findPopularComments(postId, userId);
    }

    /**
     * <h3>과거순 댓글 조회</h3>
     * <p>주어진 게시글의 댓글을 과거순으로 페이지네이션하여 조회합니다.</p>
     * <p>생성 시간이 오래된 댓글부터 최신 댓글까지 시간 순서대로 정렬</p>
     * <p>{@link CommentQueryController}에서 댓글 목록 조회 API 처리 시 호출합니다.</p>
     *
     * @param postId      게시글 ID
     * @param pageable    페이지 정보
     * @param userDetails 사용자 인증 정보
     * @return Page<CommentInfo> 과거순 댓글 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public Page<CommentInfo> getCommentsOldestOrder(Long postId, Pageable pageable, CustomUserDetails userDetails) {
        Long userId = userDetails != null ? userDetails.getUserId() : null;
        return commentQueryPort.findCommentsWithOldestOrder(postId, pageable, userId);
    }

    /**
     * <h3>댓글 ID로 댓글 조회</h3>
     * <p>댓글 ID로 댓글을 조회합니다.</p>
     * <p>존재하지 않는 댓글 ID인 경우 예외를 발생시킵니다.</p>
     * <p>CommentCommandService에서 권한 검증 및 AdminCommandService에서 신고 처리용으로 사용합니다.</p>
     *
     * @param commentId 댓글 ID
     * @return Comment 조회된 댓글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public Comment findById(Long commentId) {
        return commentQueryPort.findById(commentId);
    }

    /**
     * <h3>사용자 작성 댓글 목록 조회</h3>
     * <p>특정 사용자가 작성한 댓글 목록을 페이지네이션으로 조회합니다.</p>
     * <p>최신 작성 댓글부터 과거 순서로 정렬하여 반환</p>
     * <p>{@link UserToCommentAdapter}에서 사용자 활동 조회 시 호출합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return Page<SimpleCommentInfo> 작성한 댓글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public Page<SimpleCommentInfo> getUserComments(Long userId, Pageable pageable) {
        return commentQueryPort.findCommentsByUserId(userId, pageable);
    }

    /**
     * <h3>사용자 추천한 댓글 목록 조회</h3>
     * <p>특정 사용자가 추천한 댓글 목록을 페이지네이션으로 조회합니다.</p>
     * <p>최신 추천 댓글부터 과거 순서로 정렬하여 반환</p>
     * <p>{@link UserToCommentAdapter}에서 사용자 추천 댓글 조회 시 호출합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return Page<SimpleCommentInfo> 추천한 댓글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public Page<SimpleCommentInfo> getUserLikedComments(Long userId, Pageable pageable) {
        return commentQueryPort.findLikedCommentsByUserId(userId, pageable);
    }

    /**
     * <h3>게시글 ID 목록에 대한 댓글 수 조회</h3>
     * <p>여러 게시글의 댓글 수를 배치로 조회합니다.</p>
     * <p>게시글 ID 목록을 한 번에 처리하여 각 게시글별 댓글 수를 반환</p>
     * <p>{@link PostToCommentAdapter}에서 게시글별 댓글 수 배치 조회 시 호출합니다.</p>
     *
     * @param postIds 게시글 ID 목록
     * @return Map<Long, Integer> 게시글 ID를 키로, 댓글 수를 값으로 하는 맵
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public Map<Long, Integer> findCommentCountsByPostIds(List<Long> postIds) {
        return commentQueryPort.findCommentCountsByPostIds(postIds);
    }
}
