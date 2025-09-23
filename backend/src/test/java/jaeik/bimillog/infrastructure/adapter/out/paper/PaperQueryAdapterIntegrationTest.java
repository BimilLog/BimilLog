package jaeik.bimillog.infrastructure.adapter.out.paper;

import jaeik.bimillog.BimilLogApplication;
import jaeik.bimillog.domain.paper.entity.Message;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.testutil.TestContainersConfiguration;
import jaeik.bimillog.testutil.TestFixtures;
import jaeik.bimillog.testutil.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>PaperQueryAdapter 통합 테스트</h2>
 * <p>PaperQueryAdapter의 핵심 조회 기능을 테스트합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DataJpaTest(
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = BimilLogApplication.class
        )
)
@Import({PaperQueryAdapter.class, TestContainersConfiguration.class})
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect",
        "logging.level.org.hibernate.SQL=DEBUG"
})
@DisplayName("PaperQueryAdapter 통합 테스트")
class PaperQueryAdapterIntegrationTest {

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private PaperQueryAdapter paperQueryAdapter;

    private User testUser;
    private Message testMessage1;
    private Message testMessage2;

    @BeforeEach
    void setUp() {
        testUser = TestUsers.createUniqueWithPrefix("querytest");
        testEntityManager.persistAndFlush(testUser);

        testMessage1 = TestFixtures.createRollingPaper(
                testUser, "첫 번째 메시지", "red", "font1", 100, 50);
        testMessage2 = TestFixtures.createRollingPaper(
                testUser, "두 번째 메시지", "blue", "font2", 150, 75);

        testEntityManager.persistAndFlush(testMessage1);
        testEntityManager.persistAndFlush(testMessage2);
        testEntityManager.flush();
        testEntityManager.clear();
    }

    @Test
    @DisplayName("사용자 ID로 메시지 목록 조회 - 성공")
    void shouldFindMessagesByUserId_WhenValidUserId() {
        // Given
        Long userId = testUser.getId();

        // When
        List<Message> result = paperQueryAdapter.findMessagesByUserId(userId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUserId()).isEqualTo(userId);
        assertThat(result.get(1).getUserId()).isEqualTo(userId);
        
        // 내림차순 정렬 확인 (최신순)
        assertThat(result.get(0).getCreatedAt()).isAfterOrEqualTo(result.get(1).getCreatedAt());
    }

    @Test
    @DisplayName("사용자명으로 메시지 목록 조회 - 성공")
    void shouldFindMessagesByUserName_WhenValidUserName() {
        // When
        List<Message> result = paperQueryAdapter.findMessagesByUserName(testUser.getUserName());

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUserId()).isEqualTo(testUser.getId());
        assertThat(result.get(1).getUserId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("null 및 빈 문자열 처리")
    void shouldHandleNullAndEmptyInputs() {
        // null userId 처리
        List<Message> nullUserIdResult = paperQueryAdapter.findMessagesByUserId(null);
        assertThat(nullUserIdResult).isEmpty();
        
        // null userName 처리
        List<Message> nullUserNameResult = paperQueryAdapter.findMessagesByUserName(null);
        assertThat(nullUserNameResult).isEmpty();
        
        // 빈 문자열 userName 처리
        List<Message> emptyUserNameResult = paperQueryAdapter.findMessagesByUserName("");
        assertThat(emptyUserNameResult).isEmpty();
        
        // 공백 문자열 userName 처리
        List<Message> whiteSpaceUserNameResult = paperQueryAdapter.findMessagesByUserName("   ");
        assertThat(whiteSpaceUserNameResult).isEmpty();
    }

    @Test
    @DisplayName("메시지 ID로 소유자 ID 조회 - 성공")
    void shouldFindOwnerIdByMessageId_WhenValidId() {
        // When
        Optional<Long> result = paperQueryAdapter.findOwnerIdByMessageId(testMessage1.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testUser.getId());

        // null ID 처리
        Optional<Long> nullResult = paperQueryAdapter.findOwnerIdByMessageId(null);
        assertThat(nullResult).isEmpty();

        // 존재하지 않는 ID 처리
        Optional<Long> notFoundResult = paperQueryAdapter.findOwnerIdByMessageId(999L);
        assertThat(notFoundResult).isEmpty();
    }

}