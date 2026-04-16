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
