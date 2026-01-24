package jaeik.bimillog.testutil;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Objects;

/**
 * <h2>Redis 테스트 헬퍼</h2>
 * <p>Redis 관련 테스트에서 반복되는 보일러플레이트 코드를 제거하기 위한 유틸리티</p>
 * <p>여러 테스트 클래스에서 공통으로 사용되는 Redis 초기화 기능 제공</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public class RedisTestHelper {

    /**
     * Redis 데이터 초기화 (TestContainers 통합 테스트용)
     *
     * @param redisTemplate 실제 RedisTemplate
     */
    public static void flushRedis(RedisTemplate<String, Object> redisTemplate) {
        try (RedisConnection connection = Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection()) {
            connection.serverCommands().flushAll();
        } catch (Exception e) {
            System.err.println("Redis flush warning: " + e.getMessage());
        }
    }
}
