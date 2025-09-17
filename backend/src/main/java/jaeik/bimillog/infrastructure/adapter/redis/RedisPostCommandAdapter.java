package jaeik.bimillog.infrastructure.adapter.redis;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.post.application.port.out.RedisPostCommandPort;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.QPost;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.exception.PostErrorCode;
import jaeik.bimillog.infrastructure.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <h2>게시글 캐시 명령 어댑터</h2>
 * <p>게시글 캐시 명령 포트의 Redis 구현체입니다.</p>
 * <p>인기글 캐시 데이터 생성, 수정, 삭제</p>
 * <p>인기글 플래그 설정 및 초기화</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class RedisPostCommandAdapter implements RedisPostCommandPort {

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
            throw new PostCustomException(PostErrorCode.REDIS_READ_ERROR, "Unknown PostCacheFlag type: " + type);
        }
        return metadata;
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
            // 1. 목록 캐시: Sorted Set으로 저장 (postId, score)
            String listKey = metadata.key();
            for (PostDetail post : fullPosts) {
                // 타입별 정렬 점수 계산
                double score = switch (type) {
                    case REALTIME, WEEKLY, LEGEND -> post.likeCount();
                    case NOTICE -> -post.createdAt().toEpochMilli();
                };
                redisTemplate.opsForZSet().add(listKey, post.id().toString(), score);
            }
            // TTL 설정
            redisTemplate.expire(listKey, metadata.ttl());

            // 2. 상세 캐시: 각 PostDetail 개별 저장 (기존 방식 유지)
            for (PostDetail post : fullPosts) {
                String key = FULL_POST_CACHE_PREFIX + post.id();
                redisTemplate.opsForValue().set(key, post, FULL_POST_CACHE_TTL);
            }

        } catch (Exception e) {
            throw new PostCustomException(PostErrorCode.REDIS_WRITE_ERROR, e);
        }
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
     * <h3>캐시 삭제</h3>
     * <p>캐시를 삭제합니다. type이 null이면 특정 게시글의 모든 캐시를 삭제하고,</p>
     * <p>type이 지정되면 해당 타입의 목록 캐시와 관련 상세 캐시를 삭제합니다.</p>
     *
     * @param type   캐시할 게시글 유형 (null이면 특정 게시글 삭제 모드)
     * @param postId 게시글 ID (type이 null일 때만 사용)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteCache(PostCacheFlag type, Long postId, PostCacheFlag... targetTypes) {
        try {
            if (type == null && targetTypes.length == 0) {
                // 특정 게시글의 모든 캐시 삭제 (기존 동작 유지)
                deleteSpecificPostCache(postId);
            } else if (type == null) {
                // 특정 게시글의 지정된 캐시만 삭제 (성능 최적화)
                deleteSpecificPostCache(postId, targetTypes);
            } else {
                // 특정 타입의 캐시와 관련 상세 캐시 삭제
                deleteTypeCacheWithDetails(type);
            }
        } catch (Exception e) {
            throw new PostCustomException(PostErrorCode.REDIS_DELETE_ERROR, e);
        }
    }
    
    private void deleteSpecificPostCache(Long postId) {
        // 1. 상세 캐시 삭제
        String detailKey = FULL_POST_CACHE_PREFIX + postId;
        redisTemplate.delete(detailKey);
        
        // 2. 모든 목록 캐시에서 해당 게시글 제거
        String postIdStr = postId.toString();
        for (PostCacheFlag type : PostCacheFlag.getPopularPostTypes()) {
            CacheMetadata metadata = getCacheMetadata(type);
            redisTemplate.opsForZSet().remove(metadata.key(), postIdStr);
        }
    }
    
    private void deleteTypeCacheWithDetails(PostCacheFlag type) {
        CacheMetadata metadata = getCacheMetadata(type);
        
        // 1. 목록 캐시에서 게시글 ID들을 먼저 조회
        Set<Object> postIds = redisTemplate.opsForZSet().range(metadata.key(), 0, -1);
        
        // 2. 목록 캐시 삭제
        redisTemplate.delete(metadata.key());
        
        // 3. 해당 타입에 속했던 게시글들의 상세 캐시도 삭제
        if (postIds != null && !postIds.isEmpty()) {
            List<String> detailKeys = postIds.stream()
                    .map(Object::toString)
                    .map(postId -> FULL_POST_CACHE_PREFIX + postId)
                    .collect(Collectors.toList());
            redisTemplate.delete(detailKeys);
        }
    }
    
    /**
     * <h3>특정 게시글의 지정된 캐시만 삭제</h3>
     * <p>성능 최적화를 위해 지정된 캐시 타입에서만 게시글을 삭제합니다.</p>
     * <p>공지 해제 시 NOTICE 캐시만 스캔하도록 최적화하는데 사용됩니다.</p>
     *
     * @param postId      삭제할 게시글 ID
     * @param targetTypes 삭제할 대상 캐시 타입들
     * @author Jaeik
     * @since 2.0.0
     */
    private void deleteSpecificPostCache(Long postId, PostCacheFlag[] targetTypes) {
        // 1. 상세 캐시 삭제
        String detailKey = FULL_POST_CACHE_PREFIX + postId;
        redisTemplate.delete(detailKey);
        
        // 2. 지정된 목록 캐시에서만 해당 게시글 제거
        String postIdStr = postId.toString();
        for (PostCacheFlag type : targetTypes) {
            CacheMetadata metadata = getCacheMetadata(type);
            redisTemplate.opsForZSet().remove(metadata.key(), postIdStr);
        }
    }


}
