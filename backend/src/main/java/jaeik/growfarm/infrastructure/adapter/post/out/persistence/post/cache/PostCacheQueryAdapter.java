package jaeik.growfarm.infrastructure.adapter.post.out.persistence.post.cache;

import jaeik.growfarm.domain.post.application.port.out.PostCacheQueryPort;
import jaeik.growfarm.domain.post.entity.PostCacheFlag;
import jaeik.growfarm.domain.post.entity.PostDetail;
import jaeik.growfarm.domain.post.entity.PostSearchResult;
import jaeik.growfarm.infrastructure.adapter.post.in.web.dto.FullPostResDTO;
import jaeik.growfarm.infrastructure.adapter.post.in.web.dto.SimplePostResDTO;
import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * <h2>Redis 캐시 조회 어댑터</h2>
 * <p>Redis를 사용하여 게시글 캐시 데이터를 관리하는 영속성 어댑터입니다.</p>
 * <p>PostCacheQueryPort 인터페이스를 구현합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
public class PostCacheQueryAdapter implements PostCacheQueryPort {

    private final RedisTemplate<String, Object> redisTemplate;
    private final Map<PostCacheFlag, CacheMetadata> cacheMetadataMap;
    private static final String FULL_POST_CACHE_PREFIX = "cache:post:";

    /**
     * <h3>RedisCacheAdapter 생성자</h3>
     * <p>RedisTemplate을 주입받아 캐시 메타데이터를 초기화합니다.</p>
     *
     * @param redisTemplate Redis 작업을 위한 템플릿
     * @author Jaeik
     * @since 2.0.0
     */
    public PostCacheQueryAdapter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
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

    @Override
    public boolean hasPopularPostsCache(PostCacheFlag type) {
        CacheMetadata metadata = getCacheMetadata(type);
        try {
            return redisTemplate.hasKey(metadata.key());
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_READ_ERROR, e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PostSearchResult> getCachedPostList(PostCacheFlag type) {
        CacheMetadata metadata = getCacheMetadata(type);
        try {
            Object cached = redisTemplate.opsForValue().get(metadata.key());
            if (cached instanceof List) {
                List<SimplePostResDTO> dtoList = (List<SimplePostResDTO>) cached;
                return dtoList.stream()
                        .map(dto -> PostSearchResult.builder()
                                .id(dto.getId())
                                .title(dto.getTitle())
                                .content(dto.getContent())
                                .viewCount(dto.getViewCount())
                                .likeCount(dto.getLikeCount())
                                .postCacheFlag(dto.getPostCacheFlag())
                                .createdAt(dto.getCreatedAt())
                                .userId(dto.getUserId())
                                .userName(dto.getUserName())
                                .commentCount(dto.getCommentCount())
                                .isNotice(dto.isNotice())
                                .build())
                        .toList();
            }
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_READ_ERROR, e);
        }
        return Collections.emptyList();
    }

    @Override
    public PostDetail getCachedPost(Long postId) {
        String key = FULL_POST_CACHE_PREFIX + postId;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof FullPostResDTO dto) {
                return PostDetail.builder()
                        .id(dto.getId())
                        .title(dto.getTitle())
                        .content(dto.getContent())
                        .viewCount(dto.getViewCount())
                        .likeCount(dto.getLikeCount())
                        .postCacheFlag(dto.getPostCacheFlag())
                        .createdAt(dto.getCreatedAt())
                        .userId(dto.getUserId())
                        .userName(dto.getUserName())
                        .commentCount(dto.getCommentCount())
                        .isNotice(dto.isNotice())
                        .isLiked(dto.isLiked())
                        .build();
            }
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_READ_ERROR, e);
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Page<PostSearchResult> getCachedPostListPaged(PostCacheFlag type, Pageable pageable) {
        CacheMetadata metadata = getCacheMetadata(type);
        try {
            // Redis List 구조에서 페이징 처리
            String listKey = metadata.key() + ":list"; // List 형태로 저장된 키
            
            // 전체 크기 조회
            Long totalSize = redisTemplate.opsForList().size(listKey);
            if (totalSize == null || totalSize == 0) {
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }
            
            // 페이징 계산
            int page = pageable.getPageNumber();
            int size = pageable.getPageSize();
            long start = (long) page * size;
            long end = start + size - 1;
            
            // Redis List에서 범위 조회 (LRANGE 명령어)
            List<Object> cachedObjects = redisTemplate.opsForList().range(listKey, start, end);
            if (cachedObjects == null) {
                return new PageImpl<>(Collections.emptyList(), pageable, totalSize);
            }
            
            // Object를 PostSearchResult로 변환
            List<PostSearchResult> posts = cachedObjects.stream()
                    .filter(obj -> obj instanceof SimplePostResDTO)
                    .map(obj -> (SimplePostResDTO) obj)
                    .map(dto -> PostSearchResult.builder()
                            .id(dto.getId())
                            .title(dto.getTitle())
                            .content(dto.getContent())
                            .viewCount(dto.getViewCount())
                            .likeCount(dto.getLikeCount())
                            .postCacheFlag(dto.getPostCacheFlag())
                            .createdAt(dto.getCreatedAt())
                            .userId(dto.getUserId())
                            .userName(dto.getUserName())
                            .commentCount(dto.getCommentCount())
                            .isNotice(dto.isNotice())
                            .build())
                    .toList();
            
            return new PageImpl<>(posts, pageable, totalSize);
            
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_READ_ERROR, e);
        }
    }
}
