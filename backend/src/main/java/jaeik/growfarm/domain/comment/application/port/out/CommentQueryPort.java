package jaeik.growfarm.domain.comment.application.port.out;

import jaeik.growfarm.domain.comment.entity.Comment;
import jaeik.growfarm.dto.comment.CommentDTO;
import jaeik.growfarm.dto.comment.SimpleCommentDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <h2>댓글 조회 포트</h2>
 * <p>댓글 엔티티 조회를 위한 Out-Port</p>
 * <p>CQRS 패턴에 따른 조회 전용 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface CommentQueryPort {

    /**
     * <h3>인기 댓글 목록 조회</h3>
     * <p>주어진 게시글 ID에 대한 인기 댓글 목록을 조회합니다.</p>
     *
     * @param postId 게시글 ID
     * @param likedCommentIds 사용자가 추천한 댓글 ID 목록
     * @return List<jaeik.growfarm.dto.comment.CommentDTO> 인기 댓글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<CommentDTO> findPopularComments(Long postId, List<Long> likedCommentIds);

    /**
     * <h3>최신순 댓글 목록 조회</h3>
     * <p>주어진 게시글 ID에 대한 댓글을 최신순으로 페이지네이션하여 조회합니다.</p>
     *
     * @param postId 게시글 ID
     * @param pageable 페이지 정보
     * @param likedCommentIds 사용자가 추천한 댓글 ID 목록
     * @return Page<jaeik.growfarm.dto.comment.CommentDTO> 최신순 댓글 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<CommentDTO> findCommentsWithLatestOrder(Long postId, Pageable pageable, List<Long> likedCommentIds);

    /**
     * <h3>ID로 댓글 조회</h3>
     * <p>주어진 ID로 댓글을 조회합니다.</p>
     *
     * @param id 댓글 ID
     * @return Optional<Comment> 조회된 댓글 엔티티. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<Comment> findById(Long id);

    /**
     * <h3>게시글 ID로 루트 댓글 수 조회</h3>
     * <p>주어진 게시글 ID에 해당하는 최상위(루트) 댓글의 수를 조회합니다.</p>
     * 
     * <p><strong>⚠️ TODO: 현재 미사용 메서드 - Post 도메인 연결 필요</strong></p>
     * <ul>
     *   <li><strong>목적</strong>: 게시글 목록에서 각 게시글의 댓글 수를 표시하기 위해 필요</li>
     *   <li><strong>현재 상황</strong>: SimplePostResDTO.commentCount 필드가 있지만 실제로 채워지지 않음</li>
     *   <li><strong>연결 지점</strong>: PostQueryService에서 게시글 목록 조회 시 이 메서드 호출 필요</li>
     *   <li><strong>사용 예시</strong>: 커뮤니티 게시글 목록에서 "댓글 15개" 같은 정보 표시</li>
     *   <li><strong>구현 위치</strong>: PostQueryService.findPostList() 메서드에서 배치 조회 로직 추가</li>
     * </ul>
     *
     * @param postId 게시글 ID
     * @return Long 루트 댓글의 수
     * @author Jaeik
     * @since 2.0.0
     */
    Long countRootCommentsByPostId(Long postId);

    /**
     * <h3>여러 게시글 ID에 대한 댓글 수 조회</h3>
     * <p>주어진 여러 게시글 ID에 해당하는 각 게시글의 댓글 수를 조회합니다.</p>
     * 
     * <p><strong>⚠️ TODO: 현재 미사용 메서드 - 성능 최적화를 위한 배치 조회</strong></p>
     * <ul>
     *   <li><strong>목적</strong>: 게시글 목록 조회 시 N+1 문제를 피하기 위한 배치 댓글 수 조회</li>
     *   <li><strong>성능 이점</strong>: 20개 게시글의 댓글 수를 개별 조회하지 않고 한 번에 조회</li>
     *   <li><strong>현재 상황</strong>: SimplePostResDTO.commentCount 필드가 0으로 고정됨</li>
     *   <li><strong>연결 지점</strong>: PostQueryService에서 게시글 목록 조회 후 commentCount 설정 로직 추가</li>
     *   <li><strong>구현 방법</strong>:</li>
     *   <li>&nbsp;&nbsp;1. PostQueryService에서 게시글 목록 조회</li>
     *   <li>&nbsp;&nbsp;2. 게시글 ID 목록 추출</li>
     *   <li>&nbsp;&nbsp;3. 이 메서드로 댓글 수 배치 조회</li>
     *   <li>&nbsp;&nbsp;4. SimplePostResDTO.commentCount에 값 설정</li>
     *   <li><strong>대안</strong>: countRootCommentsByPostId를 반복 호출하면 성능 저하</li>
     * </ul>
     *
     * @param postIds 게시글 ID 목록
     * @return Map<Long, Integer> 게시글 ID를 키로, 댓글 수를 값으로 하는 맵
     * @author Jaeik
     * @since 2.0.0
     */
    Map<Long, Integer> findCommentCountsByPostIds(List<Long> postIds);

    /**
     * <h3>사용자가 추천한 댓글 ID 목록 조회</h3>
     * <p>주어진 댓글 ID 목록 중 사용자가 추천을 누른 댓글의 ID 목록을 조회합니다.</p>
     *
     * @param commentIds 댓글 ID 목록
     * @param userId     사용자 ID
     * @return List<Long> 사용자가 추천을 누른 댓글 ID 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<Long> findUserLikedCommentIds(List<Long> commentIds, Long userId);

    /**
     * <h3>사용자 작성 댓글 목록 조회</h3>
     * <p>특정 사용자가 작성한 댓글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return Page<SimpleCommentDTO> 작성한 댓글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<SimpleCommentDTO> findCommentsByUserId(Long userId, Pageable pageable);

    /**
     * <h3>사용자 추천한 댓글 목록 조회</h3>
     * <p>특정 사용자가 추천한 댓글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return Page<SimpleCommentDTO> 추천한 댓글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<SimpleCommentDTO> findLikedCommentsByUserId(Long userId, Pageable pageable);


    /**
     * <h3>게시글 ID로 사용자가 추천한 댓글 ID 목록 조회</h3>
     * <p>주어진 게시글 ID에 대한 댓글 중 사용자가 추천을 누른 댓글의 ID 목록을 조회합니다.</p>
     *
     * @param postId 게시글 ID
     * @param userId 사용자 ID
     * @return List<Long> 사용자가 추천을 누른 댓글 ID 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<Long> findUserLikedCommentIdsByPostId(Long postId, Long userId);
}