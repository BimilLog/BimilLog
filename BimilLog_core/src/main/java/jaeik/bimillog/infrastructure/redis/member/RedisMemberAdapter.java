package jaeik.bimillog.infrastructure.redis.member;

import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.bimillog.domain.member.dto.SimpleMemberDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisMemberAdapter {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String MEMBER_KEY = "member:page:%d:size:%d";
    private static final int MEMBER_TTL = 60;
    private static final long SOFT_TTL_MILLIS = 50_000L;

    public record CachedMemberPage(long cachedAt, List<SimpleMemberDTO> data) {
        public boolean isStale() {
            return System.currentTimeMillis() - cachedAt >= SOFT_TTL_MILLIS;
        }
    }

    public CachedMemberPage lookup(int page, int size) {
        String raw = redisTemplate.opsForValue().get(String.format(MEMBER_KEY, page, size));
        if (raw == null) return null;
        try {
            return objectMapper.readValue(raw, CachedMemberPage.class);
        } catch (Exception e) {
            log.warn("회원 캐시 역직렬화 오류");
            return null;
        }
    }

    public void saveMemberPage(int page, int size, List<SimpleMemberDTO> data) {
        try {
            String json = objectMapper.writeValueAsString(new CachedMemberPage(System.currentTimeMillis(), data));
            redisTemplate.opsForValue().set(String.format(MEMBER_KEY, page, size), json, MEMBER_TTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("회원 캐시 직렬화 실패");
        }
    }
}
