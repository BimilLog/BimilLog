package jaeik.bimillog.infrastructure.redis.member;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.bimillog.domain.member.dto.SimpleMemberDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisMemberAdapter {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String MEMBER_KEY = "member:page:";
    private static final int MEMBER_TTL = 1;
    private static final int BASE_SIZE = 10; // 키 하나당 고정 저장 단위
    private static final double COMPUTE_TIME = 0.1; // 예상 DB 조회 시간 (초)
    private static final double BETA = 20.0; // PER 베타 값

    @SuppressWarnings("rawtypes")
    private static final DefaultRedisScript<List> PER_SCRIPT;

    static {
        PER_SCRIPT = new DefaultRedisScript<>();
        PER_SCRIPT.setScriptText("""
                local t = redis.call('TIME')
                math.randomseed(tonumber(t[1]) * 1000000 + tonumber(t[2]))

                local prefix   = KEYS[1]
                local startKey = tonumber(ARGV[1])
                local endKey   = tonumber(ARGV[2])
                local delta    = tonumber(ARGV[3])
                local beta     = tonumber(ARGV[4])

                local jsonList = {}
                local minTtl   = math.huge

                for i = startKey, endKey do
                    local val = redis.call('GET', prefix .. i)
                    if val == false then
                        return nil
                    end
                    local ttl = redis.call('TTL', prefix .. i)
                    if ttl < 0 then
                        return nil
                    end
                    if ttl < minTtl then
                        minTtl = ttl
                    end
                    table.insert(jsonList, val)
                end

                local gap = delta * beta * (-math.log(math.random()))
                if gap > minTtl then
                    return {"1"}
                end

                local result = {"0"}
                for _, v in ipairs(jsonList) do
                    table.insert(result, v)
                end
                return result
                """);
        PER_SCRIPT.setResultType(List.class);
    }

    /**
     * 멤버 캐시를 PER로 조회
     * <p>Lua 스크립트로 GET + TTL + 갱신 판단을 원자적으로 수행합니다.</p>
     * <ul>
     *   <li>캐시 없음 → MISS</li>
     *   <li>PER 공식 gap > remaining_ttl → EARLY_REFRESH (락 없이 갱신)</li>
     *   <li>그 외 → HIT</li>
     * </ul>
     */
    @SuppressWarnings("unchecked")
    public MemberCacheResult getMemberByPageWithPER(int page, int size) {
        int start = page * size;
        int end = start + size - 1;
        int startKey = start / BASE_SIZE;
        int endKey = end / BASE_SIZE;

        List<Object> result = (List<Object>) redisTemplate.execute(
                PER_SCRIPT,
                List.of(MEMBER_KEY),
                String.valueOf(startKey),
                String.valueOf(endKey),
                String.valueOf(COMPUTE_TIME),
                String.valueOf(BETA)
        );

        if (result == null || result.isEmpty()) {
            return MemberCacheResult.miss();
        }

        String signal = (String) result.getFirst();

        if ("1".equals(signal)) {
            return MemberCacheResult.earlyRefresh();
        }

        List<SimpleMemberDTO> merged = new ArrayList<>();
        for (int i = 1; i < result.size(); i++) {
            try {
                List<SimpleMemberDTO> chunk = objectMapper.readValue(
                        (String) result.get(i),
                        new TypeReference<>() {
                        }
                );
                merged.addAll(chunk);
            } catch (Exception e) {
                log.warn("회원 캐시 레디스 역직렬화 오류", e);
                return MemberCacheResult.miss();
            }
        }

        int fromIndex = start % BASE_SIZE;
        int toIndex = Math.min(fromIndex + size, merged.size());
        List<SimpleMemberDTO> content = merged.subList(fromIndex, toIndex);

        Pageable pageable = PageRequest.of(page, size);
        return MemberCacheResult.hit(new PageImpl<>(content, pageable, content.size()));
    }

    /**
     * 멤버 캐시를 페이징하여 반환
     * <p>키는 BASE_SIZE(10) 단위로 저장되므로, 요청 size가 달라도 올바른 키 범위를 합산하여 반환합니다.</p>
     * <p>예) size=20, page=0 → member:page:0 + member:page:1 합산</p>
     * <p>예) size=30, page=1 → member:page:3 + member:page:4 + member:page:5 합산</p>
     */
    public Page<SimpleMemberDTO> getMemberByPage(int page, int size) {
        int start = page * size;
        int end = start + size - 1;
        int startKey = start / BASE_SIZE;
        int endKey = end / BASE_SIZE;

        List<SimpleMemberDTO> merged = new ArrayList<>();
        for (int i = startKey; i <= endKey; i++) {
            String memberInfo = redisTemplate.opsForValue().get(MEMBER_KEY + i);

            if (memberInfo == null) {
                return Page.empty();
            }

            try {
                List<SimpleMemberDTO> list = objectMapper.readValue(
                        memberInfo,
                        new TypeReference<>() {
                        }
                );
                merged.addAll(list);
            } catch (Exception e) {
                log.warn("회원 캐시 레디스 역직렬화 오류", e);
                return Page.empty();
            }
        }

        int fromIndex = start % BASE_SIZE;
        int toIndex = Math.min(fromIndex + size, merged.size());
        List<SimpleMemberDTO> content = merged.subList(fromIndex, toIndex);

        Pageable pageable = PageRequest.of(page, size);
        return new PageImpl<>(content, pageable, content.size());
    }

    /**
     * 멤버 캐시 삽입
     * <p>BASE_SIZE(10) 단위로 분할하여 저장합니다. page·size로 절대 시작 위치를 계산하여 키를 결정합니다.</p>
     */
    public void saveMemberPage(int page, int size, List<SimpleMemberDTO> dto) {
        if (dto.isEmpty()) {
            return;
        }

        int absoluteStart = page * size;
        int total = dto.size();
        int startKey = absoluteStart / BASE_SIZE;
        int startOffset = absoluteStart % BASE_SIZE; // 첫 키 내 시작 오프셋

        int dtoIndex = 0;
        int currentKey = startKey;
        int currentOffset = startOffset;

        while (dtoIndex < total) {
            int itemsForCurrentKey = Math.min(BASE_SIZE - currentOffset, total - dtoIndex);
            List<SimpleMemberDTO> chunk = dto.subList(dtoIndex, dtoIndex + itemsForCurrentKey);

            try {
                String json = objectMapper.writeValueAsString(chunk);
                redisTemplate.opsForValue().set(MEMBER_KEY + currentKey, json, MEMBER_TTL, TimeUnit.MINUTES);
            } catch (Exception e) {
                log.warn("유저 캐시 직렬화 실패", e);
            }

            dtoIndex += itemsForCurrentKey;
            currentKey++;
            currentOffset = 0; // 두 번째 키부터는 오프셋 0
        }
    }

    public boolean lock(int page, int size) {
        String lockKey = "member:lock";
        int start = page * size;
        int end = start + size - 1;
        int startKey = start / BASE_SIZE;
        int endKey = end / BASE_SIZE;

        List<String> acquired = new ArrayList<>();
        for (int i = startKey; i <= endKey; i++) {
            String key = lockKey + i;
            Boolean locked = redisTemplate.opsForValue().setIfAbsent(key, "lock", Duration.ofSeconds(10));
            if (Boolean.FALSE.equals(locked)) {
                redisTemplate.delete(acquired); // 부분 획득 롤백
                return false;
            }
            acquired.add(key);
        }
        return true;
    }
}
