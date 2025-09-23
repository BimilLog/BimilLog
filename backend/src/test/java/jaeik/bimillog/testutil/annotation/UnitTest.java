package jaeik.bimillog.testutil.annotation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.annotation.*;

/**
 * <h2>단위 테스트 메타 애노테이션</h2>
 * <p>단위 테스트에 필요한 공통 설정을 하나의 애노테이션으로 제공</p>
 * <p>MockitoExtension과 태그를 자동으로 적용</p>
 * 
 * <h3>포함된 설정:</h3>
 * <ul>
 *   <li>@ExtendWith(MockitoExtension.class) - Mockito 설정</li>
 *   <li>@Tag("unit") - 단위 테스트 태그</li>
 *   <li>@DisplayName 사용 가능</li>
 * </ul>
 * 
 * <h3>사용 예시:</h3>
 * <pre>
 * {@literal @}UnitTest
 * {@literal @}DisplayName("UserService 단위 테스트")
 * class UserServiceTest extends BaseUnitTest {
 *     {@literal @}Mock UserRepository repository;
 *     {@literal @}InjectMocks UserService service;
 *     
 *     {@literal @}Test
 *     void test() {
 *         // 테스트 코드
 *     }
 * }
 * </pre>
 * 
 * <h3>선택적 실행:</h3>
 * <pre>
 * # 단위 테스트만 실행
 * ./gradlew test -Dgroups=unit
 * 
 * # 단위 테스트 제외하고 실행
 * ./gradlew test -DexcludedGroups=unit
 * </pre>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ExtendWith(MockitoExtension.class)
@Tag("unit")
public @interface UnitTest {
    /**
     * 테스트 설명 (선택적)
     * DisplayName을 직접 사용하는 것을 권장
     */
    String value() default "";
}