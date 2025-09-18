package jaeik.bimillog.domain.comment.application.port.in;

import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.comment.entity.CommentInfo;
import jaeik.bimillog.domain.comment.entity.SimpleCommentInfo;
import jaeik.bimillog.infrastructure.adapter.in.comment.web.CommentQueryController;
import jaeik.bimillog.infrastructure.adapter.out.post.PostToCommentAdapter;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * <h2>댓글 조회 유스케이스</h2>
 * <p>댓글 도메인의 조회 전용 비즈니스 유스케이스입니다.</p>
 * <p>댓글 조회, 인기 댓글 조회, 사용자별 댓글 조회</p>
 * <p>게시글별 댓글 수 조회, 페이지네이션 지원</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface CommentQueryUseCase {

    /**
     * <h3>인기 댓글 조회</h3>
     * <p>주어진 게시글의 인기 댓글 목록을 조회합니다.</p>
     * <p>추천 수가 높은 댓글들을 우선순위로 정렬하여 반환합니다.</p>
     * <p>{@link CommentQueryController}에서 인기 댓글 조회 API 처리 시 호출합니다.</p>
     *
     * @param postId      게시글 ID
     * @param userDetails 사용자 인증 정보
     * @return List<CommentInfo> 인기 댓글 정보 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<CommentInfo> getPopularComments(Long postId, CustomUserDetails userDetails);

    /**
     * <h3>과거순 댓글 조회</h3>
     * <p>주어진 게시글의 댓글을 과거순으로 페이지네이션하여 조회합니다.</p>
     * <p>생성 시간이 오래된 댓글부터 최신 댓글까지 시간 순서대로 정렬합니다.</p>
     * <p>{@link CommentQueryController}에서 댓글 목록 조회 API 처리 시 호출합니다.</p>
     *
     * @param postId      게시글 ID
     * @param pageable    페이지 정보
     * @param userDetails 사용자 인증 정보
     * @return Page<CommentInfo> 과거순 댓글 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<CommentInfo> getCommentsOldestOrder(Long postId, Pageable pageable, CustomUserDetails userDetails);

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
    Comment findById(Long commentId);

    /**
     * <h3>사용자 작성 댓글 목록 조회</h3>
     * <p>특정 사용자가 작성한 댓글 목록을 페이지네이션으로 조회합니다.</p>
     * <p>최신 작성 댓글부터 과거 순서로 정렬하여 반환합니다.</p>
     * <p>UserToCommentAdapter에서 사용자 활동 조회 시 호출합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return Page<SimpleCommentInfo> 작성한 댓글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<SimpleCommentInfo> getUserComments(Long userId, Pageable pageable);

    /**
     * <h3>사용자 추천한 댓글 목록 조회</h3>
     * <p>특정 사용자가 추천한 댓글 목록을 페이지네이션으로 조회합니다.</p>
     * <p>최신 추천 댓글부터 과거 순서로 정렬하여 반환합니다.</p>
     * <p>UserToCommentAdapter에서 사용자 추천 댓글 조회 시 호출합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return Page<SimpleCommentInfo> 추천한 댓글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<SimpleCommentInfo> getUserLikedComments(Long userId, Pageable pageable);

    /**
     * <h3>게시글 ID 목록에 대한 댓글 수 조회</h3>
     * <p>여러 게시글의 댓글 수를 배치로 조회합니다.</p>
     * <p>게시글 ID 목록을 한 번에 처리하여 각 게시글별 댓글 수를 반환합니다.</p>
     * <p>{@link PostToCommentAdapter}에서 게시글별 댓글 수 배치 조회 시 호출합니다.</p>
     *
     * @param postIds 게시글 ID 목록
     * @return Map<Long, Integer> 게시글 ID를 키로, 댓글 수를 값으로 하는 맵
     * @author Jaeik
     * @since 2.0.0
     */
    Map<Long, Integer> findCommentCountsByPostIds(List<Long> postIds);


}
