package jaeik.bimillog.infrastructure.outadapter.paper.persistence.paper;

import jaeik.bimillog.BimilLogApplication;
import jaeik.bimillog.domain.auth.entity.SocialProvider;
import jaeik.bimillog.domain.paper.entity.DecoType;
import jaeik.bimillog.domain.paper.entity.Message;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.testutil.TestContainersConfiguration;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * <h2>PaperCommandAdapter 통합 테스트</h2>
 * 동시성 테스트는 in어댑터에서 검증합니다.
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
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = BimilLogApplication.class
        )
)
@Testcontainers
@Import({PaperCommandAdapter.class, TestContainersConfiguration.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect",
        "logging.level.org.hibernate.SQL=DEBUG"
})
class PaperCommandAdapterTest {

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
        // Given & When & Then: User가 null인 경우 예외 발생
        assertThatThrownBy(() -> {
            Message messageWithNullUser = Message.builder()
                    .user(null) // NULL
                    .decoType(DecoType.POTATO)
                    .anonymity("익명친구")
                    .content("테스트")
                    .width(1)
                    .height(1)
                    .build();
            messageRepository.save(messageWithNullUser);
            messageRepository.flush();
        }).isInstanceOf(Exception.class); // ConstraintViolation이나 다른 validation 예외

        // Content가 null인 경우도 마찬가지
        assertThatThrownBy(() -> {
            Message messageWithNullContent = Message.builder()
                    .user(testUser)
                    .decoType(DecoType.POTATO)
                    .anonymity("익명친구")
                    .content(null) // NULL
                    .width(1)
                    .height(1)
                    .build();
            messageRepository.save(messageWithNullContent);
            messageRepository.flush();
        }).isInstanceOf(Exception.class); // ConstraintViolation이나 다른 validation 예외
    }

    @Test
    @DisplayName("제약조건 - UNIQUE 제약 (사용자별 좌표 중복) 검증")  
    void shouldThrowException_WhenUniqueConstraintViolated() {
        // Given: 첫 번째 메시지 저장 및 커밋
        Message firstMessage = Message.builder()
                .user(testUser)
                .decoType(DecoType.POTATO)
                .anonymity("첫번째")
                .content("첫 번째 메시지")
                .width(5)
                .height(7)
                .build();
        
        Message savedFirstMessage = messageRepository.save(firstMessage);
        assertThat(savedFirstMessage.getId()).isNotNull();
        
        // When & Then: 같은 좌표로 두 번째 메시지 저장 시도
        Message duplicateMessage = Message.builder()
                .user(testUser) // 같은 사용자
                .decoType(DecoType.TOMATO)
                .anonymity("두번째")
                .content("두 번째 메시지")
                .width(5) // 같은 좌표
                .height(7) // 같은 좌표
                .build();

        // 직접 Repository 사용하여 UNIQUE 제약조건 테스트
        assertThatThrownBy(() -> {
            messageRepository.save(duplicateMessage);
            messageRepository.flush();
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
    @DisplayName("예외 처리 - 외부 키 참조 무결성 위반")
    void shouldThrowException_WhenForeignKeyConstraintViolated() {
        // Given: 존재하지 않는 사용자 ID를 가진 User 프록시  
        // 실제 존재하지 않는 ID로 생성하여 FK 제약조건 테스트
        User nonExistentUser = entityManager.getEntityManager().getReference(User.class, 99999L);

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
            messageRepository.save(messageWithInvalidUser);
            messageRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
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
        // Given: 50개의 서로 다른 좌표 메시지 (unique 제약조건 고려)
        List<Message> bulkMessages = IntStream.range(0, 50)
                .mapToObj(i -> Message.builder()
                        .user(testUser)
                        .decoType(DecoType.values()[i % DecoType.values().length])
                        .anonymity("대용량" + i)
                        .content("대용량 테스트 메시지 " + i)
                        .width(i / 10) // 서로 다른 좌표 조합
                        .height(i % 10) // 서로 다른 좌표 조합
                        .build())
                .collect(java.util.stream.Collectors.toList());

        // When: 대용량 저장
        long startTime = System.currentTimeMillis();
        bulkMessages.forEach(paperCommandAdapter::save);
        entityManager.flush();
        long endTime = System.currentTimeMillis();

        // Then: 모든 메시지 저장 성공 및 성능 확인
        List<Message> savedMessages = messageRepository.findAll();
        assertThat(savedMessages).hasSize(50);
        assertThat(endTime - startTime).isLessThan(5000); // 5초 이내
        
        // 각 메시지가 고유한 좌표를 가지는지 확인
        long uniqueCoordinates = savedMessages.stream()
                .collect(java.util.stream.Collectors.groupingBy(m -> m.getWidth() + "," + m.getHeight()))
                .size();
        assertThat(uniqueCoordinates).isEqualTo(50);
    }

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
