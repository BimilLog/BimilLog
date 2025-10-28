package jaeik.bimillog.domain.comment.application.port.out;

import jaeik.bimillog.domain.comment.application.service.CommentCommandService;
import jaeik.bimillog.domain.comment.application.service.CommentQueryService;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.comment.entity.CommentInfo;
import jaeik.bimillog.domain.comment.entity.SimpleCommentInfo;
import jaeik.bimillog.domain.post.application.service.PostQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * <h2>댓글 조회 포트</h2>
 * <p>댓글 도메인의 조회 작업을 담당하는 포트입니다.</p>
 * <p>댓글 개별 조회, 인기 댓글 조회, 페이지네이션 조회</p>
 * <p>사용자별 댓글 이력 조회, 게시글별 댓글 수 집계</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface CommentQueryPort {


    /**
     * <h3>게시글의 인기 댓글 목록 조회</h3>
     * <p>특정 게시글에 대한 인기 댓글을 추천 수 기준으로 정렬하여 조회합니다.</p>
     * <p>사용자 ID가 제공되면 각 댓글에 대한 사용자의 추천 상태도 함께 조회합니다.</p>
     * <p>{@link CommentQueryService}에서 인기 댓글 목록 조회 시 호출됩니다.</p>
     *
     * @param postId 게시글 ID
     * @param memberId 사용자 ID (추천 여부 확인용, null 가능)
     * @return List<CommentInfo> 추천 수 기준으로 정렬된 인기 댓글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<CommentInfo> findPopularComments(Long postId, Long memberId);

    /**
     * <h3>다중 게시글의 댓글 수 일괄 조회</h3>
     * <p>여러 게시글에 대한 댓글 수를 한 번의 쿼리로 조회합니다.</p>
     * <p>{@link PostQueryService}에서 게시글 목록 조회 시 댓글 수 정보를 포함하기 위해 호출됩니다.</p>
     *
     * @param postIds 게시글 ID 목록
     * @return Map<Long, Integer> 게시글 ID를 키로, 댓글 수를 값으로 하는 맵
     * @author Jaeik
     * @since 2.0.0
     */
    Map<Long, Integer> findCommentCountsByPostIds(List<Long> postIds);

    /**
     * <h3>게시글 댓글 시간순 페이지네이션 조회</h3>
     * <p>특정 게시글에 대한 댓글을 작성 시간 오름차순으로 정렬하여 페이지 단위로 조회합니다.</p>
     * <p>사용자 ID가 제공되면 각 댓글에 대한 사용자의 추천 상태도 함께 조회합니다.</p>
     * <p>{@link CommentQueryService}에서 댓글 목록 페이지네이션 조회 시 호출됩니다.</p>
     *
     * @param postId   게시글 ID
     * @param pageable 페이지 정보
     * @param memberId   사용자 ID (추천 여부 확인용, null 가능)
     * @return Page<CommentInfo> 작성 시간 오름차순으로 정렬된 댓글 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<CommentInfo> findCommentsWithOldestOrder(Long postId, Pageable pageable, Long memberId);

    /**
     * <h3>사용자 작성 댓글 이력 조회</h3>
     * <p>특정 사용자가 작성한 모든 댓글을 최신순으로 정렬하여 페이지네이션으로 조회합니다.</p>
     * <p>{@link MemberActivityAdapter}에서 사용자 활동 내역 조회 시 호출됩니다.</p>
     *
     * @param memberId   사용자 ID
     * @param pageable 페이지 정보
     * @return Page<SimpleCommentInfo> 작성한 댓글 목록 페이지 (최신순)
     * @author Jaeik
     * @since 2.0.0
     */
    Page<SimpleCommentInfo> findCommentsByMemberId(Long memberId, Pageable pageable);

    /**
     * <h3>사용자 추천 댓글 이력 조회</h3>
     * <p>특정 사용자가 추천한 모든 댓글을 최신 추천순으로 정렬하여 페이지네이션으로 조회합니다.</p>
     * <p>{@link MemberActivityAdapter}에서 사용자 추천 활동 내역 조회 시 호출됩니다.</p>
     *
     * @param memberId   사용자 ID
     * @param pageable 페이지 정보
     * @return Page<SimpleCommentInfo> 추천한 댓글 목록 페이지 (최신 추천순)
     * @author Jaeik
     * @since 2.0.0
     */
    Page<SimpleCommentInfo> findLikedCommentsByMemberId(Long memberId, Pageable pageable);

    /**
     * <h3>특정 사용자의 모든 댓글 조회</h3>
     * <p>사용자 탈퇴 시 댓글 처리를 위해 특정 사용자의 모든 댓글 엔티티를 조회합니다.</p>
     * <p>{@link CommentCommandService}에서 사용자 탈퇴 처리 시 호출됩니다.</p>
     *
     * @param memberId 조회할 사용자 ID
     * @return List<Comment> 사용자가 작성한 모든 댓글 엔티티 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<Comment> findAllByMemberId(Long memberId);

    /**
     * <h3>특정 글의 모든 댓글 조회</h3>
     * <p>게시글에 달린 모든 댓글을 계층 구조와 무관하게 플랫하게 조회합니다.</p>
     * <p>삭제된 댓글(isDeleted=true)도 포함하여 모든 댓글을 반환합니다.</p>
     * <p>{@link CommentCommandService#deleteCommentsByPost}에서 게시글 삭제 시 댓글 일괄 삭제용으로 호출됩니다.</p>
     *
     * @param postId 댓글을 조회할 게시글 ID
     * @return List<Comment> 해당 게시글의 모든 댓글 리스트
     * @author Jaeik
     * @since 2.0.0
     */
    List<Comment> findAllByPostId(Long postId);

    /**
     * <h3>자손 댓글 존재 여부 확인</h3>
     * <p>특정 댓글이 자손 댓글을 가지고 있는지 확인합니다.</p>
     * <p>{@link CommentCommandService}에서 댓글 삭제 시 하드/소프트 삭제 결정을 위해 호출됩니다.</p>
     *
     * @param commentId 확인할 댓글 ID
     * @return boolean 자손 댓글이 있으면 true, 없으면 false
     * @author Jaeik
     * @since 2.0.0
     */
    boolean hasDescendants(Long commentId);

}