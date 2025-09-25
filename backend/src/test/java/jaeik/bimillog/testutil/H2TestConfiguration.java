package jaeik.bimillog.testutil;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.infrastructure.security.EncryptionUtil;
import jakarta.persistence.EntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * <h2>H2 테스트 설정 클래스</h2>
 * <p>H2 인메모리 데이터베이스를 사용하는 통합 테스트용 설정</p>
 * <p>Redis가 필요하지 않은 테스트에서 사용</p>
 *
 * @author Jaeik
 * @since 2025
 */
@TestConfiguration(proxyBeanMethods = false)
public class H2TestConfiguration {

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @Bean(name = "testJpaQueryFactory")
    @Primary
    public JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
        return new JPAQueryFactory(entityManager);
    }

    @Bean
    public EncryptionUtil encryptionUtil() {
        return new EncryptionUtil();
    }
}