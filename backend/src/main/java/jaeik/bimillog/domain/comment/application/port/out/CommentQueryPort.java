package jaeik.bimillog.domain.comment.application.port.out;

import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.comment.entity.CommentInfo;
import jaeik.bimillog.domain.comment.entity.SimpleCommentInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * <h2>CommentQueryPort</h2>
 * <p>
 * 댓글 조회 기능을 담당하는 아웃바운드 포트입니다.
 * 헥사고날 아키텍처에서 댓글 읽기 관련 외부 의존성을 추상화하여 도메인 로직의 순수성을 보장합니다.
 * CQRS 패턴에 따른 조회 전용 포트로 읽기 작업에 특화되어 있습니다.
 * </p>
 * <p>
 * 이 포트는 다양한 댓글 조회 기능을 제공합니다:
 * - 개별 댓글 조회: ID를 통한 특정 댓글 검색
 * - 인기 댓글 조회: 추천 수 기반 댓글 정렬
 * - 페이지네이션 조회: 대용량 댓글 데이터의 효율적 조회
 * - 사용자별 댓글 이력: 작성한 댓글과 추천한 댓글 조회
 * - 통계 데이터: 게시글별 댓글 수 집계
 * </p>
 * <p>
 * 비즈니스 컨텍스트에서 이 포트가 필요한 이유:
 * 1. 사용자 경험 개선 - 댓글을 다양한 정렬 기준으로 제공
 * 2. 성능 최적화 - 페이지네이션을 통한 대용량 데이터 처리
 * 3. 개인화 서비스 - 사용자별 댓글 활동 내역 제공
 * 4. 커뮤니티 활성도 측정 - 게시글별 참여도 통계
 * </p>
 * <p>
 * CommentQueryController에서 댓글 목록 조회 API 제공 시 사용됩니다.
 * PostService에서 게시글 상세 정보에 댓글 정보를 포함할 때 사용됩니다.
 * UserService에서 사용자 활동 이력 조회 시 사용됩니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface CommentQueryPort {

    /**
     * <h3>댓글 ID로 단일 댓글 조회</h3>
     * <p>특정 ID에 해당하는 댓글 엔티티를 조회합니다.</p>
     * <p>댓글 수정, 삭제, 상세 조회 등의 기본 CRUD 작업에서 사용됩니다.</p>
     * <p>CommentService에서 댓글 수정/삭제 권한 확인 시 호출됩니다.</p>
     * <p>CommentQueryController에서 개별 댓글 상세 정보 조회 시 호출됩니다.</p>
     *
     * @param id 댓글 ID
     * @return Comment 조회된 댓글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    Comment findById(Long id);

    /**
     * <h3>게시글의 인기 댓글 목록 조회</h3>
     * <p>특정 게시글에 대한 인기 댓글을 추천 수 기준으로 정렬하여 조회합니다.</p>
     * <p>사용자 ID가 제공되면 각 댓글에 대한 사용자의 추천 상태도 함께 조회합니다.</p>
     * <p>게시글 상단에 표시할 베스트 댓글 목록을 제공하기 위해 사용됩니다.</p>
     * <p>CommentQueryController에서 인기 댓글 목록 API 제공 시 호출됩니다.</p>
     *
     * @param postId 게시글 ID
     * @param userId 사용자 ID (추천 여부 확인용, null 가능)
     * @return List<CommentInfo> 추천 수 기준으로 정렬된 인기 댓글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<CommentInfo> findPopularComments(Long postId, Long userId);

    /**
     * <h3>다중 게시글의 댓글 수 일괄 조회</h3>
     * <p>여러 게시글에 대한 댓글 수를 한 번의 쿼리로 효율적으로 조회합니다.</p>
     * <p>게시글 목록 페이지에서 각 게시글의 댓글 수를 표시할 때 N+1 문제를 방지하기 위해 사용됩니다.</p>
     * <p>PostQueryController에서 게시글 목록 조회 시 댓글 수 정보를 포함하기 위해 호출됩니다.</p>
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
     * <p>댓글이 많은 게시글에서 부드러운 스크롤링과 성능을 보장하기 위해 페이지네이션을 제공합니다.</p>
     * <p>CommentQueryController에서 댓글 목록 페이지네이션 API 제공 시 호출됩니다.</p>
     *
     * @param postId   게시글 ID
     * @param pageable 페이지 정보
     * @param userId   사용자 ID (추천 여부 확인용, null 가능)
     * @return Page<CommentInfo> 작성 시간 오름차순으로 정렬된 댓글 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<CommentInfo> findCommentsWithOldestOrder(Long postId, Pageable pageable, Long userId);

    /**
     * <h3>사용자 작성 댓글 이력 조회</h3>
     * <p>특정 사용자가 작성한 모든 댓글을 최신순으로 정렬하여 페이지네이션으로 조회합니다.</p>
     * <p>사용자 프로필 페이지나 마이페이지에서 개인의 댓글 활동 내역을 제공하기 위해 사용됩니다.</p>
     * <p>UserQueryController에서 사용자 활동 내역 조회 API 제공 시 호출됩니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return Page<SimpleCommentInfo> 작성한 댓글 목록 페이지 (최신순)
     * @author Jaeik
     * @since 2.0.0
     */
    Page<SimpleCommentInfo> findCommentsByUserId(Long userId, Pageable pageable);

    /**
     * <h3>사용자 추천 댓글 이력 조회</h3>
     * <p>특정 사용자가 추천한 모든 댓글을 최신 추천순으로 정렬하여 페이지네이션으로 조회합니다.</p>
     * <p>사용자가 관심을 보인 댓글들을 확인할 수 있는 개인화된 서비스를 제공하기 위해 사용됩니다.</p>
     * <p>UserQueryController에서 사용자 추천 활동 내역 조회 API 제공 시 호출됩니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return Page<SimpleCommentInfo> 추천한 댓글 목록 페이지 (최신 추천순)
     * @author Jaeik
     * @since 2.0.0
     */
    Page<SimpleCommentInfo> findLikedCommentsByUserId(Long userId, Pageable pageable);

}