package jaeik.bimillog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * <h2>BimilLog 메인 애플리케이션 클래스</h2>
 * <p>
 * Spring Boot 애플리케이션의 진입점
 * </p>
 * <p>
 * JPA Auditing, 스케줄링, 비동기 처리 기능을 활성화
 * </p>
 * 
 * @since 2.0.0
 * @author Jaeik
 */
@EnableJpaAuditing
@EnableScheduling
@EnableAsync
@SpringBootApplication
public class BimilLogApplication {

    /**
     * <h3>애플리케이션 시작점</h3>
     *
     * <p>
     * Spring Boot 애플리케이션을 시작한다.
     * </p>
     * 
     * @since 2.0.0
     * @author Jaeik
     * @param args 명령행 인수
     */
    public static void main(String[] args) {
        SpringApplication.run(BimilLogApplication.class, args);
    }

}
