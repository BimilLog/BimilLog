package jaeik.bimillog.testutil.annotation;

import jaeik.bimillog.testutil.TestContainersConfiguration;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.annotation.*;

/**
 * <h2>통합 테스트 메타 애노테이션</h2>
 * <p>통합 테스트에 필요한 모든 설정을 하나의 애노테이션으로 제공</p>
 * <p>Spring Boot, TestContainers, 트랜잭션 설정을 자동으로 적용</p>
 * 
 * <h3>포함된 설정:</h3>
 * <ul>
 *   <li>@SpringBootTest - Spring Boot 전체 컨텍스트 로드</li>
 *   <li>@AutoConfigureWebMvc - MockMvc 자동 설정</li>
 *   <li>@Testcontainers - TestContainers 활성화</li>
 *   <li>@Import(TestContainersConfiguration.class) - MySQL, Redis 컨테이너 설정</li>
 *   <li>@Transactional - 테스트 후 자동 롤백</li>
 *   <li>@Tag("tc") - Testcontainers 통합 테스트 태그</li>
 *   <li>@ActiveProfiles("tc") - TestContainers 프로파일 활성화</li>
 * </ul>
 * 
 * <h3>사용 예시:</h3>
 * <pre>
 * {@literal @}IntegrationTest
 * {@literal @}DisplayName("UserController 통합 테스트")
 * class UserControllerIntegrationTest extends BaseIntegrationTest {
 *     
 *     {@literal @}Test
 *     void test() throws Exception {
 *         performGet("/api/member/me", testUserDetails)
 *             .andExpect(status().isOk());
 *     }
 * }
 * </pre>
 * 
 * <h3>선택적 실행:</h3>
 * <pre>
 * # 통합 테스트만 실행
 * ./gradlew test -Dgroups=integration
 * 
 * # 통합 테스트 제외하고 실행
 * ./gradlew test -DexcludedGroups=integration
 * </pre>
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
@Testcontainers
@Import(TestContainersConfiguration.class)
@Transactional
@Tag("tc")
@ActiveProfiles("tc")
public @interface IntegrationTest {
    /**
     * 테스트 설명 (선택적)
     * DisplayName을 직접 사용하는 것을 권장
     */
    String value() default "";
}