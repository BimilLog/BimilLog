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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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


    @SuppressWarnings("rawtypes")
    private static final DefaultRedisScript<List> PER_SCRIPT;

    static {
        PER_SCRIPT = new DefaultRedisScript<>();
        PER_SCRIPT.setScriptText("""
                
                local key   = KEYS[1] -- 키
                
                -- TTL 검사 TTL이 만료되었으면 바로 null을 반환
                local ttl = redis.call('TTL', key)
                if ttl < 0 then
                    return nil
                end
                
                -- TTL이 남아있으면 TTL과 함께 캐시 반환
                local val = redis.call('GET', key)
                return {val, ttl}
                """);
        PER_SCRIPT.setResultType(List.class);
    }

    /**
     * <h3>PER 기반 멤버 캐시 조회</h3>
     * <p>GET + TTL + 갱신 판단을 Lua 스크립트로 원자적으로 수행합니다.</p>
     */
    @SuppressWarnings("unchecked")
    public Optional<CacheMemberDTO> getMemberByPageWithPER(int page, int size) {
        String key = String.format(MEMBER_KEY, page, size);

        List<Object> result = (List<Object>) redisTemplate.execute(
                PER_SCRIPT,
                List.of(key)
        );

        if (result == null || result.isEmpty()) {
            return Optional.empty();
        }

        Object o = result.get(1);
        Double computeTTL = (Double) o;

        try {
            List<SimpleMemberDTO> list = objectMapper.readValue((String) result.getFirst(), new TypeReference<>() {});
            Page<SimpleMemberDTO> simpleMemberDTOS = new PageImpl<>(list, PageRequest.of(page, size), list.size());
            return Optional.of(CacheMemberDTO.from(simpleMemberDTOS, computeTTL));
        } catch (Exception e) {
            log.warn("회원 캐시 레디스 조회 오류");
        }
        return Optional.empty();
    }

    public Page<SimpleMemberDTO> getMemberByPage(int page, int size) {
        String key = String.format(MEMBER_KEY, page, size);
        String memberInfo = redisTemplate.opsForValue().get(key);
        if (memberInfo == null) {
            return Page.empty();
        }
        try {
            List<SimpleMemberDTO> list = objectMapper.readValue(memberInfo, new TypeReference<>() {
            });
            return new PageImpl<>(list, PageRequest.of(page, size), list.size());
        } catch (Exception e) {
            log.warn("회원 캐시 레디스 역직렬화 오류");
            return Page.empty();
        }
    }

    public void saveMemberPage(int page, int size, List<SimpleMemberDTO> dto) {
        String key = String.format(MEMBER_KEY, page, size);
        try {
            String memberInfo = objectMapper.writeValueAsString(dto);
            redisTemplate.opsForValue().set(key, memberInfo, MEMBER_TTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("유저 캐시 직렬화 실패");
        }
    }
}
