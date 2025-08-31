package jaeik.bimillog.domain.post.application.port.out;

import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.user.entity.User;

import java.util.List;
import java.util.Map;

/**
 * <h2>글 추천 조회 포트</h2>
 * <p>
 *     게시글 추천 조회를 담당하는 통합 Port 인터페이스입니다.
 * </p>
 *
 * @author jaeik
 * @version 2.0.0
 */
public interface PostLikeQueryPort {

    /**
     * <h3>게시글 추천 존재 여부 확인</h3>
     * <p>
     *     사용자와 게시글을 기준으로 추천 데이터가 존재하는지 확인합니다.
     * </p>
     * @param user 사용자
     * @param post 게시글
     * @return 존재하면 true, 아니면 false
     */
    boolean existsByUserAndPost(User user, Post post);

    /**
     * <h3>게시글의 추천 개수 조회</h3>
     * <p>
     *     특정 게시글의 추천 개수를 조회합니다.
     * </p>
     * @param post 추천 개수를 조회할 게시글
     * @return 추천 개수
     */
    long countByPost(Post post);
    
    /**
     * <h3>게시글 ID 목록에 대한 추천 수 배치 조회</h3>
     * <p>
     *     여러 게시글의 추천 수를 한 번의 쿼리로 조회하여 N+1 문제를 해결합니다.
     * </p>
     * @param postIds 게시글 ID 목록
     * @return Map<Long, Integer> 게시글 ID를 키로, 추천 수를 값으로 하는 맵
     * @since 2.0.0
     * @author jaeik
     */
    Map<Long, Integer> findLikeCountsByPostIds(List<Long> postIds);

    /**
     * <h3>게시글 ID와 사용자 ID로 추천 존재 여부 확인</h3>
     * <p>
     *     Post 엔티티를 로드하지 않고 ID만으로 추천 여부를 확인합니다.
     *     캐시된 게시글의 추천 여부 확인 시 성능 향상을 위해 사용됩니다.
     * </p>
     * @param postId 게시글 ID
     * @param userId 사용자 ID
     * @return 추천이 존재하면 true, 아니면 false
     * @since 2.0.0
     * @author jaeik
     */
    boolean existsByPostIdAndUserId(Long postId, Long userId);
}