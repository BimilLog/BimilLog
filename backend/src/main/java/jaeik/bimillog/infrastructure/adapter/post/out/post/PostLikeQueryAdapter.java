package jaeik.bimillog.infrastructure.adapter.post.out.post;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.post.application.port.out.PostLikeQueryPort;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.QPostLike;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.adapter.post.out.jpa.PostLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <h2>게시글 추천 쿼리 영속성 어댑터</h2>
 * <p>게시글 추천의 조회와 관련된 데이터베이스 작업을 처리합니다.</p>
 * <p>PostLikeQueryPort 인터페이스를 구현하여 게시글 추천 조회 기능을 제공합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class PostLikeQueryAdapter implements PostLikeQueryPort {
    private final PostLikeRepository postLikeRepository;
    private final JPAQueryFactory jpaQueryFactory;
    
    private static final QPostLike postLike = QPostLike.postLike;

    /**
     * <h3>사용자와 게시글로 추천 존재 여부 확인</h3>
     * <p>특정 사용자 및 게시글에 추천가 존재하는지 확인합니다.</p>
     *
     * @param user 사용자 엔티티
     * @param post 게시글 엔티티
     * @return 추천가 존재하면 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public boolean existsByUserAndPost(User user, Post post) {
        return postLikeRepository.existsByUserAndPost(user, post);
    }

    /**
     * <h3>게시글 추천 수 조회</h3>
     * <p>특정 게시글의 추천 총 개수를 조회합니다.</p>
     *
     * @param post 게시글 엔티티
     * @return 해당 게시글의 추천 개수
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public long countByPost(Post post) {
        return postLikeRepository.countByPost(post);
    }
    
    /**
     * <h3>게시글 ID 목록에 대한 추천 수 배치 조회</h3>
     * <p>여러 게시글의 추천 수를 한 번의 쿼리로 조회하여 N+1 문제를 해결합니다.</p>
     *
     * @param postIds 게시글 ID 목록
     * @return Map<Long, Integer> 게시글 ID를 키로, 추천 수를 값으로 하는 맵
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Map<Long, Integer> findLikeCountsByPostIds(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Map.of();
        }
        
        List<Tuple> results = jpaQueryFactory
                .select(postLike.post.id, postLike.count())
                .from(postLike)
                .where(postLike.post.id.in(postIds))
                .groupBy(postLike.post.id)
                .fetch();
        
        // 결과를 Map으로 변환
        Map<Long, Integer> likeCounts = results.stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(postLike.post.id),
                        tuple -> tuple.get(postLike.count()).intValue()
                ));
        
        // 조회 결과가 없는 게시글은 0으로 설정
        for (Long postId : postIds) {
            likeCounts.putIfAbsent(postId, 0);
        }
        
        return likeCounts;
    }

    /**
     * <h3>게시글 ID와 사용자 ID로 추천 존재 여부 확인</h3>
     * <p>Post와 User 엔티티를 로드하지 않고 ID만으로 추천 여부를 확인합니다.</p>
     * <p>캐시된 게시글의 추천 여부 확인 시 성능 향상을 위해 사용됩니다.</p>
     *
     * @param postId 게시글 ID
     * @param userId 사용자 ID
     * @return 추천이 존재하면 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public boolean existsByPostIdAndUserId(Long postId, Long userId) {
        Integer count = jpaQueryFactory
                .selectOne()
                .from(postLike)
                .where(postLike.post.id.eq(postId)
                        .and(postLike.user.id.eq(userId)))
                .fetchFirst();
        
        return count != null;
    }
}
