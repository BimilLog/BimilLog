package jaeik.bimillog.infrastructure.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <h2>Redisson 설정</h2>
 * <p>분산 락(RLock) 구현을 위한 RedissonClient 빈을 생성합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Bean
    public RedissonClient redissonClient() {
        String address = String.format("redis://%s:%d", redisHost, redisPort);
        Config config = new Config();
        config.setThreads(8);
        config.setNettyThreads(8);
        config.useSingleServer()
                .setAddress(address)
                .setConnectionPoolSize(30)
                .setConnectionMinimumIdleSize(10)
                .setTimeout(3000)
                .setPassword(redisPassword);
        return Redisson.create(config);
    }
}
