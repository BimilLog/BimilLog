package jaeik.bimillog.infrastructure.adapter.paper.paper;

import jaeik.bimillog.BimilLogApplication;
import jaeik.bimillog.domain.paper.entity.DecoType;
import jaeik.bimillog.domain.paper.entity.Message;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.paper.out.jpa.MessageRepository;
import jaeik.bimillog.infrastructure.adapter.paper.out.paper.PaperCommandAdapter;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * <h2>PaperCommandAdapter 통합 테스트</h2>
 * <p>PaperCommandAdapter의 핵심 명령 기능을 테스트합니다.</p>
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
    private Message testMessage;

    @BeforeEach
    void setUp() {
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

        testMessage = Message.builder()
                .user(testUser)
                .decoType(DecoType.POTATO)
                .anonymity("익명친구")
                .content("안녕하세요! 테스트 메시지입니다.")
                .x(2)
                .y(3)
                .build();
    }

    @Test
    @DisplayName("메시지 저장 - 모든 필드 매핑 정확성")
    void shouldMapAllFields_WhenValidMessageProvided() {
        // When
        Message savedMessage = paperCommandAdapter.save(testMessage);
        entityManager.flush();
        entityManager.clear();

        // Then
        Message foundMessage = entityManager.find(Message.class, savedMessage.getId());
        assertThat(foundMessage).isNotNull();
        assertThat(foundMessage.getId()).isNotNull();
        assertThat(foundMessage.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(foundMessage.getDecoType()).isEqualTo(DecoType.POTATO);
        assertThat(foundMessage.getAnonymity()).isEqualTo("익명친구");
        assertThat(foundMessage.getContent()).isEqualTo("안녕하세요! 테스트 메시지입니다.");
        assertThat(foundMessage.getX()).isEqualTo(2);
        assertThat(foundMessage.getY()).isEqualTo(3);
        assertThat(foundMessage.getCreatedAt()).isNotNull();
        assertThat(foundMessage.getModifiedAt()).isNotNull();
    }

    @Test
    @DisplayName("메시지 삭제 - 성공")
    void shouldDeleteMessage_WhenValidIdProvided() {
        // Given
        Message savedMessage = paperCommandAdapter.save(testMessage);
        entityManager.flush();
        Long messageId = savedMessage.getId();
        
        // When
        paperCommandAdapter.deleteById(messageId);
        entityManager.flush();

        // Then
        assertThat(messageRepository.findById(messageId)).isEmpty();
    }

    @Test
    @DisplayName("UNIQUE 제약조건 - 사용자별 좌표 중복 방지")
    void shouldThrowException_WhenUniqueConstraintViolated() {
        // Given - 첫 번째 메시지 저장
        Message firstMessage = Message.builder()
                .user(testUser)
                .decoType(DecoType.POTATO)
                .anonymity("첫번째")
                .content("첫 번째 메시지")
                .x(5)
                .y(7)
                .build();
        
        paperCommandAdapter.save(firstMessage);
        entityManager.flush();
        
        // When & Then - 같은 사용자의 같은 좌표로 저장 시도
        Message duplicateMessage = Message.builder()
                .user(testUser)
                .decoType(DecoType.TOMATO)
                .anonymity("두번째")
                .content("두 번째 메시지")
                .x(5) // 같은 좌표
                .y(7) // 같은 좌표
                .build();

        assertThatThrownBy(() -> {
            paperCommandAdapter.save(duplicateMessage);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
