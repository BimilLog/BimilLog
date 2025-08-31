package jaeik.bimillog.infrastructure.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <h2>QueryDSL 설정 클래스</h2>
 * <p>QueryDSL을 사용하기 위한 설정 클래스</p>
 * <p>JPAQueryFactory를 Bean으로 등록하여 QueryDSL을 사용할 수 있도록 설정</p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@Configuration
@RequiredArgsConstructor
public class QueryDSLConfig {
    private final EntityManager entityManager;

    /**
     * <h3>JPAQueryFactory 빈 등록</h3>
     * <p>EntityManager를 사용하여 JPAQueryFactory 빈을 생성하고 반환합니다.</p>
     *
     * @return JPAQueryFactory 객체
     * @author Jaeik
     * @since 2.0.0
     */
    @Bean
    public JPAQueryFactory jpaQueryFactory(){
        return new JPAQueryFactory(entityManager);
    }
}