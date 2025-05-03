package jaeik.growfarm.global.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
 * QueryDSL 설정
 * QueryDSL을 사용하기 위한 설정을 포함하고 있습니다.
 * JPAQueryFactory를 빈으로 등록하여 QueryDSL을 사용할 수 있도록 합니다.
 * Spring의 @Configuration 어노테이션을 사용하여 Spring 컨테이너에 등록됩니다.
 * 수정일 : 2025-05-03
 */
@Configuration
@RequiredArgsConstructor
public class QueryDSLConfig {
    private final EntityManager entityManager;

    @Bean
    public JPAQueryFactory jpaQueryFactory(){
        return new JPAQueryFactory(entityManager);
    }
}