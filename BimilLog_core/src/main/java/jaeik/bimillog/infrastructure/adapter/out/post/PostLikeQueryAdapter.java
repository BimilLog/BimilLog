package jaeik.bimillog.infrastructure.adapter.out.post;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.post.application.port.out.PostLikeQueryPort;
import jaeik.bimillog.domain.post.entity.QPostLike;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <h2>게시글 추천 조회 어댑터</h2>
 * <p>게시글 추천 조회 포트의 JPA/QueryDSL 구현체입니다.</p>
 * <p>배치 조회, ID 기반 조회</p>
 * <p>JPA Repository와 QueryDSL을 활용</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class PostLikeQueryAdapter implements PostLikeQueryPort {
    private final JPAQueryFactory jpaQueryFactory;
    
    private static final QPostLike postLike = QPostLike.postLike;

    /**
     * <h3>게시글 ID 목록에 대한 추천 수 배치 조회</h3>
     * <p>여러 게시글의 추천 수를 한 번의 쿼리로 배치 조회합니다.</p>
     * <p>GROUP BY와 COUNT를 활용한 집계 쿼리</p>
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
     * <h3>ID 기반 추천 존재 여부 확인</h3>
     * <p>Post와 Member 엔티티를 로드하지 않고 ID만으로 추천 여부를 확인합니다.</p>
     * <p>캐시된 게시글의 추천 여부 확인 시 사용</p>
     *
     * @param postId 게시글 ID
     * @param memberId 사용자 ID
     * @return 추천이 존재하면 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public boolean existsByPostIdAndUserId(Long postId, Long memberId) {
        Integer count = jpaQueryFactory
                .selectOne()
                .from(postLike)
                .where(postLike.post.id.eq(postId)
                        .and(postLike.member.id.eq(memberId)))
                .fetchFirst();
        
        return count != null;
    }
}
