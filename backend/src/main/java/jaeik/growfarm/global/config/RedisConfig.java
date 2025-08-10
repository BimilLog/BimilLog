package jaeik.growfarm.global.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * <h2>Redis 설정 클래스</h2>
 * <p>Redis 데이터베이스와의 연결 및 캐시 관리를 위한 설정을 정의합니다.</p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * <h3>RedisTemplate을 Bean으로 등록합니다.</h3>
     * <p>Redis와의 데이터 직렬화 및 역직렬화를 설정합니다.</p>
     *
     * @param connectionFactory RedisConnectionFactory
     * @return template RedisTemplate<String, Object>
     * @author Jaeik
     * @since 2.0.0
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // ObjectMapper 설정
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // Java 8 시간 타입 지원
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        // GenericJackson2JsonRedisSerializer에 ObjectMapper 설정 적용
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setDefaultSerializer(serializer);

        return template;
    }

    /**
     * <h3>RedisCacheManager를 Bean으로 등록합니다.</h3>
     * <p>Redis를 캐시 매니저로 사용하여 캐시 설정을 정의합니다.</p>
     *
     * @param connectionFactory RedisConnectionFactory
     * @return cacheManager CacheManager
     * @author Jaeik
     * @since 2.0.0
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        return RedisCacheManager.RedisCacheManagerBuilder
                .fromConnectionFactory(connectionFactory)
                .cacheDefaults(cacheConfiguration())
                .build();
    }

    /**
     * <h3>Redis 캐시 설정을 정의합니다.</h3>
     * <p>캐시의 기본 TTL(Time To Live) 및 직렬화 방식을 설정합니다.</p>
     *
     * @return RedisCacheConfiguration
     * @author Jaeik
     * @since 2.0.0
     */
    private RedisCacheConfiguration cacheConfiguration() {
        // ObjectMapper 설정 (캐시용)
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(serializer));
    }
}