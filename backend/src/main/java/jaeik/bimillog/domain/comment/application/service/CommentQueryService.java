package jaeik.bimillog.domain.comment.application.service;

import jaeik.bimillog.domain.comment.application.port.in.CommentQueryUseCase;
import jaeik.bimillog.domain.comment.application.port.out.CommentQueryPort;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.comment.entity.CommentInfo;
import jaeik.bimillog.domain.comment.entity.SimpleCommentInfo;
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
 * <h2>CommentQueryService</h2>
 * <p>
 * CQRS 패턴의 Query 영역을 담당하는 댓글 조회 전용 서비스입니다.
 * </p>
 * <p>
 * 계층형 댓글 시스템의 읽기 작업을 처리하며, QueryDSL을 사용하여 타입 안전한 쿼리를 작성합니다.
 * CommentQueryPort를 통해 데이터 레이어와 연결되어 댓글 조회 작업을 수행합니다.
 * </p>
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
     * <p>게시글의 인기 댓글을 추천수 기준으로 정렬하여 조회합니다.</p>
     * <p>QueryDSL을 사용하여 댓글, 사용자, 추천 정보를 JOIN으로 조회합니다.</p>
     * <p>익명 사용자의 경우 추천 정보 없이 댓글만 조회합니다.</p>
     * <p>CommentQueryController에서 메인 페이지 렌더링 시 호출됩니다.</p>
     *
     * @param postId      게시글 ID
     * @param userDetails 사용자 인증 정보 (null 허용)
     * @return List<CommentInfo> 추천수 내림차순 정렬된 인기 댓글 목록
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
     * <h3>과거순 댓글 페이지네이션 조회</h3>
     * <p>댓글을 생성 시간 오름차순(과거순)으로 정렬하여 페이지네이션 처리합니다.</p>
     * <p>QueryDSL의 OFFSET-LIMIT 기반 페이지네이션을 사용하며, 댓글과 사용자 추천 정보를 LEFT JOIN으로 조회합니다.</p>
     * <p>COUNT 쿼리는 별도로 실행하여 전체 데이터 건수를 조회합니다.</p>
     * <p>CommentQueryController에서 댓글 더보기 기능 구현 시 호출됩니다.</p>
     *
     * @param postId      게시글 ID
     * @param pageable    페이지 정보 (size, sort, offset)
     * @param userDetails 사용자 인증 정보 (null 허용)
     * @return Page<CommentInfo> 생성시간 오름차순 정렬된 댓글 페이지
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
     * <h3>댓글 단건 조회</h3>
     * <p>Primary Key 기반으로 댓글 엔티티를 단건 조회합니다.</p>
     * <p>JPA의 EntityManager 1차 캐시를 활용하여 같은 트랜잭션 내에서 재조회합니다.</p>
     * <p>댓글 수정/삭제 권한 검증 시 사용되며, 존재하지 않는 댓글에 대해서는 도메인 예외를 발생시킵니다.</p>
     * <p>CommentCommandService에서 댓글 수정/삭제 시 권한 검증을 위해 호출됩니다.</p>
     *
     * @param commentId 댓글 ID (Primary Key)
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
     * <p>특정 사용자가 작성한 모든 댓글을 최신순으로 페이지네이션 처리하여 조회합니다.</p>
     * <p>QueryDSL을 사용하여 댓글과 게시글 정보를 JOIN으로 조회합니다.</p>
     * <p>사용자별 댓글 이력 관리를 위해 createdAt 인덱스를 활용하여 정렬합니다.</p>
     * <p>UserQueryController에서 마이페이지 댓글 목록 표시 시 호출됩니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보 (최신순 정렬 권장)
     * @return Page<SimpleCommentInfo> 사용자가 작성한 댓글 목록 페이지
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
     * <p>CommentQueryPort를 통해 댓글 추천 테이블과 댓글 테이블을 JOIN하여 조회합니다.</p>
     * <p>UserQueryController에서 마이페이지 추천 댓글 목록 표시 시 호출됩니다.</p>
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
     * <p>QueryDSL의 GROUP BY를 사용하여 게시글별 댓글 수를 한 번의 쿼리로 조회합니다.</p>
     * <p>PostQueryController에서 게시글 목록 표시 시 댓글 수 표시를 위해 호출됩니다.</p>
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
