package jaeik.growfarm.infrastructure.adapter.paper.out.persistence.paper;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.GrowfarmApplication;
import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.domain.paper.entity.DecoType;
import jaeik.growfarm.domain.paper.entity.Message;
import jaeik.growfarm.domain.user.entity.Setting;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.user.entity.UserRole;
import jaeik.growfarm.infrastructure.adapter.paper.in.web.dto.MessageDTO;
import jaeik.growfarm.infrastructure.adapter.paper.in.web.dto.VisitMessageDTO;
import jaeik.growfarm.infrastructure.security.EncryptionUtil;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * <h2>PaperQueryAdapter 테스트</h2>
 * <p>PaperQueryAdapter의 모든 기능을 테스트합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DataJpaTest(
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = GrowfarmApplication.class
        )
)
@Testcontainers
@EntityScan(basePackages = {
        "jaeik.growfarm.domain.admin.entity", 
        "jaeik.growfarm.domain.user.entity",
        "jaeik.growfarm.domain.paper.entity",
        "jaeik.growfarm.domain.post.entity",
        "jaeik.growfarm.domain.comment.entity",
        "jaeik.growfarm.domain.notification.entity",
        "jaeik.growfarm.domain.common.entity"
})
@Import(PaperQueryAdapter.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create"
})
class PaperQueryAdapterTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
            return new JPAQueryFactory(entityManager);
        }

        // EncryptionUtil 빈 정의: MessageEncryptConverter의 의존성을 만족시킵니다.
        @Bean
        public EncryptionUtil encryptionUtil() {
            return new EncryptionUtil();
        }
    }

    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private PaperQueryAdapter paperQueryAdapter;

    private User testUser;
    private Message testMessage1;
    private Message testMessage2;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = User.builder()
                .userName("testUser")
                .socialId("test123")
                .provider(SocialProvider.KAKAO)
                .socialNickname("테스트사용자")
                .role(UserRole.USER)
                .setting(Setting.builder().build())
                .build();
        entityManager.persistAndFlush(testUser);

        // 테스트 메시지들 생성
        testMessage1 = Message.builder()
                .user(testUser)
                .content("Hello World 1")
                .decoType(DecoType.POTATO)
                .anonymity("테스트익명1")
                .width(100)
                .height(200)
                .build();
        
        testMessage2 = Message.builder()
                .user(testUser)
                .content("Hello World 2")
                .decoType(DecoType.CARROT)
                .anonymity("테스트익명2")
                .width(150)
                .height(250)
                .build();
        
        entityManager.persistAndFlush(testMessage1);
        entityManager.persistAndFlush(testMessage2);
        entityManager.clear();
    }

    @Test
    @DisplayName("정상 케이스 - 메시지 ID로 메시지 조회")
    void shouldFindMessageById_WhenValidIdProvided() {
        // Given
        Long messageId = testMessage1.getId();

        // When
        Optional<Message> result = paperQueryAdapter.findMessageById(messageId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(messageId);
        assertThat(result.get().getContent()).isEqualTo("Hello World 1");
        assertThat(result.get().getUser().getUserName()).isEqualTo("testUser");
    }

    @Test
    @DisplayName("경계값 - 존재하지 않는 메시지 ID로 메시지 조회")
    void shouldReturnEmpty_WhenNonExistentMessageIdProvided() {
        // Given
        Long nonExistentMessageId = 999999L;

        // When
        Optional<Message> result = paperQueryAdapter.findMessageById(nonExistentMessageId);

        // Then
        assertThat(result).isEmpty();
    }

    // TODO: 테스트 실패 - 메인 로직 문제 의심
    // QueryDSL 정렬 로직: 마이크로초 단위 시간차가 없어 동일 시간으로 생성되는 경우 발생
    // 가능한 문제: 1) 테스트 환경에서 빠른 연속 생성으로 인한 시간 중복 2) 정렬 기준 모호성
    // 수정 필요: 1) ID 기준 추가 정렬 2) 테스트 데이터 생성 시 시간 간격 조정
    @Test
    @DisplayName("정상 케이스 - 사용자 ID로 MessageDTO 목록 조회")
    void shouldFindMessageDTOsByUserId_WhenValidUserIdProvided() {
        // Given
        Long userId = testUser.getId();

        // When
        List<MessageDTO> result = paperQueryAdapter.findMessageDTOsByUserId(userId);

        // Then
        assertThat(result).hasSize(2); // testMessage1, testMessage2
        assertThat(result.get(0).getUserId()).isEqualTo(userId);
        assertThat(result.get(0).getContent()).isNotEmpty();
        // ID 기준 정렬 확인 (시간 정렬 대신 안정적인 ID 정렬)
        assertThat(result.get(0).getId()).isGreaterThan(result.get(1).getId());
    }

    @Test
    @DisplayName("경계값 - 메시지가 없는 사용자 ID로 MessageDTO 목록 조회")
    void shouldReturnEmptyList_WhenUserHasNoMessages() {
        // Given
        Long userIdWithoutMessages = 999999L;

        // When
        List<MessageDTO> result = paperQueryAdapter.findMessageDTOsByUserId(userIdWithoutMessages);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("정상 케이스 - 사용자 이름으로 VisitMessageDTO 목록 조회")
    void shouldFindVisitMessageDTOsByUserName_WhenValidUserNameProvided() {
        // Given
        String userName = testUser.getUserName();

        // When
        List<VisitMessageDTO> result = paperQueryAdapter.findVisitMessageDTOsByUserName(userName);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUserId()).isEqualTo(testUser.getId());
        assertThat(result.get(0).getDecoType()).isNotNull();
    }

    @Test
    @DisplayName("경계값 - 메시지가 없는 사용자 이름으로 VisitMessageDTO 목록 조회")
    void shouldReturnEmptyList_WhenUserHasNoVisitMessages() {
        // Given
        String userNameWithoutMessages = "nonExistentUser";

        // When
        List<VisitMessageDTO> result = paperQueryAdapter.findVisitMessageDTOsByUserName(userNameWithoutMessages);

        // Then
        assertThat(result).isEmpty();
    }

    // TODO: 테스트 실패 - 메인 로직 문제 의심
    // QueryDSL null 처리: eq(null) 사용 시 IllegalArgumentException 발생
    // 가능한 문제: 1) QueryDSL 조건식에서 null 값 처리 누락 2) 방어 코드 부족
    // 수정 필요: PaperQueryAdapter에서 null 입력에 대한 early return 또는 isNull() 사용
    @Test
    @DisplayName("예외 처리 - null 입력 처리")
    void shouldHandleGracefully_WhenNullInputProvided() {
        // When & Then: null 입력 처리 - 예외 대신 빈 결과 반환
        assertThatNoException().isThrownBy(() -> {
            Optional<Message> nullIdResult = paperQueryAdapter.findMessageById(null);
            assertThat(nullIdResult).isEmpty();
        });
        
        assertThatNoException().isThrownBy(() -> {
            List<MessageDTO> nullUserIdResult = paperQueryAdapter.findMessageDTOsByUserId(null);
            assertThat(nullUserIdResult).isEmpty();
        });
        
        assertThatNoException().isThrownBy(() -> {
            List<VisitMessageDTO> nullUserNameResult = paperQueryAdapter.findVisitMessageDTOsByUserName(null);
            assertThat(nullUserNameResult).isEmpty();
        });
        
        // 빈 문자열 처리
        assertThatNoException().isThrownBy(() -> {
            List<VisitMessageDTO> emptyUserNameResult = paperQueryAdapter.findVisitMessageDTOsByUserName("");
            assertThat(emptyUserNameResult).isEmpty();
        });
    }
}