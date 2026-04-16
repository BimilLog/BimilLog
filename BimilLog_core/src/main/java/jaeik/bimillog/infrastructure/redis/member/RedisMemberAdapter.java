package jaeik.bimillog.infrastructure.redis.member;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.bimillog.domain.member.dto.SimpleMemberDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisMemberAdapter {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String MEMBER_KEY = "member:page:%d:size:%d";
    private static final int MEMBER_TTL = 60;
    private static final double COMPUTE_TIME = 0.1; // 예상 DB 조회 시간 (초)
    private static final double BETA = 20.0;        // PER 베타 값

    @SuppressWarnings("rawtypes")
    private static final DefaultRedisScript<List> PER_SCRIPT;

    static {
        PER_SCRIPT = new DefaultRedisScript<>();
        PER_SCRIPT.setScriptText("""
                local t = redis.call('TIME')
                math.randomseed(tonumber(t[1]) * 1000000 + tonumber(t[2]))

                local key   = KEYS[1]
                local delta = tonumber(ARGV[1])
                local beta  = tonumber(ARGV[2])

                local val = redis.call('GET', key)
                if val == false then
                    return nil
                end

                local ttl = redis.call('TTL', key)
                if ttl < 0 then
                    return nil
                end

                local gap = delta * beta * (-math.log(math.random()))
                if gap > ttl then
                    return {"1"}
                end

                return {"0", val}
                """);
        PER_SCRIPT.setResultType(List.class);
    }

    /**
     * <h3>PER 기반 멤버 캐시 조회</h3>
     * <p>GET + TTL + 갱신 판단을 Lua 스크립트로 원자적으로 수행합니다.</p>
     */
    @SuppressWarnings("unchecked")
    public MemberCacheResult getMemberByPageWithPER(int page, int size) {
        String key = String.format(MEMBER_KEY, page, size);

        List<Object> result = (List<Object>) redisTemplate.execute(
                PER_SCRIPT,
                List.of(key),
                String.valueOf(COMPUTE_TIME),
                String.valueOf(BETA)
        );

        if (result == null || result.isEmpty()) {
            return MemberCacheResult.miss();
        }

        if ("1".equals(result.getFirst())) {
            return MemberCacheResult.earlyRefresh();
        }

        try {
            List<SimpleMemberDTO> list = objectMapper.readValue(
                    (String) result.get(1), new TypeReference<>() {});
            return MemberCacheResult.hit(new PageImpl<>(list, PageRequest.of(page, size), list.size()));
        } catch (Exception e) {
            log.warn("회원 캐시 레디스 역직렬화 오류");
            return MemberCacheResult.miss();
        }
    }

    public Page<SimpleMemberDTO> getMemberByPage(int page, int size) {
        String key = String.format(MEMBER_KEY, page, size);
        String memberInfo = redisTemplate.opsForValue().get(key);
        if (memberInfo == null) {
            return Page.empty();
        }
        try {
            List<SimpleMemberDTO> list = objectMapper.readValue(memberInfo, new TypeReference<>() {});
            return new PageImpl<>(list, PageRequest.of(page, size), list.size());
        } catch (Exception e) {
            log.warn("회원 캐시 레디스 역직렬화 오류");
            return Page.empty();
        }
    }

    public void saveMemberPage(int page, int size, List<SimpleMemberDTO> dto) {
        String key = String.format(MEMBER_KEY, page, size);
        int jitter = ThreadLocalRandom.current().nextInt(-10, 11);
        try {
            String memberInfo = objectMapper.writeValueAsString(dto);
            redisTemplate.opsForValue().set(key, memberInfo, MEMBER_TTL + jitter, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("유저 캐시 직렬화 실패");
        }
    }
}
