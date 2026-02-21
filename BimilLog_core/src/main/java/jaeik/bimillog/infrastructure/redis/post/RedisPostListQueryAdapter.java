package jaeik.bimillog.infrastructure.redis.post;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class RedisPostListQueryAdapter {
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * <h3>전체 조회</h3>
     * <p>LRANGE 0 -1 → JSON 파싱하여 PostSimpleDetail 리스트 반환</p>
     */
    public List<PostSimpleDetail> getAll(String key) {
        List<String> jsonList = stringRedisTemplate.opsForList().range(key, 0, -1);
        if (jsonList == null) {
            return Collections.emptyList();
        }

        List<PostSimpleDetail> result = new ArrayList<>(jsonList.size());
        try {
            for (String json : jsonList) {
                result.add(objectMapper.readValue(json, PostSimpleDetail.class));
            }
        } catch (JsonProcessingException e) {
            log.warn("[JSON_LIST] JSON 파싱 실패 (key={}): {}", key, e.getMessage());
        }
        return result;
    }
}
