package jaeik.bimillog;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * <h2>BimilLogApplicationTests</h2>
 * <p>
 * Growfarm 애플리케이션의 컨텍스트 로딩 테스트입니다.
 * </p>
 * @version 2.0.0
 * @author Jaeik
 */
@ActiveProfiles("h2test")
@SpringBootTest
@Tag("fast-integration")
class BimilLogApplicationTests {

    @Test
    void contextLoads() {
    }

}
