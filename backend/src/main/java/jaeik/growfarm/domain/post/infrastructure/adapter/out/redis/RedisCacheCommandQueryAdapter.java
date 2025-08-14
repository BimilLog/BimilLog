package jaeik.growfarm.domain.post.infrastructure.adapter.out.redis;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.domain.post.application.port.out.PostCacheCommandPort;
import jaeik.growfarm.domain.post.application.port.out.PostCacheQueryPort;
import jaeik.growfarm.domain.post.entity.PostCacheFlag;
import jaeik.growfarm.domain.post.entity.QPost;
import jaeik.growfarm.dto.post.FullPostResDTO;
import jaeik.growfarm.dto.post.SimplePostResDTO;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * <h2>Redis 캐시 어댑터</h2>
 * <p>Redis를 사용하여 게시글 캐시 데이터를 관리하는 영속성 어댑터입니다.</p>
 * <p>ManagePostCachePort와 LoadPostCachePort 인터페이스를 구현합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component

public class RedisCacheCommandQueryAdapter implements PostCacheCommandPort, PostCacheQueryPort {

    private final RedisTemplate<String, Object> redisTemplate;
    private final Map<PostCacheFlag, CacheMetadata> cacheMetadataMap;
    private static final String FULL_POST_CACHE_PREFIX = "cache:post:";
    private static final Duration FULL_POST_CACHE_TTL = Duration.ofDays(1);

    private final JPAQueryFactory jpaQueryFactory;




    /**
     * <h3>RedisCacheAdapter 생성자</h3>
     * <p>RedisTemplate을 주입받아 캐시 메타데이터를 초기화합니다.</p>
     *
     * @param redisTemplate Redis 작업을 위한 템플릿
     * @author Jaeik
     * @since 2.0.0
     */
    public RedisCacheCommandQueryAdapter(RedisTemplate<String, Object> redisTemplate, JPAQueryFactory jpaQueryFactory) {
        this.redisTemplate = redisTemplate;
        this.jpaQueryFactory = jpaQueryFactory;
        this.cacheMetadataMap = new EnumMap<>(PostCacheFlag.class);
        cacheMetadataMap.put(PostCacheFlag.REALTIME, new CacheMetadata("cache:posts:realtime", Duration.ofMinutes(30)));
        cacheMetadataMap.put(PostCacheFlag.WEEKLY, new CacheMetadata("cache:posts:weekly", Duration.ofDays(1)));
        cacheMetadataMap.put(PostCacheFlag.LEGEND, new CacheMetadata("cache:posts:legend", Duration.ofDays(1)));
        cacheMetadataMap.put(PostCacheFlag.NOTICE, new CacheMetadata("cache:posts:notice", Duration.ofDays(7)));
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
     * <h3>게시글 목록 캐시</h3>
     * <p>지정된 유형의 게시글 목록을 Redis에 캐시합니다.</p>
     *
     * @param type       게시글 캐시 유형
     * @param cachePosts 캐시할 SimplePostResDTO 목록
     * @throws CustomException Redis 쓰기 오류 발생 시
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void cachePosts(PostCacheFlag type, List<SimplePostResDTO> cachePosts) {
        CacheMetadata metadata = getCacheMetadata(type);
        try {
            redisTemplate.opsForValue().set(metadata.key(), cachePosts, metadata.ttl());
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_WRITE_ERROR, e);
        }
    }




    /**
     * <h3>인기 플래그 적용</h3>
     * <p>주어진 게시글 ID 목록에 특정 캐시 플래그를 적용합니다.</p>
     *
     * @param postIds       캐시 플래그를 적용할 게시글 ID 목록
     * @param postCacheFlag 적용할 캐시 플래그
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
     * <h3>인기 플래그 초기화</h3>
     * <p>주어진 캐시 플래그에 해당하는 게시글들의 플래그를 초기화(null로 설정)합니다.</p>
     *
     * @param postCacheFlag 초기화할 캐시 플래그
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
     * <p>지정된 유형의 인기 게시글 캐시를 Redis에서 삭제합니다.</p>
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
     * <h3>인기 게시글 캐시 존재 여부 확인</h3>
     * <p>지정된 유형의 인기 게시글 캐시가 Redis에 존재하는지 확인합니다.</p>
     *
     * @param type 게시글 캐시 유형
     * @return 캐시 존재 여부
     * @throws CustomException Redis 읽기 오류 발생 시
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public boolean hasPopularPostsCache(PostCacheFlag type) {
        CacheMetadata metadata = getCacheMetadata(type);
        try {
            return redisTemplate.hasKey(metadata.key());
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_READ_ERROR, e);
        }
    }

    /**
     * <h3>전체 게시글 캐시</h3>
     * <p>FullPostResDTO 형태의 게시글 상세 정보를 Redis에 캐시합니다.</p>
     *
     * @param post 캐시할 FullPostResDTO
     * @throws CustomException Redis 쓰기 오류 발생 시
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void cacheFullPost(FullPostResDTO post) {
        String key = FULL_POST_CACHE_PREFIX + post.getId();
        try {
            redisTemplate.opsForValue().set(key, post, FULL_POST_CACHE_TTL);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_WRITE_ERROR, e);
        }
    }

    /**
     * <h3>캐시글 조회</h3>
     * <p>지정된 유형의 캐시글 목록을 Redis에서 조회합니다.</p>
     *
     * @param type 게시글 캐시 유형
     * @return 캐시된 SimplePostResDTO 목록. 캐시가 없으면 빈 리스트 반환
     * @throws CustomException Redis 읽기 오류 발생 시
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<SimplePostResDTO> getCachedPostList(PostCacheFlag type) {
        CacheMetadata metadata = getCacheMetadata(type);
        try {
            Object cached = redisTemplate.opsForValue().get(metadata.key());
            if (cached instanceof List) {
                return (List<SimplePostResDTO>) cached;
            }
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_READ_ERROR, e);
        }
        return Collections.emptyList();
    }

    /**
     * <h3>캐시된 전체 게시글 조회</h3>
     * <p>지정된 게시글 ID의 캐시된 전체 게시글 상세 정보를 Redis에서 조회합니다.</p>
     *
     * @param postId 게시글 ID
     * @return 캐시된 FullPostResDTO. 캐시가 없으면 null 반환
     * @throws CustomException Redis 읽기 오류 발생 시
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public FullPostResDTO getCachedPost(Long postId) {
        String key = FULL_POST_CACHE_PREFIX + postId;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof FullPostResDTO) {
                return (FullPostResDTO) cached;
            }
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_READ_ERROR, e);
        }
        return null;
    }

    /**
     * <h3>전체 게시글 캐시 삭제</h3>
     * <p>지정된 게시글 ID의 전체 게시글 캐시를 Redis에서 삭제합니다.</p>
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


}
