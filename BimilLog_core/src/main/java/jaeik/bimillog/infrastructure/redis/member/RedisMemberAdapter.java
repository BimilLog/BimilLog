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

    public Page<SimpleMemberDTO> getMemberByPage(int page, int size) {
        int start = (page) * size;
        int end = start + size - 1;
        int startPage = (start / size);
        int endPage = (end / size);

        List<SimpleMemberDTO> merged = new ArrayList<>();
        for (int i = startPage; i < endPage; i++) {
            String memberInfo = redisTemplate.opsForValue().get(MEMBER_KEY + i);

            if (memberInfo == null) {
                return Page.empty();
            }

            try {
                List<SimpleMemberDTO> list = objectMapper.readValue(
                        memberInfo,
                        new TypeReference<>() {}
                );

                merged.addAll(list);
            } catch (Exception e) {
                log.warn("회원 캐시 레디스 역직렬화 오류");
            }
        }

        int fromIndex = start % size;
        int toIndex = Math.min(fromIndex + size, merged.size());
        List<SimpleMemberDTO> content = merged.subList(fromIndex, toIndex);

        Pageable pageable = PageRequest.of(page, size);
        return new PageImpl<>(content, pageable, content.size());
    }

    public void saveMemberPage(int page, int size, List<SimpleMemberDTO> dto) {
        int total = dto.size();
        int pageCount = (int) Math.ceil((double) total / size);

        for (int i = 0; i < pageCount; i++) {
            int from = i * size;
            int to = Math.min(from + size, total);
            List<SimpleMemberDTO> subList = dto.subList(from, to);

            try {
                String memberInfo = objectMapper.writeValueAsString(subList);
                redisTemplate.opsForValue().set(MEMBER_KEY + (i + page), memberInfo, MEMBER_TTL, TimeUnit.MINUTES);
            } catch (Exception e) {
                log.warn("유저 캐시 직렬화 실패");
            }
        }
    }
}
