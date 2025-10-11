package jaeik.bimillog.testutil.config;

import jaeik.bimillog.infrastructure.security.EncryptionUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.SyncTaskExecutor;

import java.util.concurrent.Executor;

/**
 * <h2>로컬 통합 테스트 지원 설정</h2>
 * <p>local-integration 프로파일에서 필요한 공통 테스트 빈을 제공</p>
 */
@TestConfiguration
@Profile("local-integration")
public class LocalIntegrationTestSupportConfig {

    @Bean
    @ConditionalOnMissingBean
    public EncryptionUtil encryptionUtil() {
        return new EncryptionUtil();
    }

    @Bean(name = "taskExecutor")
    @ConditionalOnMissingBean(name = "taskExecutor")
    public Executor taskExecutor() {
        return new SyncTaskExecutor();
    }
}
