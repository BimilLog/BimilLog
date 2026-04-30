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

    /**
     * <h3>회원 페이지 캐시 레코드</h3>
     * <p>{@code totalElements}는 전체 회원 수를 의미한다. 캐시 히트 시
     * {@link org.springframework.data.domain.PageImpl} 복원에 사용되어
     * 페이지네이션 totalPages 계산이 올바르게 이루어지도록 한다.</p>
     */
    public record CachedMemberPage(long cachedAt, List<SimpleMemberDTO> data, long totalElements) {
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

    public void saveMemberPage(int page, int size, List<SimpleMemberDTO> data, long totalElements) {
        try {
            String json = objectMapper.writeValueAsString(new CachedMemberPage(System.currentTimeMillis(), data, totalElements));
            redisTemplate.opsForValue().set(String.format(MEMBER_KEY, page, size), json, MEMBER_TTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("회원 캐시 직렬화 실패");
        }
    }
}
