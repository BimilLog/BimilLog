package jaeik.bimillog.domain.post.application.port.out;

import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.user.entity.User;

import java.util.List;
import java.util.Map;

/**
 * <h2>PostLikeQueryPort</h2>
 * <p>
 * Post 도메인에서 게시글 추천(좋아요) 데이터의 조회 작업을 처리하는 아웃바운드 포트입니다.
 * 헥사고날 아키텍처에서 Post 도메인과 PostLike 엔티티의 조회 계층을 분리하는 추상화된 인터페이스 역할을 합니다.
 * </p>
 * <p>PostQueryService와 PostInteractionService에서 추천 관련 정보 조회 시 호출됩니다.</p>
 * <p>개별 추천 확인, 추천 수 집계, 배치 조회를 통한 성능 최적화 기능을 제공합니다.</p>
 * <p>CQRS 패턴에 따라 읽기 전용 작업만을 담당하며, 복잡한 집계 쿼리의 성능 최적화에 중점을 둡니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PostLikeQueryPort {

    /**
     * <h3>사용자별 게시글 추천 여부 확인</h3>
     * <p>특정 사용자가 특정 게시글에 추천을 눌렀는지 확인합니다.</p>
     * <p>PostInteractionService의 toggleLike 메서드에서 추천 상태 확인 시 호출됩니다.</p>
     * <p>User와 Post 엔티티의 복합 조건으로 PostLike 테이블에서 레코드 존재 여부를 확인합니다.</p>
     * <p>추천 토글 로직에서 현재 상태를 판단하기 위한 중요한 체크 포인트 역할을 합니다.</p>
     *
     * @param user 추천 여부를 확인할 사용자
     * @param post 추천 여부를 확인할 게시글
     * @return 추천이 존재하면 true, 존재하지 않으면 false
     * @author Jaeik
     * @since 2.0.0
     */
    boolean existsByUserAndPost(User user, Post post);

    /**
     * <h3>게시글별 추천 수 조회</h3>
     * <p>특정 게시글에 대한 총 추천 수를 조회합니다.</p>
     * <p>PostQueryService의 게시글 상세 조회 시 추천 수 정보 제공을 위해 호출됩니다.</p>
     * <p>해당 게시글에 연결된 모든 PostLike 레코드를 카운팅하여 정확한 추천 수를 반환합니다.</p>
     * <p>캐시 미스 상황에서 실시간 추천 수 집계를 위한 백업 조회 메서드로 활용됩니다.</p>
     *
     * @param post 추천 수를 조회할 게시글
     * @return 해당 게시글의 총 추천 수
     * @author Jaeik
     * @since 2.0.0
     */
    long countByPost(Post post);
    
    /**
     * <h3>게시글 ID 목록별 추천 수 배치 조회</h3>
     * <p>여러 게시글의 추천 수를 한 번의 쿼리로 배치 조회합니다.</p>
     * <p>PostQueryService의 게시글 목록 조회 메서드들에서 각 게시글의 추천 수 정보를 함께 제공하기 위해 호출됩니다.</p>
     * <p>GROUP BY와 COUNT를 활용한 집계 쿼리로 각 게시글별 추천 수를 효율적으로 계산합니다.</p>
     * <p>단일 쿼리로 모든 게시글의 추천 수를 조회하여 N+1 쿼리 문제를 방지하고 성능을 최적화합니다.</p>
     *
     * @param postIds 추천 수를 조회할 게시글 ID 목록
     * @return 게시글 ID를 키로, 해당 게시글의 추천 수를 값으로 하는 맵
     * @author Jaeik
     * @since 2.0.0
     */
    Map<Long, Integer> findLikeCountsByPostIds(List<Long> postIds);

    /**
     * <h3>ID 기반 추천 존재 여부 확인</h3>
     * <p>게시글 ID와 사용자 ID만으로 추천 여부를 확인합니다.</p>
     * <p>PostQueryService에서 캐시된 게시글 상세 조회 시 추천 여부 정보를 함께 제공하기 위해 호출됩니다.</p>
     * <p>Post와 User 엔티티를 로드하지 않고 ID만으로 조회하여 성능을 최적화합니다.</p>
     * <p>캐시된 데이터와 함께 사용자별 추천 상태를 효율적으로 확인하는 경량화된 조회 메서드입니다.</p>
     *
     * @param postId 추천 여부를 확인할 게시글 ID
     * @param userId 추천 여부를 확인할 사용자 ID
     * @return 추천이 존재하면 true, 존재하지 않으면 false
     * @author Jaeik
     * @since 2.0.0
     */
    boolean existsByPostIdAndUserId(Long postId, Long userId);
}