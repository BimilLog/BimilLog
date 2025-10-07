package jaeik.bimillog.infrastructure.adapter.out.redis;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * <h2>Redis 설정 클래스</h2>
 * <p>Redis 데이터베이스와의 연결 및 데이터 직렬화를 위한 설정을 정의합니다.</p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@Configuration
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
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        // 역직렬화 시 알 수 없는 속성 무시 (boolean isLiked/liked 필드 호환성 유지)
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 타입 정보 저장 활성화 (역직렬화 시 정확한 타입으로 복원)
        objectMapper.activateDefaultTyping(
            objectMapper.getPolymorphicTypeValidator(),
            ObjectMapper.DefaultTyping.NON_FINAL
        );

        // GenericJackson2JsonRedisSerializer에 ObjectMapper 설정 적용
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setDefaultSerializer(serializer);

        return template;
    }
}