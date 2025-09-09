package jaeik.bimillog.domain.post.application.port.out;

import java.util.List;
import java.util.Map;

/**
 * <h2>PostCommentToPort</h2>
 * <p>
 * Post 도메인에서 Comment 도메인의 데이터를 조회하기 위한 크로스 도메인 아웃바운드 포트입니다.
 * 헥사고날 아키텍처에서 도메인 간 데이터 의존성을 추상화하여 도메인 경계를 명확히 분리합니다.
 * </p>
 * <p>사용자가 게시판을 조회하는 상황에서 PostQueryController → PostQueryService → PostCommentToPort 순으로 호출되어 
 * 게시글과 함께 댓글 수를 표시함으로써 사용자 참여도를 시각적으로 제공합니다.</p>
 * <p>Comment 도메인의 세부 구현(계층형 댓글, 삭제 처리 등)을 숨기고 Post 도메인이 필요로 하는 집계 정보만 제공합니다.</p>
 * <p>배치 조회를 통해 N+1 쿼리 문제를 방지하고 게시글 목록 조회의 성능을 최적화합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PostCommentToPort {

    /**
     * <h3>게시글 ID 목록별 댓글 수 배치 조회</h3>
     * <p>여러 게시글에 속한 댓글 수를 한 번의 쿼리로 배치 조회합니다.</p>
     * <p>사용자가 게시판 페이지(/api/post/board)를 방문하는 상황에서 게시글 목록과 함께 각 글의 댓글 수를 표시하기 위해 호출됩니다.</p>
     * <p>댓글이 많은 글을 시각적으로 구별할 수 있게 하여 사용자의 참여도 높은 게시글 식별을 돕습니다.</p>
     * <p>계층형 댓글 구조에서 삭제되지 않은 모든 댓글을 카운팅하여 정확한 댓글 수를 반환합니다.</p>
     * <p>단일 쿼리로 모든 게시글의 댓글 수를 조회하여 N+1 쿼리 문제를 방지하고 성능을 최적화합니다.</p>
     *
     * @param postIds 댓글 수를 조회할 게시글 ID 목록
     * @return 게시글 ID를 키로, 해당 게시글의 댓글 수를 값으로 하는 맵
     * @author Jaeik
     * @since 2.0.0
     */
    Map<Long, Integer> findCommentCountsByPostIds(List<Long> postIds);
}