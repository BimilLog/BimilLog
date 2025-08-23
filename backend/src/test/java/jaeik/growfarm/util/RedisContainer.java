package jaeik.growfarm.util;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;

/**
 * <h2>Redis TestContainer 설정 클래스</h2>
 * <p>Testcontainers를 사용하여 Redis 컨테이너를 관리하고 RedisTemplate 빈을 설정합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public abstract class RedisContainer {

    static final String REDIS_IMAGE = "redis:latest";
    static final GenericContainer REDIS_CONTAINER;

    static {
        REDIS_CONTAINER = new GenericContainer<>(REDIS_IMAGE)
                .withExposedPorts(6379)
                .withReuse(true);
        REDIS_CONTAINER.start();
    }

    /**
     * <h3>redisProperties</h3>
     * <p>Redis 컨테이너의 호스트와 포트를 Spring 속성에 동적으로 추가합니다.</p>
     * @param registry DynamicPropertyRegistry 객체
     * @return void
     * @author Jaeik
     * @version 2.0.0
     */
    @DynamicPropertySource
    public static void redisProperties(DynamicPropertyRegistry registry){
        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port", () -> ""+REDIS_CONTAINER.getMappedPort(6379));
    }

    /**
     * <h2>Redis 테스트 설정 클래스</h2>
     * <p>RedisTemplate과 RedisConnectionFactory 빈을 정의합니다.</p>
     *
     * @author Jaeik
     * @version 2.0.0
     */
    @TestConfiguration
    static class RedisTestConfig {
        /**
         * <h3>redisConnectionFactory</h3>
         * <p>Lettuce 기반의 Redis 연결 팩토리를 생성합니다.</p>
         * @return RedisConnectionFactory Redis 연결 팩토리 객체
         * @author Jaeik
         * @version 2.0.0
         */
        @Bean
        public RedisConnectionFactory redisConnectionFactory() {
            return new LettuceConnectionFactory(REDIS_CONTAINER.getHost(), REDIS_CONTAINER.getMappedPort(6379));
        }

        /**
         * <h3>redisTemplate</h3>
         * <p>RedisTemplate 빈을 설정합니다. 키는 String, 값은 JSON으로 직렬화됩니다.</p>
         * @return RedisTemplate RedisTemplate 객체
         * @author Jaeik
         * @version 2.0.0
         */
        @Bean
        public RedisTemplate<String, Object> redisTemplate() {
            RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
            redisTemplate.setConnectionFactory(redisConnectionFactory());
            redisTemplate.setKeySerializer(new StringRedisSerializer());
            redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
            return redisTemplate;
        }
    }
}
