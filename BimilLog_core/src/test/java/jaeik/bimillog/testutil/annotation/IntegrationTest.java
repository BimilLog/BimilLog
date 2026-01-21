package jaeik.bimillog.testutil.annotation;

import jaeik.bimillog.testutil.config.LocalIntegrationTestSupportConfig;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.*;

/**
 * <h2>통합 테스트 메타 애노테이션</h2>
 * <p>통합 테스트에 필요한 모든 설정을 하나의 애노테이션으로 제공</p>
 * <p>Spring Boot, 로컬 인프라 연동, 트랜잭션 설정을 자동으로 적용</p>
 * 
 * <h3>포함된 설정:</h3>
 * <ul>
 *   <li>@SpringBootTest - Spring Boot 전체 컨텍스트 로드</li>
 *   <li>@AutoConfigureWebMvc - MockMvc 자동 설정</li>
 *   <li>@Transactional - 테스트 후 자동 롤백</li>
 *   <li>@Tag("local-integration") - 로컬 통합 테스트 태그</li>
 *   <li>@ActiveProfiles("local-integration") - 로컬 통합 테스트용 프로파일 활성화</li>
 * </ul>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Import(LocalIntegrationTestSupportConfig.class)
@Transactional
@Tag("local-integration")
@ActiveProfiles("local-integration")
@TestPropertySource(properties = "spring.task.scheduling.enabled=false")
public @interface IntegrationTest {
    /**
     * 테스트 설명 (선택적)
     * DisplayName을 직접 사용하는 것을 권장
     */
    String value() default "";
}
