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
