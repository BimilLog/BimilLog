package jaeik.bimillog.infrastructure.redis.post;

import jaeik.bimillog.infrastructure.redis.RedisKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * <h2>게시글 조회수 버퍼 Redis 어댑터</h2>
 * <p>중복 조회 방지 및 조회수 버퍼링을 담당합니다.</p>
 * <p>실제 카운터(조회수/추천수/댓글수)는 JSON LIST 캐시에 직접 관리합니다.</p>
 *
 * @author Jaeik
 * @version 3.1.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RedisPostViewAdapter {
    private final StringRedisTemplate stringRedisTemplate;

    private static final String VIEW_PREFIX = RedisKey.VIEW_PREFIX;
    private static final Duration VIEW_TTL = Duration.ofSeconds(RedisKey.VIEW_TTL_SECONDS);
    private static final String VIEW_COUNTS_KEY = RedisKey.VIEW_COUNTS_KEY;

    /**
     * <h3>조회 마킹 + 조회수 증가</h3>
     * <p>SET NX EX로 중복 확인과 마킹을 원자적 1커맨드로 처리합니다.</p>
     * <p>키가 새로 생성된 경우에만 조회수 버퍼를 증가시킵니다.</p>
     *
     * @param postId    게시글 ID
     * @param viewerKey 조회자 키 (m:{memberId} 또는 ip:{clientIp})
     * @return 조회수가 증가되었으면 true, 이미 조회한 경우 false
     */
    public boolean markViewedAndIncrement(Long postId, String viewerKey) {
        String key = VIEW_PREFIX + postId + ":" + viewerKey;
        Boolean isNew = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", VIEW_TTL);

        if (Boolean.TRUE.equals(isNew)) {
            stringRedisTemplate.opsForHash().increment(VIEW_COUNTS_KEY, postId.toString(), 1L);
            return true;
        }
        return false;
    }


}
