package jaeik.bimillog.infrastructure.adapter.post.out.persistence.post.cache;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.post.application.port.out.PostCacheCommandPort;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSearchResult;
import jaeik.bimillog.domain.post.entity.QPost;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * <h2>Redis 캐시 명령 어댑터</h2>
 * <p>Redis를 사용하여 게시글 캐시 데이터를 관리하는 영속성 어댑터입니다.</p>
 * <p>PostCacheCommandPort 인터페이스를 구현합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class PostCacheCommandAdapter implements PostCacheCommandPort {

    private final RedisTemplate<String, Object> redisTemplate;
    private final Map<PostCacheFlag, CacheMetadata> cacheMetadataMap = initializeCacheMetadata();
    private static final String FULL_POST_CACHE_PREFIX = "cache:post:";
    private static final Duration FULL_POST_CACHE_TTL = Duration.ofDays(1);

    private final JPAQueryFactory jpaQueryFactory;

    private static Map<PostCacheFlag, CacheMetadata> initializeCacheMetadata() {
        Map<PostCacheFlag, CacheMetadata> map = new EnumMap<>(PostCacheFlag.class);
        map.put(PostCacheFlag.REALTIME, new CacheMetadata("cache:posts:realtime", Duration.ofMinutes(30)));
        map.put(PostCacheFlag.WEEKLY, new CacheMetadata("cache:posts:weekly", Duration.ofDays(1)));
        map.put(PostCacheFlag.LEGEND, new CacheMetadata("cache:posts:legend", Duration.ofDays(1)));
        map.put(PostCacheFlag.NOTICE, new CacheMetadata("cache:posts:notice", Duration.ofDays(7)));
        return map;
    }

    private record CacheMetadata(String key, Duration ttl) {}

    /**
     * <h3>캐시 메타데이터 조회</h3>
     * <p>주어진 캐시 유형에 해당하는 메타데이터를 조회합니다.</p>
     *
     * @param type 게시글 캐시 유형
     * @return 캐시 메타데이터
     * @throws CustomException 알 수 없는 PostCacheFlag 유형인 경우
     * @author Jaeik
     * @since 2.0.0
     */
    private CacheMetadata getCacheMetadata(PostCacheFlag type) {
        CacheMetadata metadata = cacheMetadataMap.get(type);
        if (metadata == null) {
            throw new CustomException(ErrorCode.REDIS_READ_ERROR.getStatus(), "Unknown PostCacheFlag type: " + type);
        }
        return metadata;
    }

    /**
     * <h3>인기 게시글 캐시 플래그 적용</h3>
     * <p>주어진 게시글 ID 목록에 대해 인기 게시글 캐시 플래그를 적용합니다.</p>
     *
     * @param postIds 게시글 ID 목록
     * @param postCacheFlag 게시글 캐시 유형
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public void applyPopularFlag(List<Long> postIds, PostCacheFlag postCacheFlag) {
        if (postIds == null || postIds.isEmpty()) {
            return;
        }
        QPost post = QPost.post;
        jpaQueryFactory.update(post)
                .set(post.postCacheFlag, postCacheFlag)
                .where(post.id.in(postIds))
                .execute();
    }

    /**
     * <h3>인기 게시글 캐시 플래그 초기화</h3>
     * <p>주어진 게시글 캐시 유형에 해당하는 인기 게시글 캐시 플래그를 초기화합니다.</p>
     *
     * @param postCacheFlag 게시글 캐시 유형
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public void resetPopularFlag(PostCacheFlag postCacheFlag) {
        QPost post = QPost.post;
        jpaQueryFactory.update(post)
                .set(post.postCacheFlag, (PostCacheFlag) null)
                .where(post.postCacheFlag.eq(postCacheFlag))
                .execute();
    }

    /**
     * <h3>인기 게시글 캐시 삭제</h3>
     * <p>주어진 게시글 캐시 유형에 해당하는 인기 게시글 캐시를 Redis에서 삭제합니다.</p>
     *
     * @param type 게시글 캐시 유형
     * @throws CustomException Redis 삭제 오류 발생 시
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deletePopularPostsCache(PostCacheFlag type) {
        CacheMetadata metadata = getCacheMetadata(type);
        try {
            redisTemplate.delete(metadata.key());
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_DELETE_ERROR, e);
        }
    }

    /**
     * <h3>전체 게시글 캐시 삭제</h3>
     * <p>주어진 게시글 ID에 해당하는 전체 게시글 캐시를 Redis에서 삭제합니다.</p>
     *
     * @param postId 게시글 ID
     * @throws CustomException Redis 삭제 오류 발생 시
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteFullPostCache(Long postId) {
        String key = FULL_POST_CACHE_PREFIX + postId;
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_DELETE_ERROR, e);
        }
    }

    /**
     * <h3>게시글 전체 캐시 (목록 + 상세)</h3>
     * <p>게시글 목록과 각 게시글의 상세 정보를 함께 캐시합니다.</p>
     * <p>PostDetail에서 PostSearchResult를 추출하여 목록 캐시를 생성하고,</p>
     * <p>각 PostDetail을 개별 상세 캐시로 저장합니다.</p>
     *
     * @param type 캐시할 게시글 유형
     * @param fullPosts 캐시할 게시글 상세 정보 목록
     * @throws CustomException Redis 쓰기 오류 발생 시
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void cachePostsWithDetails(PostCacheFlag type, List<PostDetail> fullPosts) {
        if (fullPosts == null || fullPosts.isEmpty()) {
            return;
        }
        
        CacheMetadata metadata = getCacheMetadata(type);
        
        try {
            // 1. 목록 캐시: PostDetail -> PostSearchResult 변환 후 저장
            List<PostSearchResult> searchResults = fullPosts.stream()
                    .map(PostDetail::toSearchResult)
                    .toList();
            redisTemplate.opsForValue().set(metadata.key(), searchResults, metadata.ttl());
            
            // 2. 상세 캐시: 각 PostDetail 개별 저장
            for (PostDetail post : fullPosts) {
                String key = FULL_POST_CACHE_PREFIX + post.id();
                redisTemplate.opsForValue().set(key, post, FULL_POST_CACHE_TTL);
            }
            
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_WRITE_ERROR, e);
        }
    }

}
