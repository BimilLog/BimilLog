package jaeik.bimillog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * <h2>로컬 샘플 데이터 초기화</h2>
 * <p>
 * 로컬 프로필에서 애플리케이션 기동 시 data.sql 스크립트를 한 번 실행한다.
 * </p>
 */
@Component
@Profile("local")
public class LocalSampleDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalSampleDataInitializer.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public LocalSampleDataInitializer(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        Long postCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM post", Long.class);
        if (postCount != null && postCount > 0) {
            log.info("[LocalSampleDataInitializer] 샘플 데이터가 이미 존재하여 초기화를 건너뜁니다.");
            return;
        }

        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("data.sql"));
            log.info("[LocalSampleDataInitializer] 샘플 데이터 초기화가 완료되었습니다.");
        } catch (Exception ex) {
            log.error("[LocalSampleDataInitializer] 샘플 데이터 초기화 중 오류가 발생했습니다.", ex);
        }
    }
}
