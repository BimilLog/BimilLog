package jaeik.bimillog.domain.post.application.port.out;

import jaeik.bimillog.domain.post.service.PostQueryService;

import java.util.List;
import java.util.Map;

/**
 * <h2>게시글 추천 조회 포트</h2>
 * <p>게시글 도메인의 추천(좋아요) 조회 작업을 담당하는 포트입니다.</p>
 * <p>추천 여부 확인, 추천 수 배치 조회</p>
 * <p>ID 기반 추천 상태 조회</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PostLikeQueryPort {

    /**
     * <h3>게시글 ID 목록별 추천 수 배치 조회</h3>
     * <p>여러 게시글의 추천 수를 한 번의 쿼리로 배치 조회합니다.</p>
     * <p>GROUP BY와 COUNT를 활용한 집계 쿼리로 각 게시글별 추천 수를 처리</p>
     * <p>{@link PostQueryService}에서 게시글 목록 조회 시 추천 수 배치 조회를 위해 호출됩니다.</p>
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
     * <p>ID만으로 추천 여부를 조회</p>
     * <p>{@link PostQueryService}에서 캐시된 게시글의 사용자별 추천 상태 확인 시 호출됩니다.</p>
     *
     * @param postId 추천 여부를 확인할 게시글 ID
     * @param memberId 추천 여부를 확인할 사용자 ID
     * @return 추천이 존재하면 true, 존재하지 않으면 false
     * @author Jaeik
     * @since 2.0.0
     */
    boolean existsByPostIdAndUserId(Long postId, Long memberId);
}