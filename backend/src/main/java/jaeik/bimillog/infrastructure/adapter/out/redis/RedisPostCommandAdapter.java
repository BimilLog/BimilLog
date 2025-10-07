package jaeik.bimillog.infrastructure.adapter.out.redis;

import jaeik.bimillog.domain.post.application.port.out.RedisPostCommandPort;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.exception.PostErrorCode;
import jaeik.bimillog.infrastructure.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
    private static final String POSTIDS_PREFIX = "cache:postids:";
    private static final String REALTIME_POPULAR_SCORE_KEY = "cache:realtime:scores";
    private static final double SCORE_DECAY_RATE = 0.9;
    private static final double SCORE_THRESHOLD = 1.0;
    private static final Duration FULL_POST_CACHE_TTL = Duration.ofMinutes(5);
    private static final Duration POSTIDS_TTL_WEEKLY_LEGEND = Duration.ofDays(1);
    private static final RedisScript<Long> SCORE_DECAY_SCRIPT;

    static {
        String luaScript =
            "local members = redis.call('ZRANGE', KEYS[1], 0, -1, 'WITHSCORES') " +
            "for i = 1, #members, 2 do " +
            "    local member = members[i] " +
            "    local score = tonumber(members[i + 1]) " +
            "    local newScore = score * tonumber(ARGV[1]) " +
            "    redis.call('ZADD', KEYS[1], newScore, member) " +
            "end " +
            "return redis.call('ZCARD', KEYS[1])";

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(luaScript);
        script.setResultType(Long.class);
        SCORE_DECAY_SCRIPT = script;
    }

    private static Map<PostCacheFlag, CacheMetadata> initializeCacheMetadata() {
        Map<PostCacheFlag, CacheMetadata> map = new EnumMap<>(PostCacheFlag.class);
        map.put(PostCacheFlag.REALTIME, new CacheMetadata("cache:posts:realtime", Duration.ofMinutes(5)));
        map.put(PostCacheFlag.WEEKLY, new CacheMetadata("cache:posts:weekly", Duration.ofMinutes(5)));
        map.put(PostCacheFlag.LEGEND, new CacheMetadata("cache:posts:legend", Duration.ofMinutes(5)));
        map.put(PostCacheFlag.NOTICE, new CacheMetadata("cache:posts:notice", Duration.ofMinutes(5)));
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
     * <h3>인기글 postId 영구 저장</h3>
     * <p>인기글 postId 목록만 Redis List에 영구 또는 긴 TTL로 저장합니다.</p>
     * <p>목록 캐시 TTL 만료 시 복구용으로 사용됩니다.</p>
     *
     * @param type  캐시할 게시글 유형 (WEEKLY, LEGEND, NOTICE)
     * @param postIds 캐시할 게시글 ID 목록 (이미 인기도 순으로 정렬됨)
     * @throws CustomException Redis 쓰기 오류 발생 시
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void cachePostIdsOnly(PostCacheFlag type, List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return;
        }

        String postIdsKey = POSTIDS_PREFIX + type.name().toLowerCase();

        try {
            // 기존 postIds 캐시 삭제
            redisTemplate.delete(postIdsKey);

            // List에 postId만 저장 (RPUSH로 순서대로 삽입)
            for (Long postId : postIds) {
                redisTemplate.opsForList().rightPush(postIdsKey, postId.toString());
            }

            // TTL 설정: 주간/레전드는 1일, 공지는 영구
            if (type == PostCacheFlag.NOTICE) {
                // 공지는 TTL 설정하지 않음 (영구)
            } else {
                // 주간/레전드는 1일
                redisTemplate.expire(postIdsKey, POSTIDS_TTL_WEEKLY_LEGEND);
            }

        } catch (Exception e) {
            throw new PostCustomException(PostErrorCode.REDIS_WRITE_ERROR, e);
        }
    }

    /**
     * <h3>인기글 postId 목록 캐싱</h3>
     * <p>인기글 postId 목록만 Redis List에 저장합니다 (상세 정보는 저장하지 않음).</p>
     * <p>메모리 효율을 위해 postId만 저장하고, 조회 시 캐시 어사이드 패턴으로 PostDetail을 조회합니다.</p>
     *
     * @param type  캐시할 게시글 유형 (WEEKLY, LEGEND, NOTICE)
     * @param postIds 캐시할 게시글 ID 목록 (이미 인기도 순으로 정렬됨)
     * @throws CustomException Redis 쓰기 오류 발생 시
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void cachePostIds(PostCacheFlag type, List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return;
        }

        CacheMetadata metadata = getCacheMetadata(type);

        try {
            // List에 postId만 저장 (RPUSH로 순서대로 삽입)
            String listKey = metadata.key();
            for (Long postId : postIds) {
                redisTemplate.opsForList().rightPush(listKey, postId.toString());
            }
            // TTL 설정
            redisTemplate.expire(listKey, metadata.ttl());

        } catch (Exception e) {
            throw new PostCustomException(PostErrorCode.REDIS_WRITE_ERROR, e);
        }
    }

    /**
     * <h3>단일 게시글 상세 정보 캐싱</h3>
     * <p>게시글 상세 정보를 Redis 캐시에 저장합니다 (캐시 어사이드 패턴).</p>
     *
     * @param postDetail 캐시할 게시글 상세 정보
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void cachePostDetail(PostDetail postDetail) {
        String key = FULL_POST_CACHE_PREFIX + postDetail.getId();
        try {
            redisTemplate.opsForValue().set(key, postDetail, FULL_POST_CACHE_TTL);
        } catch (Exception e) {
            throw new PostCustomException(PostErrorCode.REDIS_WRITE_ERROR, e);
        }
    }

    /**
     * <h3>단일 게시글 캐시 무효화</h3>
     * <p>특정 게시글의 캐시 데이터를 Redis에서 삭제합니다 (라이트 어라운드 패턴).</p>
     *
     * @param postId 캐시를 삭제할 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteSinglePostCache(Long postId) {
        String detailKey = FULL_POST_CACHE_PREFIX + postId;
        try {
            redisTemplate.delete(detailKey);
        } catch (Exception e) {
            throw new PostCustomException(PostErrorCode.REDIS_DELETE_ERROR, e);
        }
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
                // 특정 게시글의 모든 캐시 삭제
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
            // LREM: 0 = 모든 매칭 요소 제거
            redisTemplate.opsForList().remove(metadata.key(), 0, postIdStr);
        }
    }
    
    private void deleteTypeCacheWithDetails(PostCacheFlag type) {
        CacheMetadata metadata = getCacheMetadata(type);

        // 1. 목록 캐시에서 게시글 ID들을 먼저 조회
        List<Object> postIds = redisTemplate.opsForList().range(metadata.key(), 0, -1);

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
            // LREM: 0 = 모든 매칭 요소 제거
            redisTemplate.opsForList().remove(metadata.key(), 0, postIdStr);
        }
    }

    /**
     * <h3>실시간 인기글 점수 증가</h3>
     * <p>Redis Sorted Set에서 특정 게시글의 점수를 증가시킵니다.</p>
     * <p>이벤트 리스너에서 조회/댓글/추천 이벤트 발생 시 호출됩니다.</p>
     *
     * @param postId 점수를 증가시킬 게시글 ID
     * @param score  증가시킬 점수 (조회: 2점, 댓글: 3점, 추천: 4점)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void incrementRealtimePopularScore(Long postId, double score) {
        try {
            redisTemplate.opsForZSet().incrementScore(REALTIME_POPULAR_SCORE_KEY, postId.toString(), score);
        } catch (Exception e) {
            throw new PostCustomException(PostErrorCode.REDIS_WRITE_ERROR, e);
        }
    }

    /**
     * <h3>실시간 인기글 전체 점수 지수감쇠 적용</h3>
     * <p>Redis Sorted Set의 모든 게시글 점수에 0.9를 곱하고, 임계값(1점) 이하의 게시글을 제거합니다.</p>
     * <p>PostScheduledService 스케줄러에서 5분마다 호출됩니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void applyRealtimePopularScoreDecay() {
        try {
            // 1. 모든 항목의 점수에 0.9 곱하기 (Lua 스크립트 사용)
            redisTemplate.execute(
                SCORE_DECAY_SCRIPT,
                List.of(REALTIME_POPULAR_SCORE_KEY),
                SCORE_DECAY_RATE
            );

            // 2. 임계값(1점) 이하의 게시글 제거
            redisTemplate.opsForZSet().removeRangeByScore(REALTIME_POPULAR_SCORE_KEY, 0, SCORE_THRESHOLD);

        } catch (Exception e) {
            throw new PostCustomException(PostErrorCode.REDIS_WRITE_ERROR, e);
        }
    }

    /**
     * <h3>postIds 저장소에 단일 게시글 추가</h3>
     * <p>postIds 영구 저장소의 앞에 게시글 ID를 추가합니다 (LPUSH).</p>
     * <p>공지사항 설정 시 호출됩니다.</p>
     *
     * @param type 캐시 유형 (NOTICE만 사용)
     * @param postId 추가할 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void addPostIdToStorage(PostCacheFlag type, Long postId) {
        String postIdsKey = POSTIDS_PREFIX + type.name().toLowerCase();
        try {
            redisTemplate.opsForList().leftPush(postIdsKey, postId.toString());
        } catch (Exception e) {
            throw new PostCustomException(PostErrorCode.REDIS_WRITE_ERROR, e);
        }
    }

    /**
     * <h3>postIds 저장소에서 단일 게시글 제거</h3>
     * <p>postIds 영구 저장소에서 게시글 ID를 제거합니다 (LREM).</p>
     * <p>공지사항 해제 시 호출됩니다.</p>
     *
     * @param type 캐시 유형 (NOTICE만 사용)
     * @param postId 제거할 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void removePostIdFromStorage(PostCacheFlag type, Long postId) {
        String postIdsKey = POSTIDS_PREFIX + type.name().toLowerCase();
        try {
            // LREM: 0 = 모든 매칭 요소 제거
            redisTemplate.opsForList().remove(postIdsKey, 0, postId.toString());
        } catch (Exception e) {
            throw new PostCustomException(PostErrorCode.REDIS_WRITE_ERROR, e);
        }
    }

}
