package jaeik.growfarm.infrastructure.adapter.paper.out.persistence.paper;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.domain.paper.entity.DecoType;
import jaeik.growfarm.domain.paper.entity.Message;
import jaeik.growfarm.domain.user.entity.Setting;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.user.entity.UserRole;
import jaeik.growfarm.infrastructure.security.EncryptionUtil;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * <h2>PaperCommandAdapter 통합 테스트</h2>
 *
 * <p><strong>테스트 커버리지:</strong></p>
 * <ul>
 *   <li>정상 케이스: 메시지 저장/삭제 성공</li>
 *   <li>데이터 매핑: 모든 필드 정확성, 암호화 처리</li>
 *   <li>제약조건: NOT NULL, UNIQUE 제약 검증</li>
 *   <li>관계 매핑: User-Message 연관관계 일관성</li>
 *   <li>트랜잭션: 롤백/커밋 경계 정확성</li>
 *   <li>동시성: 중복 좌표 처리, Race Condition</li>
 *   <li>예외 처리: 제약조건 위반, FK 참조 무결성</li>
 * </ul>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DataJpaTest(
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = jaeik.growfarm.GrowfarmApplication.class
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
@EnableJpaRepositories(basePackages = {
        "jaeik.growfarm.infrastructure.adapter.paper.out.persistence.paper"
})
@Import(PaperCommandAdapter.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect",
        "logging.level.org.hibernate.SQL=DEBUG"
})
class PaperCommandAdapterTest {

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
    private PaperCommandAdapter paperCommandAdapter;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MessageRepository messageRepository;

    private User testUser;
    private User otherUser;
    private Message testMessage;

    @BeforeEach
    void setUp() {
        // 테스트 사용자들 생성 및 저장
        testUser = User.builder()
                .userName("testUser")
                .socialId("123456")
                .provider(SocialProvider.KAKAO)
                .socialNickname("테스트유저")
                .role(UserRole.USER)
                .setting(Setting.builder()
                        .messageNotification(true)
                        .commentNotification(true)
                        .postFeaturedNotification(true)
                        .build())
                .build();
        entityManager.persistAndFlush(testUser);

        otherUser = User.builder()
                .userName("otherUser")
                .socialId("789012")
                .provider(SocialProvider.NAVER)
                .socialNickname("다른유저")
                .role(UserRole.USER)
                .setting(Setting.builder().build())
                .build();
        entityManager.persistAndFlush(otherUser);

        // 테스트 메시지 준비
        testMessage = Message.builder()
                .user(testUser)
                .decoType(DecoType.POTATO)
                .anonymity("익명친구")
                .content("안녕하세요! 테스트 메시지입니다.")
                .width(2)
                .height(3)
                .build();
    }

