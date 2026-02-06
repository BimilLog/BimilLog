package jaeik.bimillog.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisCheck {
    private final RedisTemplate<String, Object> redisTemplate;

    public boolean isRedisHealthy() {
        try {
            RedisConnectionFactory connectionFactory = redisTemplate.getConnectionFactory();
            if (connectionFactory == null) {
                return false;
            }
            String pong = connectionFactory.getConnection().ping();
            return "PONG".equals(pong);
        } catch (Exception e) {
            log.warn(" Redis ping 실패", e);
            return false;
        }
    }
}