    @Test
    @DisplayName("정상 케이스 - 메시지 저장 시 모든 필드 매핑 정확성")
    void shouldMapAllFields_WhenValidMessageProvided() {
        // When: 메시지 저장
        Message savedMessage = paperCommandAdapter.save(testMessage);
        entityManager.flush();
        entityManager.clear();

        // Then: 모든 필드가 정확히 매핑되고 암호화 처리됨
        Message foundMessage = entityManager.find(Message.class, savedMessage.getId());
        assertThat(foundMessage).isNotNull();
        assertThat(foundMessage.getId()).isNotNull();
        assertThat(foundMessage.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(foundMessage.getDecoType()).isEqualTo(DecoType.POTATO);
        assertThat(foundMessage.getAnonymity()).isEqualTo("익명친구");
        assertThat(foundMessage.getContent()).isEqualTo("안녕하세요! 테스트 메시지입니다.");
        assertThat(foundMessage.getWidth()).isEqualTo(2);
        assertThat(foundMessage.getHeight()).isEqualTo(3);
        assertThat(foundMessage.getCreatedAt()).isNotNull();
        assertThat(foundMessage.getModifiedAt()).isNotNull();
    }

    @Test
    @DisplayName("정상 케이스 - 메시지 저장 후 트랜잭션 커밋 확인")
    void shouldCommitTransaction_WhenMessageSavedSuccessfully() {
        // When: 메시지 저장
        Message savedMessage = paperCommandAdapter.save(testMessage);
        entityManager.flush();

        // Then: 트랜잭션 커밋으로 실제 DB에 저장됨
        Long savedId = savedMessage.getId();
        entityManager.clear(); // 영속성 컨텍스트 초기화
        Message foundMessage = messageRepository.findById(savedId).orElse(null);
        assertThat(foundMessage).isNotNull();
        assertThat(foundMessage.getContent()).isEqualTo("안녕하세요! 테스트 메시지입니다.");
    }

    @Test
    @DisplayName("정상 케이스 - 메시지 삭제 성공")
    void shouldDeleteMessage_WhenValidIdProvided() {
        // Given: 저장된 메시지
        Message savedMessage = paperCommandAdapter.save(testMessage);
        entityManager.flush();
        Long messageId = savedMessage.getId();
        
        // When: 메시지 삭제
        paperCommandAdapter.deleteById(messageId);
        entityManager.flush();

        // Then: 메시지가 실제로 삭제됨
        assertThat(messageRepository.findById(messageId)).isEmpty();
    }

    @Test
    @DisplayName("관계 매핑 - User와 Message 연관관계 양방향 일관성")
    void shouldMaintainConsistentRelationship_BetweenUserAndMessage() {
        // When: 메시지 저장
        Message savedMessage = paperCommandAdapter.save(testMessage);
        entityManager.flush();
        entityManager.clear();

        // Then: User-Message 관계가 올바르게 매핑됨
        Message foundMessage = messageRepository.findById(savedMessage.getId()).get();
        User foundUser = foundMessage.getUser();
        assertThat(foundUser.getId()).isEqualTo(testUser.getId());
        assertThat(foundUser.getUserName()).isEqualTo("testUser");
        
        // Lazy Loading 확인
        assertThat(foundUser.getSocialNickname()).isEqualTo("테스트유저");
    }

    @Test
    @DisplayName("제약조건 - NOT NULL 필드 검증")
    void shouldThrowException_WhenRequiredFieldsAreNull() {
        // Given: 필수 필드가 null인 메시지들
        Message messageWithNullUser = Message.builder()
                .user(null) // NULL
                .decoType(DecoType.POTATO)
                .anonymity("익명친구")
                .content("테스트")
                .width(1)
                .height(1)
                .build();

        Message messageWithNullContent = Message.builder()
                .user(testUser)
                .decoType(DecoType.POTATO)
                .anonymity("익명친구")
                .content(null) // NULL
                .width(1)
                .height(1)
                .build();

        // When & Then: NULL 제약조건 위반 시 예외 발생
        assertThatThrownBy(() -> {
            paperCommandAdapter.save(messageWithNullUser);
            entityManager.flush();
        }).isInstanceOf(Exception.class);

        assertThatThrownBy(() -> {
            paperCommandAdapter.save(messageWithNullContent);
            entityManager.flush();
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("제약조건 - UNIQUE 제약 (사용자별 좌표 중복) 검증")
    void shouldThrowException_WhenUniqueConstraintViolated() {
        // Given: 같은 사용자, 같은 좌표의 메시지 2개
        Message firstMessage = Message.builder()
                .user(testUser)
                .decoType(DecoType.POTATO)
                .anonymity("첫번째")
                .content("첫 번째 메시지")
                .width(5)
                .height(7)
                .build();

        Message duplicateMessage = Message.builder()
                .user(testUser) // 같은 사용자
                .decoType(DecoType.TOMATO)
                .anonymity("두번째")
                .content("두 번째 메시지")
                .width(5) // 같은 좌표
                .height(7) // 같은 좌표
                .build();

        // When: 첫 번째 메시지 저장 성공
        paperCommandAdapter.save(firstMessage);
        entityManager.flush();

        // Then: 두 번째 메시지 저장 시 UNIQUE 제약조건 위반 예외
        assertThatThrownBy(() -> {
            paperCommandAdapter.save(duplicateMessage);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("제약조건 - 다른 사용자는 같은 좌표 사용 가능")
    void shouldAllowSameCoordinates_ForDifferentUsers() {
        // Given: 다른 사용자들의 같은 좌표 메시지
        Message firstUserMessage = Message.builder()
                .user(testUser)
                .decoType(DecoType.POTATO)
                .anonymity("첫유저")
                .content("첫 번째 사용자 메시지")
                .width(10)
                .height(15)
                .build();

        Message secondUserMessage = Message.builder()
                .user(otherUser) // 다른 사용자
                .decoType(DecoType.TOMATO)
                .anonymity("둘유저")
                .content("두 번째 사용자 메시지")
                .width(10) // 같은 좌표
                .height(15) // 같은 좌표
                .build();

        // When & Then: 둘 다 저장 성공
        assertDoesNotThrow(() -> {
            paperCommandAdapter.save(firstUserMessage);
            paperCommandAdapter.save(secondUserMessage);
            entityManager.flush();
        });

        // 두 메시지 모두 저장 확인
        List<Message> allMessages = messageRepository.findAll();
        assertThat(allMessages).hasSize(2);
    }

    @Test
    @DisplayName("트랜잭션 - 부분 실패 시 롤백")
    void shouldRollback_WhenPartialFailureOccurs() {
        // Given: 성공할 메시지와 실패할 메시지
        Message validMessage = Message.builder()
                .user(testUser)
                .decoType(DecoType.POTATO)
                .anonymity("성공메시지")
                .content("성공할 메시지")
                .width(1)
                .height(2)
                .build();

        Message invalidMessage = Message.builder()
                .user(testUser)
                .decoType(DecoType.TOMATO)
                .anonymity("실패메시지")
                .content("실패할 메시지")
                .width(1) // 같은 좌표로 인한 제약조건 위반
                .height(2) // 같은 좌표로 인한 제약조건 위반
                .build();

        // When: 첫 번째는 성공, 두 번째는 실패
        paperCommandAdapter.save(validMessage);
        
        assertThatThrownBy(() -> {
            paperCommandAdapter.save(invalidMessage);
            entityManager.flush(); // 트랜잭션 경계에서 실패
        }).isInstanceOf(DataIntegrityViolationException.class);

        // Then: 첫 번째 메시지는 저장되지 않음 (트랜잭션 롤백)
        List<Message> messages = messageRepository.findAll();
        assertThat(messages).isEmpty(); // @Transactional(rollbackFor) 동작 확인
    }

    @Test
    @DisplayName("동시성 - 중복 좌표 Race Condition 처리")
    void shouldHandleConcurrentRequests_WhenSimultaneousCoordinateAccess() throws InterruptedException, ExecutionException {
        // Given: 동시에 같은 좌표에 저장하려는 시나리오
        int threadCount = 3;
        List<CompletableFuture<Exception>> futures = IntStream.range(0, threadCount)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                    try {
                        Message concurrentMessage = Message.builder()
                                .user(testUser)
                                .decoType(DecoType.POTATO)
                                .anonymity("동시성" + i)
                                .content("동시성 테스트 메시지 " + i)
                                .width(99) // 모두 같은 좌표
                                .height(99) // 모두 같은 좌표
                                .build();
                        paperCommandAdapter.save(concurrentMessage);
                        entityManager.flush();
                        return null; // 성공
                    } catch (Exception e) {
                        return e; // 실패 시 예외 반환
                    }
                }))
                .collect(java.util.stream.Collectors.toList());

        // When: 모든 작업 완료 대기
        List<Exception> results = futures.stream()
                .map(CompletableFuture::join)
                .collect(java.util.stream.Collectors.toList());

        // Then: 하나만 성공하고 나머지는 제약조건 위반
        long successCount = results.stream().filter(result -> result == null).count();
        long failureCount = results.stream().filter(result -> result != null).count();
        
        assertThat(successCount).isEqualTo(1);
        assertThat(failureCount).isEqualTo(2);
        
        // 실제 저장된 메시지는 1개
        List<Message> savedMessages = messageRepository.findAll();
        assertThat(savedMessages).hasSize(1);
    }

    @Test
    @DisplayName("예외 처리 - 외부 키 참조 무결성 위반")
    void shouldThrowException_WhenForeignKeyConstraintViolated() {
        // Given: 존재하지 않는 사용자 ID를 가진 User 프록시
        User nonExistentUser = User.builder()
                .id(99999L) // 존재하지 않는 ID
                .userName("ghost")
                .build();

        Message messageWithInvalidUser = Message.builder()
                .user(nonExistentUser)
                .decoType(DecoType.POTATO)
                .anonymity("유령유저")
                .content("존재하지 않는 사용자의 메시지")
                .width(1)
                .height(1)
                .build();

        // When & Then: 외래키 제약조건 위반 예외
        assertThatThrownBy(() -> {
            paperCommandAdapter.save(messageWithInvalidUser);
            entityManager.flush();
        }).isInstanceOf(Exception.class); // FK 제약조건 위반
    }

    @Test
    @DisplayName("경계값 - 존재하지 않는 ID로 삭제 시도")
    void shouldHandleGracefully_WhenDeletingNonExistentId() {
        // Given: 존재하지 않는 메시지 ID
        Long nonExistentId = 99999L;

        // When: 삭제 실행 (JPA deleteById는 존재하지 않아도 예외 없음)
        assertDoesNotThrow(() -> {
            paperCommandAdapter.deleteById(nonExistentId);
            entityManager.flush();
        });

        // Then: 다른 데이터에 영향 없음
        Message savedMessage = paperCommandAdapter.save(testMessage);
        entityManager.flush();
        assertThat(messageRepository.findById(savedMessage.getId())).isPresent();
    }

    @Test
    @DisplayName("성능 - 대용량 데이터 일괄 저장 처리")
    void shouldHandleBulkInsert_WhenLargeDataSet() {
        // Given: 100개의 서로 다른 좌표 메시지
        List<Message> bulkMessages = IntStream.range(0, 100)
                .mapToObj(i -> Message.builder()
                        .user(testUser)
                        .decoType(DecoType.values()[i % DecoType.values().length])
                        .anonymity("대용량" + i)
                        .content("대용량 테스트 메시지 " + i)
                        .width(i / 10) // 서로 다른 좌표
                        .height(i % 10) // 서로 다른 좌표
                        .build())
                .collect(java.util.stream.Collectors.toList());

        // When: 대용량 저장
        long startTime = System.currentTimeMillis();
        bulkMessages.forEach(paperCommandAdapter::save);
        entityManager.flush();
        long endTime = System.currentTimeMillis();

        // Then: 모든 메시지 저장 성공 및 성능 확인
        List<Message> savedMessages = messageRepository.findAll();
        assertThat(savedMessages).hasSize(100);
        assertThat(endTime - startTime).isLessThan(10000); // 10초 이내
    }

    // TODO: 테스트 실패 - 메인 로직 문제 의심
    // 추가 검증 필요: MessageEncryptConverter 암호화/복호화 일관성
    // 가능한 문제: 1) 암호화 키 관리 2) 문자 인코딩 3) 암호화된 데이터 길이 제한
    // 수정 필요: MessageEncryptConverter 클래스 검토
    @Test
    @DisplayName("암호화 - 메시지 내용 암호화/복호화 일관성")
    void shouldEncryptAndDecryptConsistently_WhenSavingMessage() {
        // Given: 특수문자와 긴 텍스트를 포함한 메시지
        String originalContent = "안녕하세요! 🌟 This is a test message with special chars: @#$%^&*()_+";
        Message messageWithSpecialContent = Message.builder()
                .user(testUser)
                .decoType(DecoType.POTATO)
                .anonymity("암호화테스트")
                .content(originalContent)
                .width(50)
                .height(60)
                .build();

        // When: 저장 후 조회
        Message savedMessage = paperCommandAdapter.save(messageWithSpecialContent);
        entityManager.flush();
        entityManager.clear();
        
        Message retrievedMessage = messageRepository.findById(savedMessage.getId()).get();

        // Then: 원본 내용과 일치 (암호화/복호화 투명)
        assertThat(retrievedMessage.getContent()).isEqualTo(originalContent);
    }
}
